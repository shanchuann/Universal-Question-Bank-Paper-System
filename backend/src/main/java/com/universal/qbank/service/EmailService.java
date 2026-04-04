package com.universal.qbank.service;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  @Autowired(required = false)
  private JavaMailSender mailSender;

  @Autowired private SystemConfigService systemConfigService;

  @Value("${spring.mail.username:}")
  private String fromEmail;

  // 验证码存储 (邮箱 -> 验证码信息)
  private static final Map<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();
  // 发送频控 (邮箱+类型 -> 最近发送时间)
  private static final Map<String, Long> lastSendAt = new ConcurrentHashMap<>();

  // 验证码有效期：10分钟
  private static final long CODE_EXPIRY_MS = 10 * 60 * 1000;
  // 发送冷却：60秒
  private static final long SEND_COOLDOWN_MS = 60 * 1000;
  // 最大校验失败次数
  private static final int MAX_VERIFY_ATTEMPTS = 5;

  public static class VerificationCode {
    public String code;
    public long expiryTime;
    public String type; // "register", "reset_password", "change_email"
    public String subjectId; // change_email 时绑定用户ID
    public int remainingAttempts;

    public VerificationCode(String code, String type) {
      this.code = code;
      this.type = type;
      this.expiryTime = System.currentTimeMillis() + CODE_EXPIRY_MS;
      this.subjectId = null;
      this.remainingAttempts = MAX_VERIFY_ATTEMPTS;
    }

    public VerificationCode(String code, String type, String subjectId) {
      this.code = code;
      this.type = type;
      this.subjectId = subjectId;
      this.expiryTime = System.currentTimeMillis() + CODE_EXPIRY_MS;
      this.remainingAttempts = MAX_VERIFY_ATTEMPTS;
    }

    public boolean isExpired() {
      return System.currentTimeMillis() > expiryTime;
    }
  }

  /** 生成6位数字验证码 */
  private String generateCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }

  /** 检查邮件服务是否可用 */
  public boolean isEmailServiceAvailable() {
    return mailSender != null && fromEmail != null && !fromEmail.isEmpty();
  }

  /** 发送注册验证码 */
  public void sendRegistrationCode(String toEmail) {
    if (!isEmailServiceAvailable()) {
      throw new RuntimeException("邮件服务未配置");
    }
    String code = issueCode(toEmail, "register", null);

    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】注册验证码");
    message.setText(
        "您好！\n\n"
            + "您的注册验证码是："
            + code
            + "\n\n"
            + "验证码有效期为10分钟，请尽快完成注册。\n\n"
            + "如果这不是您的操作，请忽略此邮件。\n\n"
            + "——"
            + siteName);

    try {
      mailSender.send(message);
      System.out.println("Registration code sent to: " + toEmail);
    } catch (Exception e) {
      System.err.println("Failed to send email: " + e.getMessage());
      throw new RuntimeException("发送邮件失败，请稍后再试");
    }
  }

  /** 发送密码重置验证码 */
  public void sendPasswordResetCode(String toEmail) {
    if (!isEmailServiceAvailable()) {
      throw new RuntimeException("邮件服务未配置");
    }

    // 检查是否允许密码重置
    if (!systemConfigService.getBooleanConfig(SystemConfigService.ALLOW_PASSWORD_RESET, true)) {
      throw new RuntimeException("系统不支持密码重置功能");
    }

    String code = issueCode(toEmail, "reset_password", null);

    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】密码重置验证码");
    message.setText(
        "您好！\n\n"
            + "您正在重置密码，验证码是："
            + code
            + "\n\n"
            + "验证码有效期为10分钟。\n\n"
            + "如果这不是您的操作，请立即检查账户安全。\n\n"
            + "——"
            + siteName);

    try {
      mailSender.send(message);
      System.out.println("Password reset code sent to: " + toEmail);
    } catch (Exception e) {
      System.err.println("Failed to send email: " + e.getMessage());
      throw new RuntimeException("发送邮件失败，请稍后再试");
    }
  }

  /** 发送邮箱修改验证码 */
  public void sendEmailChangeCode(String toEmail, String userId) {
    if (!isEmailServiceAvailable()) {
      throw new RuntimeException("邮件服务未配置");
    }
    String code = issueCode(toEmail, "change_email", userId);
    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】邮箱修改验证码");
    message.setText(
        "您好！\n\n"
            + "您正在修改邮箱，验证码为："
            + code
            + "\n\n"
            + "验证码有效期为10分钟，请尽快完成操作。\n\n"
            + "如果这不是您的操作，请忽略此邮件。\n\n"
            + "——"
            + siteName);
    try {
      mailSender.send(message);
    } catch (Exception e) {
      System.err.println("Failed to send email change code: " + e.getMessage());
      verificationCodes.remove(toEmail);
      lastSendAt.remove(toEmail + "#change_email");
      throw new RuntimeException(buildMailSendErrorMessage(e));
    }
  }

  /** 校验邮箱验证码 */
  public boolean verifyEmailCode(String email, String code, String userId) {
    return verifyCode(email, code, "change_email", userId);
  }

  /** 验证码 */
  public boolean verifyCode(String email, String code, String type) {
    return verifyCode(email, code, type, null);
  }

  public boolean verifyCode(String email, String code, String type, String subjectId) {
    VerificationCode stored = verificationCodes.get(email);
    if (stored == null) {
      return false;
    }
    if (stored.isExpired()) {
      verificationCodes.remove(email);
      return false;
    }
    if (!stored.type.equals(type)) {
      return false;
    }
    if (stored.subjectId != null && (subjectId == null || !stored.subjectId.equals(subjectId))) {
      return false;
    }
    if (!stored.code.equals(code)) {
      stored.remainingAttempts -= 1;
      if (stored.remainingAttempts <= 0) {
        verificationCodes.remove(email);
      }
      return false;
    }
    // 验证成功后删除验证码
    verificationCodes.remove(email);
    return true;
  }

  /** 清理过期验证码（可定期调用） */
  public void cleanupExpiredCodes() {
    verificationCodes.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  private String issueCode(String email, String type, String subjectId) {
    cleanupExpiredCodes();
    String key = email + "#" + type;
    long now = System.currentTimeMillis();
    Long last = lastSendAt.get(key);
    if (last != null && now - last < SEND_COOLDOWN_MS) {
      long left = (SEND_COOLDOWN_MS - (now - last)) / 1000;
      throw new RuntimeException("请求过于频繁，请 " + Math.max(left, 1) + " 秒后重试");
    }

    String code = generateCode();
    verificationCodes.put(email, new VerificationCode(code, type, subjectId));
    lastSendAt.put(key, now);
    return code;
  }

  private String buildMailSendErrorMessage(Exception e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }

    String message = root.getMessage() == null ? "" : root.getMessage();
    if (root instanceof SocketTimeoutException || message.toLowerCase().contains("timed out")) {
      return "邮件发送失败：邮件服务器连接超时，请检查邮件配置或稍后重试";
    }
    if (message.toLowerCase().contains("authentication")
        || message.toLowerCase().contains("auth")) {
      return "邮件发送失败：邮箱账号或授权码校验失败，请检查 SMTP 用户名和密码";
    }
    if (message.toLowerCase().contains("unknownhost")
        || message.toLowerCase().contains("nodename")) {
      return "邮件发送失败：SMTP 服务器地址不可达，请检查服务器地址和网络";
    }
    return "邮件发送失败：" + (message.isBlank() ? "未知错误" : message);
  }

  /** 发送成绩通知邮件 */
  public void sendScoreNotification(
      String toEmail, String studentName, String examTitle, int score, String comments) {
    if (!isEmailServiceAvailable()) {
      System.out.println("邮件服务未配置，无法发送成绩通知");
      return;
    }

    // 检查是否启用通知
    if (!systemConfigService.getBooleanConfig(SystemConfigService.ENABLE_NOTIFICATIONS, true)) {
      System.out.println("系统通知已禁用");
      return;
    }

    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】考试成绩通知 - " + examTitle);

    StringBuilder content = new StringBuilder();
    content.append("亲爱的 ").append(studentName).append("：\n\n");
    content.append("您参加的考试「").append(examTitle).append("」已批阅完成。\n\n");
    content.append("考试成绩：").append(score).append(" 分\n\n");

    if (comments != null && !comments.isEmpty()) {
      content.append("教师评语：\n").append(comments).append("\n\n");
    }

    content.append("您可以登录系统查看详细的答题情况和评分明细。\n\n");
    content.append("祝您学习进步！\n\n");
    content.append("——").append(siteName);

    message.setText(content.toString());

    try {
      mailSender.send(message);
      System.out.println("Score notification sent to: " + toEmail);
    } catch (Exception e) {
      System.err.println("Failed to send score notification: " + e.getMessage());
    }
  }

  /** 发送考试即将开始提醒 */
  public void sendExamReminder(
      String toEmail, String studentName, String examTitle, String startTime) {
    if (!isEmailServiceAvailable()) {
      return;
    }

    if (!systemConfigService.getBooleanConfig(SystemConfigService.ENABLE_NOTIFICATIONS, true)) {
      return;
    }

    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】考试提醒 - " + examTitle);

    StringBuilder content = new StringBuilder();
    content.append("亲爱的 ").append(studentName).append("：\n\n");
    content.append("您报名的考试「").append(examTitle).append("」即将开始。\n\n");
    content.append("考试时间：").append(startTime).append("\n\n");
    content.append("请提前做好准备，按时参加考试。\n\n");
    content.append("祝您考试顺利！\n\n");
    content.append("——").append(siteName);

    message.setText(content.toString());

    try {
      mailSender.send(message);
      System.out.println("Exam reminder sent to: " + toEmail);
    } catch (Exception e) {
      System.err.println("Failed to send exam reminder: " + e.getMessage());
    }
  }

  /** 发送公告发布通知 */
  public void sendAnnouncementNotification(
      String toEmail, String receiverName, String title, String content) {
    if (!isEmailServiceAvailable()) {
      return;
    }

    if (!systemConfigService.getBooleanConfig(SystemConfigService.ENABLE_NOTIFICATIONS, true)) {
      return;
    }

    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】新公告通知 - " + title);

    String summary = content == null ? "" : content;
    if (summary.length() > 180) {
      summary = summary.substring(0, 180) + "...";
    }

    message.setText(
        "亲爱的 "
            + receiverName
            + "：\n\n"
            + "系统发布了新公告："
            + title
            + "\n\n"
            + "公告摘要：\n"
            + summary
            + "\n\n"
            + "请登录系统查看完整公告内容。\n\n"
            + "——"
            + siteName);

    try {
      mailSender.send(message);
    } catch (Exception e) {
      System.err.println("Failed to send announcement notification: " + e.getMessage());
    }
  }

  /** 发送资料修改提醒 */
  public void sendProfileUpdateNotification(String toEmail, String username) {
    if (!isEmailServiceAvailable()) {
      return;
    }

    if (!systemConfigService.getBooleanConfig(SystemConfigService.ENABLE_NOTIFICATIONS, true)) {
      return;
    }

    String siteName = systemConfigService.getConfig(SystemConfigService.SITE_NAME);
    if (siteName == null || siteName.isEmpty()) {
      siteName = "UQBank 题库系统";
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail);
    message.setTo(toEmail);
    message.setSubject("【" + siteName + "】个人信息更新提醒");
    message.setText(
        "您好，"
            + username
            + "：\n\n"
            + "您的个人信息刚刚发生了变更。\n"
            + "如果这不是您本人操作，请尽快修改密码并联系管理员。\n\n"
            + "——"
            + siteName);

    try {
      mailSender.send(message);
    } catch (Exception e) {
      System.err.println("Failed to send profile update notification: " + e.getMessage());
    }
  }
}

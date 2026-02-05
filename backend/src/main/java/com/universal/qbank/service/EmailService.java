package com.universal.qbank.service;

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

  // 验证码有效期：10分钟
  private static final long CODE_EXPIRY_MS = 10 * 60 * 1000;

  public static class VerificationCode {
    public String code;
    public long expiryTime;
    public String type; // "register", "reset_password", "change_email"

    public VerificationCode(String code, String type) {
      this.code = code;
      this.type = type;
      this.expiryTime = System.currentTimeMillis() + CODE_EXPIRY_MS;
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

    String code = generateCode();
    verificationCodes.put(toEmail, new VerificationCode(code, "register"));

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
            + "您的注册验证码是：" + code + "\n\n"
            + "验证码有效期为10分钟，请尽快完成注册。\n\n"
            + "如果这不是您的操作，请忽略此邮件。\n\n"
            + "——" + siteName);

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

    String code = generateCode();
    verificationCodes.put(toEmail, new VerificationCode(code, "reset_password"));

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
            + "您正在重置密码，验证码是：" + code + "\n\n"
            + "验证码有效期为10分钟。\n\n"
            + "如果这不是您的操作，请立即检查账户安全。\n\n"
            + "——" + siteName);

    try {
      mailSender.send(message);
      System.out.println("Password reset code sent to: " + toEmail);
    } catch (Exception e) {
      System.err.println("Failed to send email: " + e.getMessage());
      throw new RuntimeException("发送邮件失败，请稍后再试");
    }
  }

  /** 发送邮箱修改验证码 */
  public void sendEmailChangeCode(String toEmail) {
    if (!isEmailServiceAvailable()) {
      throw new RuntimeException("邮件服务未配置");
    }
    String code = generateCode();
    verificationCodes.put(toEmail, new VerificationCode(code, "change_email"));
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
            + "您正在修改邮箱，验证码为：" + code + "\n\n"
            + "验证码有效期为10分钟，请尽快完成操作。\n\n"
            + "如果这不是您的操作，请忽略此邮件。\n\n"
            + "——" + siteName);
    mailSender.send(message);
  }

  /** 校验邮箱验证码 */
  public boolean verifyEmailCode(String email, String code) {
    VerificationCode vc = verificationCodes.get(email);
    if (vc == null || !"change_email".equals(vc.type) || vc.isExpired()) {
      return false;
    }
    return vc.code.equals(code);
  }

  /** 验证码 */
  public boolean verifyCode(String email, String code, String type) {
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
    if (!stored.code.equals(code)) {
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

  /** 发送成绩通知邮件 */
  public void sendScoreNotification(String toEmail, String studentName, String examTitle, 
      int score, String comments) {
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
  public void sendExamReminder(String toEmail, String studentName, String examTitle, 
      String startTime) {
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
}

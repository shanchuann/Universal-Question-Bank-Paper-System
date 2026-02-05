package com.universal.qbank.controller;

import com.universal.qbank.api.generated.AuthApi;
import com.universal.qbank.api.generated.model.AuthTokenResponse;
import com.universal.qbank.api.generated.model.LoginRequest;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.service.EmailService;
import com.universal.qbank.service.SystemConfigService;
import com.universal.qbank.service.UserService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  @Autowired private UserService userService;

  @Autowired private SystemConfigService systemConfigService;

  @Autowired private EmailService emailService;

  // 登录失败次数记录 (用户名 -> 失败次数)
  private static final Map<String, Integer> loginAttempts = new ConcurrentHashMap<>();
  // 账号锁定时间记录 (用户名 -> 锁定到期时间戳)
  private static final Map<String, Long> lockoutUntil = new ConcurrentHashMap<>();
  // 锁定时间：15分钟
  private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000;

  public static class RegisterRequest {
    public String username;
    public String password;
    public String role;
    public String email;
    public String verificationCode;
  }

  public static class SendCodeRequest {
    public String email;
    public String type; // "register" or "reset_password"
  }

  public static class ResetPasswordRequest {
    public String email;
    public String verificationCode;
    public String newPassword;
  }

  @PostMapping("/api/auth/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
    try {
      // 检查是否需要邮箱验证
      boolean requireVerification = systemConfigService.getBooleanConfig(
          SystemConfigService.REQUIRE_EMAIL_VERIFICATION, false);
      
      if (requireVerification) {
        if (req.email == null || req.email.isEmpty()) {
          return ResponseEntity.badRequest().body(Map.of("error", "需要提供邮箱地址"));
        }
        if (req.verificationCode == null || req.verificationCode.isEmpty()) {
          return ResponseEntity.badRequest().body(Map.of("error", "需要提供验证码"));
        }
        if (!emailService.verifyCode(req.email, req.verificationCode, "register")) {
          return ResponseEntity.badRequest().body(Map.of("error", "验证码错误或已过期"));
        }
      }
      
      UserEntity user = userService.register(req.username, req.password, req.role);
      
      // 如果提供了邮箱，保存到用户信息
      if (req.email != null && !req.email.isEmpty()) {
        user.setEmail(req.email);
        user = userService.saveUser(user);
      }
      
      return ResponseEntity.ok(user);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/api/auth/send-code")
  public ResponseEntity<?> sendVerificationCode(@RequestBody SendCodeRequest req) {
    try {
      if (req.email == null || req.email.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "请提供邮箱地址"));
      }
      
      // 验证邮箱格式
      if (!req.email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
        return ResponseEntity.badRequest().body(Map.of("error", "邮箱格式不正确"));
      }

      if ("register".equals(req.type)) {
        emailService.sendRegistrationCode(req.email);
      } else if ("reset_password".equals(req.type)) {
        // 检查邮箱是否存在
        if (!userService.existsByEmail(req.email)) {
          return ResponseEntity.badRequest().body(Map.of("error", "该邮箱未注册"));
        }
        emailService.sendPasswordResetCode(req.email);
      } else {
        return ResponseEntity.badRequest().body(Map.of("error", "无效的验证码类型"));
      }

      return ResponseEntity.ok(Map.of("message", "验证码已发送"));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/api/auth/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
    try {
      // 检查是否允许密码重置
      if (!systemConfigService.getBooleanConfig(SystemConfigService.ALLOW_PASSWORD_RESET, true)) {
        return ResponseEntity.badRequest().body(Map.of("error", "系统不支持密码重置功能"));
      }

      if (req.email == null || req.email.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "请提供邮箱地址"));
      }
      if (req.verificationCode == null || req.verificationCode.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "请提供验证码"));
      }
      if (req.newPassword == null || req.newPassword.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "请提供新密码"));
      }

      // 检查密码长度
      int minLength = systemConfigService.getIntConfig(SystemConfigService.PASSWORD_MIN_LENGTH, 6);
      if (req.newPassword.length() < minLength) {
        return ResponseEntity.badRequest().body(Map.of("error", "密码长度不能少于 " + minLength + " 个字符"));
      }

      // 验证验证码
      if (!emailService.verifyCode(req.email, req.verificationCode, "reset_password")) {
        return ResponseEntity.badRequest().body(Map.of("error", "验证码错误或已过期"));
      }

      // 重置密码
      userService.resetPasswordByEmail(req.email, req.newPassword);

      return ResponseEntity.ok(Map.of("message", "密码重置成功"));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @Override
  public ResponseEntity<AuthTokenResponse> apiAuthLoginPost(LoginRequest loginRequest) {
    String username = loginRequest.getUsername();
    System.out.println("Login attempt: " + username);

    // 检查账号是否被锁定
    Long lockTime = lockoutUntil.get(username);
    if (lockTime != null && System.currentTimeMillis() < lockTime) {
      long remainingSeconds = (lockTime - System.currentTimeMillis()) / 1000;
      System.out.println("Account locked, remaining: " + remainingSeconds + "s");
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    // 清除过期的锁定
    if (lockTime != null && System.currentTimeMillis() >= lockTime) {
      lockoutUntil.remove(username);
      loginAttempts.remove(username);
    }

    Optional<UserEntity> user =
        userService.login(username, loginRequest.getPassword());

    if (user.isPresent()) {
      // 检查系统状态
      if (!systemConfigService.isSystemEnabled() && !"ADMIN".equals(user.get().getRole())) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      // 登录成功，清除失败记录
      loginAttempts.remove(username);
      lockoutUntil.remove(username);

      System.out.println("Login success");
      AuthTokenResponse response = new AuthTokenResponse();
      response.setAccessToken("dummy-jwt-token-" + user.get().getId());
      response.setRefreshToken("dummy-refresh-token");
      response.setExpiresIn(systemConfigService.getIntConfig(SystemConfigService.SESSION_TIMEOUT, 30) * 60);
      return ResponseEntity.ok(response);
    } else {
      // 处理 admin 后备账号
      if ("admin".equals(username) && "password".equals(loginRequest.getPassword())) {
        loginAttempts.remove(username);
        AuthTokenResponse response = new AuthTokenResponse();
        response.setAccessToken("dummy-jwt-token-admin");
        response.setRefreshToken("dummy-refresh-token");
        response.setExpiresIn(3600);
        return ResponseEntity.ok(response);
      }

      // 登录失败，增加失败次数
      int attempts = loginAttempts.getOrDefault(username, 0) + 1;
      loginAttempts.put(username, attempts);

      int maxAttempts = systemConfigService.getIntConfig(SystemConfigService.MAX_LOGIN_ATTEMPTS, 5);
      if (attempts >= maxAttempts) {
        lockoutUntil.put(username, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
        System.out.println("Account locked due to too many failed attempts");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
      }

      System.out.println("Login failed, attempts: " + attempts + "/" + maxAttempts);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @GetMapping("/api/auth/registration-settings")
  public ResponseEntity<?> getRegistrationSettings() {
    boolean allowRegistration = systemConfigService.getBooleanConfig(
        SystemConfigService.ALLOW_REGISTRATION, true);
    boolean requireEmailVerification = systemConfigService.getBooleanConfig(
        SystemConfigService.REQUIRE_EMAIL_VERIFICATION, false);
    int passwordMinLength = systemConfigService.getIntConfig(
        SystemConfigService.PASSWORD_MIN_LENGTH, 6);
    
    return ResponseEntity.ok(Map.of(
        "allowRegistration", allowRegistration,
        "requireEmailVerification", requireEmailVerification,
        "passwordMinLength", passwordMinLength
    ));
  }
}

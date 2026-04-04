package com.universal.qbank.controller;

import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.service.EmailService;
import com.universal.qbank.service.UserService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

  @Autowired private UserService userService;
  @Autowired private EmailService emailService;

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return null;
  }

  @GetMapping("/profile")
  public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }

    return userService
        .getUserById(userId)
        .map(user -> ResponseEntity.ok(toProfileResponse(user)))
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  /** 发送邮箱修改验证码 */
  @PostMapping("/send-email-verification")
  public ResponseEntity<?> sendEmailVerification(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    try {
      String userId = getUserIdFromToken(token);
      if (userId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
      }

      UserEntity user = userService.getUserById(userId).orElse(null);
      if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
      }

      String email = payload.get("email");
      String currentPassword = payload.get("currentPassword");
      if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
        return ResponseEntity.badRequest().body("邮箱格式不正确");
      }
      if (email.equalsIgnoreCase(user.getEmail() == null ? "" : user.getEmail())) {
        return ResponseEntity.badRequest().body("新邮箱不能与当前邮箱相同");
      }
      if (currentPassword == null || !currentPassword.equals(user.getPassword())) {
        return ResponseEntity.badRequest().body("当前密码错误，无法发送验证码");
      }
      if (userService.existsByEmail(email)) {
        return ResponseEntity.badRequest().body("该邮箱已被其他账号使用");
      }

      emailService.sendEmailChangeCode(email, userId);
      return ResponseEntity.ok("验证码已发送");
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PutMapping("/profile")
  public ResponseEntity<?> updateProfile(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, Object> payload) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }
    try {
      UserEntity existingUser = userService.getUserById(userId).orElse(null);
      if (existingUser == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
      }

      // 校验邮箱验证码
      String email = asString(payload.get("email"));
      String emailVerifyCode = asString(payload.get("emailVerifyCode"));
      boolean emailChanged = email != null && !email.equals(existingUser.getEmail());
      if (emailChanged) {
        if (emailVerifyCode == null || emailVerifyCode.isBlank()) {
          return ResponseEntity.badRequest().body("修改邮箱必须提供验证码");
        }
        if (!emailService.verifyEmailCode(email, emailVerifyCode, userId)) {
          return ResponseEntity.badRequest().body("邮箱验证码错误或已过期");
        }
      }
      UserEntity updatedUser =
          userService.updateProfile(
              userId,
              asString(payload.get("username")),
              email,
              asString(payload.get("avatarUrl")),
              asString(payload.get("nickname")),
              asString(payload.get("currentPassword")),
              asBoolean(payload.get("emailNotification")),
              asBoolean(payload.get("systemNotification")),
              asBoolean(payload.get("publicProfile")),
              asBoolean(payload.get("showActivity")));
      return ResponseEntity.ok(toProfileResponse(updatedUser));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Boolean asBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private Map<String, Object> toProfileResponse(UserEntity user) {
    Map<String, Object> profile = new HashMap<>();
    profile.put("id", user.getId());
    profile.put("username", user.getUsername());
    profile.put("email", user.getEmail());
    profile.put("avatarUrl", user.getAvatarUrl());
    profile.put("nickname", user.getNickname());
    profile.put("role", user.getRole());
    profile.put("status", user.getStatus());
    profile.put("emailNotification", user.getEmailNotification());
    profile.put("systemNotification", user.getSystemNotification());
    profile.put("publicProfile", user.getPublicProfile());
    profile.put("showActivity", user.getShowActivity());
    return profile;
  }

  @PutMapping("/password")
  public ResponseEntity<?> updatePassword(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    String userId = getUserIdFromToken(token);

    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }

    try {
      userService.updatePassword(userId, payload.get("oldPassword"), payload.get("newPassword"));
      return ResponseEntity.ok().build();
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}

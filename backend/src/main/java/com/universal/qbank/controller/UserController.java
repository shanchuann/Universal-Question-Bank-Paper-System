package com.universal.qbank.controller;

import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.service.UserService;
import com.universal.qbank.service.EmailService;
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
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  /** 发送邮箱修改验证码 */
  @PostMapping("/send-email-verification")
  public ResponseEntity<?> sendEmailVerification(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }
    String email = payload.get("email");
    if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
      return ResponseEntity.badRequest().body("邮箱格式不正确");
    }
    emailService.sendEmailChangeCode(email);
    return ResponseEntity.ok("验证码已发送");
  }

  @PutMapping("/profile")
  public ResponseEntity<?> updateProfile(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }
    try {
      // 校验邮箱验证码
      String email = payload.get("email");
      String emailVerifyCode = payload.get("emailVerifyCode");
      if (email != null && emailVerifyCode != null) {
        if (!emailService.verifyEmailCode(email, emailVerifyCode)) {
          return ResponseEntity.badRequest().body("邮箱验证码错误或已过期");
        }
      }
      UserEntity updatedUser =
          userService.updateProfile(
              userId, 
              payload.get("username"),
              payload.get("email"), 
              payload.get("avatarUrl"), 
              payload.get("nickname"),
              payload.get("currentPassword"));
      return ResponseEntity.ok(updatedUser);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
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

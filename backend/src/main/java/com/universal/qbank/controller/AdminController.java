package com.universal.qbank.controller;

import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.service.SystemConfigService;
import com.universal.qbank.service.UserService;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

  @Autowired private UserService userService;

  @Autowired private SystemConfigService systemConfigService;

  private boolean isAdmin(String token) {
    String userId = getUserIdFromToken(token);
    System.out.println("Checking admin access for token: " + token + ", userId: " + userId);
    if (userId == null) return false;
    
    // Allow fallback admin user
    if ("admin".equals(userId)) {
        System.out.println("Access granted for fallback admin");
        return true;
    }

    Optional<UserEntity> user = userService.getUserById(userId);
    System.out.println("User found in DB: " + user.isPresent());
    if (user.isPresent()) {
        System.out.println("User role: " + user.get().getRole());
    }
    return user.isPresent() && "ADMIN".equals(user.get().getRole());
  }

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return null;
  }

  @GetMapping("/users")
  public ResponseEntity<?> listUsers(
      @RequestHeader("Authorization") String token,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    if (!isAdmin(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }
    Page<UserEntity> users = userService.getUsers(role, PageRequest.of(page, size));
    return ResponseEntity.ok(users);
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<?> deleteUser(
      @RequestHeader("Authorization") String token, @PathVariable String id) {
    if (!isAdmin(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }
    userService.deleteUser(id);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/users/{id}/status")
  public ResponseEntity<?> updateUserStatus(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      @RequestBody Map<String, String> payload) {
    if (!isAdmin(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }
    String status = payload.get("status");
    UserEntity user = userService.updateUserStatus(id, status);
    return ResponseEntity.ok(user);
  }

  @PutMapping("/users/{id}/role")
  public ResponseEntity<?> updateUserRole(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      @RequestBody Map<String, String> payload) {
    if (!isAdmin(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }
    String role = payload.get("role");
    UserEntity user = userService.updateUserRole(id, role);
    return ResponseEntity.ok(user);
  }

  @GetMapping("/system/status")
  public ResponseEntity<?> getSystemStatus(@RequestHeader("Authorization") String token) {
    if (!isAdmin(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }
    boolean enabled = systemConfigService.isSystemEnabled();
    return ResponseEntity.ok(Map.of("enabled", enabled));
  }

  @PutMapping("/system/status")
  public ResponseEntity<?> setSystemStatus(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, Boolean> payload) {
    if (!isAdmin(token)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
    }
    Boolean enabled = payload.get("enabled");
    systemConfigService.setSystemEnabled(enabled);
    return ResponseEntity.ok(Map.of("enabled", enabled));
  }
}

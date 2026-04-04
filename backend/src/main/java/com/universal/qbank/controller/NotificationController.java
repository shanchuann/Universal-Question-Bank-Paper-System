package com.universal.qbank.controller;

import com.universal.qbank.entity.UserNotificationEntity;
import com.universal.qbank.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  @Autowired private NotificationService notificationService;

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return null;
  }

  @GetMapping
  public ResponseEntity<?> getNotifications(
      @RequestHeader("Authorization") String token,
      @RequestParam(defaultValue = "false") boolean unreadOnly,
      @RequestParam(defaultValue = "30") int limit) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    List<UserNotificationEntity> notifications =
        notificationService.listNotifications(userId, unreadOnly, limit);
    return ResponseEntity.ok(notifications);
  }

  @GetMapping("/unread-count")
  public ResponseEntity<?> getUnreadCount(@RequestHeader("Authorization") String token) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
  }

  @PostMapping("/{id}/read")
  public ResponseEntity<?> markRead(
      @RequestHeader("Authorization") String token, @PathVariable String id) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    boolean success = notificationService.markAsRead(userId, id);
    if (!success) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "消息不存在"));
    }
    return ResponseEntity.ok(Map.of("message", "已标记为已读"));
  }

  @PostMapping("/read-all")
  public ResponseEntity<?> markAllRead(@RequestHeader("Authorization") String token) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    int changed = notificationService.markAllAsRead(userId);
    return ResponseEntity.ok(Map.of("message", "已全部标记为已读", "count", changed));
  }

  @GetMapping("/recipients")
  public ResponseEntity<?> getRecipients(@RequestHeader("Authorization") String token) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    return ResponseEntity.ok(notificationService.getAvailableRecipients(userId));
  }

  @GetMapping("/conversations")
  public ResponseEntity<?> getConversations(
      @RequestHeader("Authorization") String token, @RequestParam(defaultValue = "50") int limit) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    return ResponseEntity.ok(notificationService.getConversations(userId, limit));
  }

  @GetMapping("/chat/{peerId}")
  public ResponseEntity<?> getConversationMessages(
      @RequestHeader("Authorization") String token,
      @PathVariable String peerId,
      @RequestParam(defaultValue = "200") int limit) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    try {
      return ResponseEntity.ok(notificationService.getConversationMessages(userId, peerId, limit));
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/conversation/{peerId}/read")
  public ResponseEntity<?> markConversationRead(
      @RequestHeader("Authorization") String token, @PathVariable String peerId) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }
    int changed = notificationService.markConversationAsRead(userId, peerId);
    return ResponseEntity.ok(Map.of("message", "会话已读更新完成", "count", changed));
  }

  @PostMapping("/private")
  public ResponseEntity<?> sendPrivateMessage(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
    }

    try {
      UserNotificationEntity message =
          notificationService.sendPrivateMessage(
              userId,
              payload.get("receiverId"),
              payload.get("title"),
              payload.get("content"),
              payload.get("type"));
      return ResponseEntity.ok(message);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}

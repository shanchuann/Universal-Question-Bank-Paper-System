package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "operation_logs",
    indexes = {
      @Index(name = "idx_operation_log_timestamp", columnList = "timestamp"),
      @Index(name = "idx_operation_log_action_timestamp", columnList = "action, timestamp"),
      @Index(name = "idx_operation_log_user_timestamp", columnList = "userId, timestamp")
    })
public class OperationLogEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String userId;

  private String username;

  @Column(nullable = false)
  private String action; // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, EXPORT

  private String target; // 操作目标类型：用户、题目、试卷等

  private String targetId; // 目标对象ID

  private String ip;

  @Column(length = 2048)
  private String details;

  @Column(nullable = false)
  private OffsetDateTime timestamp;

  private String userAgent;

  @PrePersist
  public void prePersist() {
    this.timestamp = OffsetDateTime.now();
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}

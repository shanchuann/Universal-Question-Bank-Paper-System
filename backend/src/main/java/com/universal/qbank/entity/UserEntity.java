package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_user_role", columnList = "role"),
      @Index(name = "idx_user_email", columnList = "email"),
      @Index(name = "idx_user_created_at", columnList = "createdAt")
    })
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String password;

  private String email;

  @Column(length = 1024)
  private String avatarUrl;

  private String nickname;

  private String role; // ADMIN, USER

  private String status = "ACTIVE"; // ACTIVE, BANNED

  // 通知偏好
  private Boolean emailNotification = true;

  private Boolean systemNotification = true;

  // 隐私偏好
  private Boolean publicProfile = false;

  private Boolean showActivity = true;

  private OffsetDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = OffsetDateTime.now();
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Boolean getEmailNotification() {
    return emailNotification;
  }

  public void setEmailNotification(Boolean emailNotification) {
    this.emailNotification = emailNotification;
  }

  public Boolean getSystemNotification() {
    return systemNotification;
  }

  public void setSystemNotification(Boolean systemNotification) {
    this.systemNotification = systemNotification;
  }

  public Boolean getPublicProfile() {
    return publicProfile;
  }

  public void setPublicProfile(Boolean publicProfile) {
    this.publicProfile = publicProfile;
  }

  public Boolean getShowActivity() {
    return showActivity;
  }

  public void setShowActivity(Boolean showActivity) {
    this.showActivity = showActivity;
  }
}

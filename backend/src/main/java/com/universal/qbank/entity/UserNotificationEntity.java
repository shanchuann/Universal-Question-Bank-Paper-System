package com.universal.qbank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "user_notifications",
    indexes = {
      @Index(name = "idx_notification_receiver_created", columnList = "receiverId, createdAt"),
      @Index(name = "idx_notification_receiver_read", columnList = "receiverId, isRead")
    })
public class UserNotificationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String receiverId;

  private String senderId;

  private String senderName;

  private String senderRole;

  @Column(nullable = false, length = 32)
  private String type = "SYSTEM";

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 3000)
  private String content;

  private String relatedId;

  @Column(nullable = false)
  private Boolean isRead = false;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (isRead == null) {
      isRead = false;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(String receiverId) {
    this.receiverId = receiverId;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
  }

  public String getSenderRole() {
    return senderRole;
  }

  public void setSenderRole(String senderRole) {
    this.senderRole = senderRole;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getRelatedId() {
    return relatedId;
  }

  public void setRelatedId(String relatedId) {
    this.relatedId = relatedId;
  }

  public Boolean getIsRead() {
    return isRead;
  }

  public void setIsRead(Boolean isRead) {
    this.isRead = isRead;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}

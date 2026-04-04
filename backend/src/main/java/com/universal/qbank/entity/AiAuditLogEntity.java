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
    name = "ai_audit_logs",
    indexes = {
      @Index(name = "idx_ai_audit_user_created", columnList = "userId, createdAt"),
      @Index(name = "idx_ai_audit_feature_created", columnList = "feature, createdAt"),
      @Index(name = "idx_ai_audit_accepted", columnList = "accepted")
    })
public class AiAuditLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, length = 80)
  private String feature;

  @Column(nullable = false)
  private String userId;

  private String userRole;

  @Column(length = 5000)
  private String promptText;

  @Column(length = 8000)
  private String responseText;

  @Column(length = 3000)
  private String contextText;

  @Column(length = 120)
  private String contextRef;

  @Column(length = 100)
  private String modelName;

  private Boolean success = true;

  @Column(length = 1000)
  private String errorMessage;

  private Long latencyMs;

  private Boolean accepted = false;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (accepted == null) {
      accepted = false;
    }
    if (success == null) {
      success = true;
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFeature() {
    return feature;
  }

  public void setFeature(String feature) {
    this.feature = feature;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserRole() {
    return userRole;
  }

  public void setUserRole(String userRole) {
    this.userRole = userRole;
  }

  public String getPromptText() {
    return promptText;
  }

  public void setPromptText(String promptText) {
    this.promptText = promptText;
  }

  public String getResponseText() {
    return responseText;
  }

  public void setResponseText(String responseText) {
    this.responseText = responseText;
  }

  public String getContextText() {
    return contextText;
  }

  public void setContextText(String contextText) {
    this.contextText = contextText;
  }

  public String getContextRef() {
    return contextRef;
  }

  public void setContextRef(String contextRef) {
    this.contextRef = contextRef;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Long getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(Long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public Boolean getAccepted() {
    return accepted;
  }

  public void setAccepted(Boolean accepted) {
    this.accepted = accepted;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}

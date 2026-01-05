package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 题目审核记录实体
 */
@Entity
@Table(name = "question_reviews")
public class QuestionReviewEntity {

  @Id private String id;

  @Column(nullable = false)
  private String questionId;

  /** 针对哪个版本 */
  private Integer version;

  /** 审核动作: SUBMIT, APPROVE, REJECT, REVISE */
  @Column(nullable = false, length = 20)
  private String action;

  /** 操作人ID */
  private String reviewerId;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String notes;

  private OffsetDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
    if (this.createdAt == null) {
      this.createdAt = OffsetDateTime.now();
    }
  }

  // Getters and Setters

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getQuestionId() {
    return questionId;
  }

  public void setQuestionId(String questionId) {
    this.questionId = questionId;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(String reviewerId) {
    this.reviewerId = reviewerId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}

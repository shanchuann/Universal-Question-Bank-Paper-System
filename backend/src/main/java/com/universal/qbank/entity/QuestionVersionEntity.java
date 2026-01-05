package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 题目版本历史实体
 */
@Entity
@Table(name = "question_versions")
public class QuestionVersionEntity {

  @Id private String id;

  @Column(nullable = false)
  private String questionId;

  @Column(nullable = false)
  private Integer version;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String stem;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String optionsJson;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String answerSchema;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String analysis;

  private String difficulty;

  /** 修改人ID */
  private String changedBy;

  private OffsetDateTime changedAt;

  /** 变更摘要 */
  @Column(length = 500)
  private String changeSummary;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
    if (this.changedAt == null) {
      this.changedAt = OffsetDateTime.now();
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

  public String getStem() {
    return stem;
  }

  public void setStem(String stem) {
    this.stem = stem;
  }

  public String getOptionsJson() {
    return optionsJson;
  }

  public void setOptionsJson(String optionsJson) {
    this.optionsJson = optionsJson;
  }

  public String getAnswerSchema() {
    return answerSchema;
  }

  public void setAnswerSchema(String answerSchema) {
    this.answerSchema = answerSchema;
  }

  public String getAnalysis() {
    return analysis;
  }

  public void setAnalysis(String analysis) {
    this.analysis = analysis;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public OffsetDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(OffsetDateTime changedAt) {
    this.changedAt = changedAt;
  }

  public String getChangeSummary() {
    return changeSummary;
  }

  public void setChangeSummary(String changeSummary) {
    this.changeSummary = changeSummary;
  }
}

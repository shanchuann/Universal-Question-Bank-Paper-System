package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class QuestionEntity {

  @Id private String id;

  private String subjectId;

  private String type; // SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE

  private String difficulty; // EASY, MEDIUM, HARD

  /** 状态: DRAFT, PENDING_REVIEW, APPROVED, REJECTED, ARCHIVED */
  private String status; // DRAFT, PENDING_REVIEW, APPROVED, REJECTED, ARCHIVED

  /** 当前版本号 */
  private Integer version = 1;

  /** 创建者ID */
  private String createdBy;

  /** 审核人ID */
  private String reviewerId;

  /** 审核时间 */
  private OffsetDateTime reviewedAt;

  /** 审核意见 */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String reviewNotes;

  /** 所属组织ID (null表示共用题库) */
  private String organizationId;

  @ElementCollection(fetch = FetchType.EAGER)
  private List<String> tags;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "question_knowledge_points",
      joinColumns = @JoinColumn(name = "question_id"))
  @Column(name = "knowledge_point_id")
  private List<String> knowledgePointIds;

  private OffsetDateTime createdAt;

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

  public String getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(String subjectId) {
    this.subjectId = subjectId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
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

  public List<String> getKnowledgePointIds() {
    return knowledgePointIds;
  }

  public void setKnowledgePointIds(List<String> knowledgePointIds) {
    this.knowledgePointIds = knowledgePointIds;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(String reviewerId) {
    this.reviewerId = reviewerId;
  }

  public OffsetDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(OffsetDateTime reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getReviewNotes() {
    return reviewNotes;
  }

  public void setReviewNotes(String reviewNotes) {
    this.reviewNotes = reviewNotes;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }
}

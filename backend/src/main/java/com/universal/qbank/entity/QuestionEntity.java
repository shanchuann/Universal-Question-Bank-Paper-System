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

  private String status; // DRAFT, PUBLISHED

  @ElementCollection private List<String> tags;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "question_knowledge_points",
      joinColumns = @JoinColumn(name = "question_id"))
  @Column(name = "knowledge_point_id")
  private List<String> knowledgePointIds;

  private OffsetDateTime createdAt;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String stem;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String optionsJson;

  @Lob
  @Column(columnDefinition = "CLOB")
  private String answerSchema;

  @Lob
  @Column(columnDefinition = "CLOB")
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
}

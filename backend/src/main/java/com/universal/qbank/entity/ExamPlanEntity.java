package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 考试计划实体
 */
@Entity
@Table(name = "exam_plans")
public class ExamPlanEntity {

  @Id private String id;

  @Column(nullable = false, length = 100)
  private String name;

  /** 关联试卷ID */
  private Long paperId;

  /** 关联课程ID */
  private String courseId;

  /** 考试类型: FORMAL(正式), MAKEUP(补考), RETAKE(重考) */
  @Column(length = 20)
  private String examType = "FORMAL";

  /** 状态: DRAFT, PUBLISHED, ONGOING, FINISHED, CANCELLED */
  @Column(length = 20)
  private String status = "DRAFT";

  /** 考试开始时间 */
  private OffsetDateTime startTime;

  /** 考试结束时间 */
  private OffsetDateTime endTime;

  /** 考试时长（分钟） */
  private Integer durationMins;

  /** 最大尝试次数 */
  private Integer maxAttempts = 1;

  /** 及格分数 */
  private BigDecimal passScore;

  /** 允许查看解析 */
  private Boolean allowReview = false;

  /** 题目乱序 */
  private Boolean shuffleQuestions = false;

  /** 选项乱序 */
  private Boolean shuffleOptions = false;

  /** 创建人 */
  private String createdBy;

  private OffsetDateTime createdAt;

  /** 发布时间 */
  private OffsetDateTime publishedAt;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getPaperId() {
    return paperId;
  }

  public void setPaperId(Long paperId) {
    this.paperId = paperId;
  }

  public String getCourseId() {
    return courseId;
  }

  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  public String getExamType() {
    return examType;
  }

  public void setExamType(String examType) {
    this.examType = examType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime = startTime;
  }

  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime = endTime;
  }

  public Integer getDurationMins() {
    return durationMins;
  }

  public void setDurationMins(Integer durationMins) {
    this.durationMins = durationMins;
  }

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public BigDecimal getPassScore() {
    return passScore;
  }

  public void setPassScore(BigDecimal passScore) {
    this.passScore = passScore;
  }

  public Boolean getAllowReview() {
    return allowReview;
  }

  public void setAllowReview(Boolean allowReview) {
    this.allowReview = allowReview;
  }

  public Boolean getShuffleQuestions() {
    return shuffleQuestions;
  }

  public void setShuffleQuestions(Boolean shuffleQuestions) {
    this.shuffleQuestions = shuffleQuestions;
  }

  public Boolean getShuffleOptions() {
    return shuffleOptions;
  }

  public void setShuffleOptions(Boolean shuffleOptions) {
    this.shuffleOptions = shuffleOptions;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }
}

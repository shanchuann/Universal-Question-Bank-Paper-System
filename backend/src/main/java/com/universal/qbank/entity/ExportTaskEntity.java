package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 导出任务实体
 */
@Entity
@Table(name = "export_tasks")
public class ExportTaskEntity {

  @Id private String id;

  /** 导出类型: PAPER(试卷), ANSWER_SHEET(答题卡), ANALYSIS(解析) */
  @Column(length = 20)
  private String type;

  /** 格式: PDF, DOCX */
  @Column(length = 10)
  private String format;

  /** 关联试卷ID */
  private Long paperId;

  /** 关联考试计划ID */
  private String examPlanId;

  /** 导出配置JSON（页边距、字体等） */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String optionsJson;

  /** 状态: PENDING, PROCESSING, DONE, FAILED */
  @Column(length = 20)
  private String status = "PENDING";

  /** 生成文件URL */
  @Column(length = 500)
  private String fileUrl;

  /** 文件大小（字节） */
  private Long fileSize;

  /** 错误信息 */
  @Lob
  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  /** 创建人 */
  private String createdBy;

  private OffsetDateTime createdAt;

  /** 完成时间 */
  private OffsetDateTime completedAt;

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public Long getPaperId() {
    return paperId;
  }

  public void setPaperId(Long paperId) {
    this.paperId = paperId;
  }

  public String getExamPlanId() {
    return examPlanId;
  }

  public void setExamPlanId(String examPlanId) {
    this.examPlanId = examPlanId;
  }

  public String getOptionsJson() {
    return optionsJson;
  }

  public void setOptionsJson(String optionsJson) {
    this.optionsJson = optionsJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
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

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(OffsetDateTime completedAt) {
    this.completedAt = completedAt;
  }
}

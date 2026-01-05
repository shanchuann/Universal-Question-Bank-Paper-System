package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 考试报名/名单实体
 */
@Entity
@Table(name = "exam_enrollments")
public class ExamEnrollmentEntity {

  @Id private String id;

  @Column(nullable = false)
  private String examPlanId;

  @Column(nullable = false)
  private String studentId;

  /** 班级ID */
  private String classId;

  /** 状态: ENROLLED(已报名), EXEMPTED(免考), ABSENT(缺考) */
  @Column(length = 20)
  private String status = "ENROLLED";

  /** 座位号 */
  @Column(length = 20)
  private String seatNumber;

  /** 已使用尝试次数 */
  private Integer attemptsUsed = 0;

  /** 最高得分 */
  private BigDecimal bestScore;

  private OffsetDateTime enrolledAt;

  /** 免考说明 */
  @Column(length = 255)
  private String exemptionNote;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
    if (this.enrolledAt == null) {
      this.enrolledAt = OffsetDateTime.now();
    }
  }

  // Getters and Setters

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getExamPlanId() {
    return examPlanId;
  }

  public void setExamPlanId(String examPlanId) {
    this.examPlanId = examPlanId;
  }

  public String getStudentId() {
    return studentId;
  }

  public void setStudentId(String studentId) {
    this.studentId = studentId;
  }

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classId) {
    this.classId = classId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSeatNumber() {
    return seatNumber;
  }

  public void setSeatNumber(String seatNumber) {
    this.seatNumber = seatNumber;
  }

  public Integer getAttemptsUsed() {
    return attemptsUsed;
  }

  public void setAttemptsUsed(Integer attemptsUsed) {
    this.attemptsUsed = attemptsUsed;
  }

  public BigDecimal getBestScore() {
    return bestScore;
  }

  public void setBestScore(BigDecimal bestScore) {
    this.bestScore = bestScore;
  }

  public OffsetDateTime getEnrolledAt() {
    return enrolledAt;
  }

  public void setEnrolledAt(OffsetDateTime enrolledAt) {
    this.enrolledAt = enrolledAt;
  }

  public String getExemptionNote() {
    return exemptionNote;
  }

  public void setExemptionNote(String exemptionNote) {
    this.exemptionNote = exemptionNote;
  }
}

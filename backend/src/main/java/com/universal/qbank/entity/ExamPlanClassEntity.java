package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * 考试计划-班级关联实体
 */
@Entity
@Table(name = "exam_plan_classes")
@IdClass(ExamPlanClassEntity.ExamPlanClassId.class)
public class ExamPlanClassEntity {

  @Id
  @Column(nullable = false)
  private String examPlanId;

  @Id
  @Column(nullable = false)
  private String classId;

  // Getters and Setters

  public String getExamPlanId() {
    return examPlanId;
  }

  public void setExamPlanId(String examPlanId) {
    this.examPlanId = examPlanId;
  }

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classId) {
    this.classId = classId;
  }

  /** 复合主键类 */
  public static class ExamPlanClassId implements Serializable {
    private String examPlanId;
    private String classId;

    public ExamPlanClassId() {}

    public ExamPlanClassId(String examPlanId, String classId) {
      this.examPlanId = examPlanId;
      this.classId = classId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ExamPlanClassId that = (ExamPlanClassId) o;
      return Objects.equals(examPlanId, that.examPlanId) && Objects.equals(classId, that.classId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(examPlanId, classId);
    }
  }
}

package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * 教师-班级-课程关联实体
 */
@Entity
@Table(name = "teacher_classes")
public class TeacherClassEntity {

  @Id private String id;

  @Column(nullable = false)
  private String teacherId;

  @Column(nullable = false)
  private String classId;

  @Column(nullable = false)
  private String courseId;

  /** 学期: 如 2024-1, 2024-2 */
  @Column(length = 20)
  private String semester;

  /** 是否主讲教师 */
  private Boolean isPrimary = true;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
  }

  // Getters and Setters

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTeacherId() {
    return teacherId;
  }

  public void setTeacherId(String teacherId) {
    this.teacherId = teacherId;
  }

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classId) {
    this.classId = classId;
  }

  public String getCourseId() {
    return courseId;
  }

  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  public String getSemester() {
    return semester;
  }

  public void setSemester(String semester) {
    this.semester = semester;
  }

  public Boolean getIsPrimary() {
    return isPrimary;
  }

  public void setIsPrimary(Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }
}

package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 课程实体
 */
@Entity
@Table(name = "courses")
public class CourseEntity {

  @Id private String id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(unique = true, length = 50)
  private String code;

  /** 所属学院ID */
  private String departmentId;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String description;

  /** 状态: ACTIVE, ARCHIVED */
  @Column(length = 20)
  private String status = "ACTIVE";

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(String departmentId) {
    this.departmentId = departmentId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}

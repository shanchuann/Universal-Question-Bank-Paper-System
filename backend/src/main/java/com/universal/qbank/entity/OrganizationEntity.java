package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 组织架构实体 - 支持学校/学院/班级等层级结构
 */
@Entity
@Table(name = "organizations")
public class OrganizationEntity {

  @Id private String id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(unique = true, length = 50)
  private String code;

  /** 组织类型: SCHOOL, DEPARTMENT, CLASS */
  @Column(nullable = false, length = 20)
  private String type;

  /** 上级组织ID */
  private String parentId;

  /** 排序 */
  private Integer sortOrder = 0;

  /** 状态: ACTIVE, INACTIVE */
  @Column(length = 20)
  private String status = "ACTIVE";

  /** 邀请码 - 用于学生加入班级 */
  @Column(length = 20, unique = true)
  private String inviteCode;

  /** 创建者ID */
  private String createdBy;

  private OffsetDateTime createdAt;

  private OffsetDateTime updatedAt;

  @Transient private List<OrganizationEntity> children = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
    if (this.createdAt == null) {
      this.createdAt = OffsetDateTime.now();
    }
    this.updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = OffsetDateTime.now();
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
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

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<OrganizationEntity> getChildren() {
    return children;
  }

  public void setChildren(List<OrganizationEntity> children) {
    this.children = children;
  }
}

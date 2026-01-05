package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 用户-组织关联实体
 */
@Entity
@Table(name = "user_organizations")
public class UserOrganizationEntity {

  @Id private String id;

  @Column(nullable = false)
  private String userId;

  @Column(nullable = false)
  private String organizationId;

  /** 在组织中的角色: MEMBER, MANAGER, OWNER */
  @Column(length = 20)
  private String roleInOrg = "MEMBER";

  private OffsetDateTime joinedAt;

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
    if (this.joinedAt == null) {
      this.joinedAt = OffsetDateTime.now();
    }
  }

  // Getters and Setters

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getRoleInOrg() {
    return roleInOrg;
  }

  public void setRoleInOrg(String roleInOrg) {
    this.roleInOrg = roleInOrg;
  }

  public OffsetDateTime getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(OffsetDateTime joinedAt) {
    this.joinedAt = joinedAt;
  }
}

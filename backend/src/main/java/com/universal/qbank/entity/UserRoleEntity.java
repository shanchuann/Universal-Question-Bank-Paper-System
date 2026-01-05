package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * 用户-角色关联实体（支持按组织分配角色）
 */
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleEntity.UserRoleId.class)
public class UserRoleEntity {

  @Id
  @Column(nullable = false)
  private String userId;

  @Id
  @Column(nullable = false)
  private String roleId;

  /** 权限作用域（组织ID），为空表示全局 */
  @Id
  @Column(nullable = false)
  private String scopeOrgId = "";

  // Getters and Setters

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getScopeOrgId() {
    return scopeOrgId;
  }

  public void setScopeOrgId(String scopeOrgId) {
    this.scopeOrgId = scopeOrgId != null ? scopeOrgId : "";
  }

  /** 复合主键类 */
  public static class UserRoleId implements Serializable {
    private String userId;
    private String roleId;
    private String scopeOrgId;

    public UserRoleId() {}

    public UserRoleId(String userId, String roleId, String scopeOrgId) {
      this.userId = userId;
      this.roleId = roleId;
      this.scopeOrgId = scopeOrgId != null ? scopeOrgId : "";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UserRoleId that = (UserRoleId) o;
      return Objects.equals(userId, that.userId)
          && Objects.equals(roleId, that.roleId)
          && Objects.equals(scopeOrgId, that.scopeOrgId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, roleId, scopeOrgId);
    }
  }
}

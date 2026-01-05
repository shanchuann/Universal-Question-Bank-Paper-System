package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * 角色-权限关联实体
 */
@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionEntity.RolePermissionId.class)
public class RolePermissionEntity {

  @Id
  @Column(nullable = false)
  private String roleId;

  @Id
  @Column(nullable = false)
  private String permissionId;

  // Getters and Setters

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getPermissionId() {
    return permissionId;
  }

  public void setPermissionId(String permissionId) {
    this.permissionId = permissionId;
  }

  /** 复合主键类 */
  public static class RolePermissionId implements Serializable {
    private String roleId;
    private String permissionId;

    public RolePermissionId() {}

    public RolePermissionId(String roleId, String permissionId) {
      this.roleId = roleId;
      this.permissionId = permissionId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RolePermissionId that = (RolePermissionId) o;
      return Objects.equals(roleId, that.roleId)
          && Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(roleId, permissionId);
    }
  }
}

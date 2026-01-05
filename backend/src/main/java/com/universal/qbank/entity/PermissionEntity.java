package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * 权限点实体
 */
@Entity
@Table(name = "permissions")
public class PermissionEntity {

  @Id private String id;

  @Column(nullable = false, length = 100)
  private String name;

  /** 权限编码，如 question:create, paper:read:own */
  @Column(unique = true, nullable = false, length = 100)
  private String code;

  /** 资源类型: question, paper, exam, user, org, system */
  @Column(length = 50)
  private String resource;

  /** 动作: create, read, update, delete, review, publish, export */
  @Column(length = 20)
  private String action;

  /** 数据范围: ALL, DEPT, OWN */
  @Column(length = 20)
  private String scope;

  @Column(length = 255)
  private String description;

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

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

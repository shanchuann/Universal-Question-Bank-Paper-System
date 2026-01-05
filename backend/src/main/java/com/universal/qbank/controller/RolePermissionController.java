package com.universal.qbank.controller;

import com.universal.qbank.entity.PermissionEntity;
import com.universal.qbank.entity.RoleEntity;
import com.universal.qbank.service.PermissionService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RolePermissionController {

  @Autowired private PermissionService permissionService;

  /** 获取所有角色 */
  @GetMapping("/roles")
  public ResponseEntity<List<RoleEntity>> getAllRoles() {
    return ResponseEntity.ok(permissionService.getAllRoles());
  }

  /** 创建角色 */
  @PostMapping("/roles")
  public ResponseEntity<RoleEntity> createRole(@RequestBody RoleEntity role) {
    RoleEntity created = permissionService.createRole(role);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /** 获取角色权限 */
  @GetMapping("/roles/{roleId}/permissions")
  public ResponseEntity<List<PermissionEntity>> getRolePermissions(@PathVariable String roleId) {
    return ResponseEntity.ok(permissionService.getRolePermissions(roleId));
  }

  /** 更新角色权限 */
  @PutMapping("/roles/{roleId}/permissions")
  public ResponseEntity<Void> updateRolePermissions(
      @PathVariable String roleId, @RequestBody List<String> permissionIds) {
    permissionService.updateRolePermissions(roleId, permissionIds);
    return ResponseEntity.ok().build();
  }

  /** 获取所有权限点 */
  @GetMapping("/permissions")
  public ResponseEntity<List<PermissionEntity>> getAllPermissions() {
    return ResponseEntity.ok(permissionService.getAllPermissions());
  }

  /** 获取用户角色 */
  @GetMapping("/users/{userId}/roles")
  public ResponseEntity<List<RoleEntity>> getUserRoles(@PathVariable String userId) {
    return ResponseEntity.ok(permissionService.getUserRoles(userId));
  }

  /** 分配用户角色 */
  @PostMapping("/users/{userId}/roles")
  public ResponseEntity<Void> assignUserRole(
      @PathVariable String userId, @RequestBody Map<String, String> request) {
    String roleId = request.get("roleId");
    String scopeOrgId = request.get("scopeOrgId");
    permissionService.assignUserRole(userId, roleId, scopeOrgId);
    return ResponseEntity.ok().build();
  }

  /** 移除用户角色 */
  @DeleteMapping("/users/{userId}/roles/{roleId}")
  public ResponseEntity<Void> removeUserRole(
      @PathVariable String userId,
      @PathVariable String roleId,
      @RequestParam(required = false) String scopeOrgId) {
    permissionService.removeUserRole(userId, roleId, scopeOrgId);
    return ResponseEntity.noContent().build();
  }

  /** 获取当前用户权限 */
  @GetMapping("/users/{userId}/permissions")
  public ResponseEntity<Set<String>> getUserPermissions(@PathVariable String userId) {
    return ResponseEntity.ok(permissionService.getUserPermissions(userId));
  }
}

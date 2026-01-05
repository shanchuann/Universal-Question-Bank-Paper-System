package com.universal.qbank.service;

import com.universal.qbank.entity.*;
import com.universal.qbank.repository.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

  @Autowired private RoleRepository roleRepository;

  @Autowired private PermissionRepository permissionRepository;

  @Autowired private RolePermissionRepository rolePermissionRepository;

  @Autowired private UserRoleRepository userRoleRepository;

  /** 获取用户的所有权限编码 */
  public Set<String> getUserPermissions(String userId) {
    List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
    Set<String> permissions = new HashSet<>();

    for (UserRoleEntity ur : userRoles) {
      List<String> rolePerm = rolePermissionRepository.findPermissionCodesByRoleId(ur.getRoleId());
      permissions.addAll(rolePerm);
    }

    return permissions;
  }

  /** 检查用户是否有指定权限 */
  public boolean hasPermission(String userId, String permissionCode) {
    Set<String> permissions = getUserPermissions(userId);
    return permissions.contains(permissionCode);
  }

  /** 检查用户是否有指定角色 */
  public boolean hasRole(String userId, String roleCode) {
    List<String> roleCodes = userRoleRepository.findRoleCodesByUserId(userId);
    return roleCodes.contains(roleCode);
  }

  /** 获取所有角色 */
  public List<RoleEntity> getAllRoles() {
    return roleRepository.findAll();
  }

  /** 获取所有权限点 */
  public List<PermissionEntity> getAllPermissions() {
    return permissionRepository.findAll();
  }

  /** 创建角色 */
  @Transactional
  public RoleEntity createRole(RoleEntity role) {
    return roleRepository.save(role);
  }

  /** 更新角色权限 */
  @Transactional
  public void updateRolePermissions(String roleId, List<String> permissionIds) {
    rolePermissionRepository.deleteByRoleId(roleId);

    for (String permId : permissionIds) {
      RolePermissionEntity rp = new RolePermissionEntity();
      rp.setRoleId(roleId);
      rp.setPermissionId(permId);
      rolePermissionRepository.save(rp);
    }
  }

  /** 为用户分配角色 */
  @Transactional
  public void assignUserRole(String userId, String roleId, String scopeOrgId) {
    UserRoleEntity ur = new UserRoleEntity();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setScopeOrgId(scopeOrgId != null ? scopeOrgId : "");
    userRoleRepository.save(ur);
  }

  /** 移除用户角色 */
  @Transactional
  public void removeUserRole(String userId, String roleId, String scopeOrgId) {
    UserRoleEntity.UserRoleId id =
        new UserRoleEntity.UserRoleId(userId, roleId, scopeOrgId != null ? scopeOrgId : "");
    userRoleRepository.deleteById(id);
  }

  /** 获取用户的角色列表 */
  public List<RoleEntity> getUserRoles(String userId) {
    List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
    List<String> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
    return roleRepository.findAllById(roleIds);
  }

  /** 获取角色的权限列表 */
  public List<PermissionEntity> getRolePermissions(String roleId) {
    List<RolePermissionEntity> rolePerms = rolePermissionRepository.findByRoleId(roleId);
    List<String> permIds = rolePerms.stream().map(RolePermissionEntity::getPermissionId).toList();
    return permissionRepository.findAllById(permIds);
  }
}

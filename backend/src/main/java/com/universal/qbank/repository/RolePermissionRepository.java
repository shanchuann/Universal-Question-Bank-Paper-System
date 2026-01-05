package com.universal.qbank.repository;

import com.universal.qbank.entity.RolePermissionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository
    extends JpaRepository<RolePermissionEntity, RolePermissionEntity.RolePermissionId> {

  List<RolePermissionEntity> findByRoleId(String roleId);

  void deleteByRoleId(String roleId);

  @Query(
      "SELECT p.code FROM RolePermissionEntity rp "
          + "JOIN PermissionEntity p ON rp.permissionId = p.id "
          + "WHERE rp.roleId = :roleId")
  List<String> findPermissionCodesByRoleId(String roleId);
}

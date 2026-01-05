package com.universal.qbank.repository;

import com.universal.qbank.entity.UserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository
    extends JpaRepository<UserRoleEntity, UserRoleEntity.UserRoleId> {

  List<UserRoleEntity> findByUserId(String userId);

  List<UserRoleEntity> findByRoleId(String roleId);

  void deleteByUserId(String userId);

  @Query(
      "SELECT r.code FROM UserRoleEntity ur "
          + "JOIN RoleEntity r ON ur.roleId = r.id "
          + "WHERE ur.userId = :userId")
  List<String> findRoleCodesByUserId(String userId);
}

package com.universal.qbank.repository;

import com.universal.qbank.entity.OrganizationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, String> {

  List<OrganizationEntity> findByParentIdIsNullOrderBySortOrder();

  List<OrganizationEntity> findByParentIdOrderBySortOrder(String parentId);

  List<OrganizationEntity> findByType(String type);

  List<OrganizationEntity> findByTypeAndStatus(String type, String status);

  boolean existsByCode(String code);

  Optional<OrganizationEntity> findByInviteCode(String inviteCode);

  /** 获取用户创建的组织 */
  List<OrganizationEntity> findByCreatedBy(String createdBy);
}

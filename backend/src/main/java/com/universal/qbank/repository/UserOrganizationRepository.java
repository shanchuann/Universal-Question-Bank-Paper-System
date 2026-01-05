package com.universal.qbank.repository;

import com.universal.qbank.entity.UserOrganizationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganizationEntity, String> {

  List<UserOrganizationEntity> findByUserId(String userId);

  List<UserOrganizationEntity> findByOrganizationId(String organizationId);

  void deleteByUserIdAndOrganizationId(String userId, String organizationId);

  Optional<UserOrganizationEntity> findByUserIdAndOrganizationId(String userId, String organizationId);

  boolean existsByUserIdAndOrganizationId(String userId, String organizationId);

  long countByOrganizationId(String organizationId);
}

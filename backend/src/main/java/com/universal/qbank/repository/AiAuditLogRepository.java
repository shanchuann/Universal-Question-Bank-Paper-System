package com.universal.qbank.repository;

import com.universal.qbank.entity.AiAuditLogEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiAuditLogRepository extends JpaRepository<AiAuditLogEntity, String> {

  List<AiAuditLogEntity> findTop100ByUserIdOrderByCreatedAtDesc(String userId);

  List<AiAuditLogEntity> findTop200ByFeatureAndCreatedAtAfterOrderByCreatedAtDesc(
      String feature, OffsetDateTime createdAt);

  long countByFeatureAndCreatedAtAfter(String feature, OffsetDateTime createdAt);

  long countByFeatureAndAcceptedTrueAndCreatedAtAfter(String feature, OffsetDateTime createdAt);
}

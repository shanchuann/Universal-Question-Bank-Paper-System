package com.universal.qbank.repository;

import com.universal.qbank.entity.OperationLogEntity;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLogEntity, String> {

  Page<OperationLogEntity> findByAction(String action, Pageable pageable);

  Page<OperationLogEntity> findByUserId(String userId, Pageable pageable);

  @Query(
      "SELECT o FROM OperationLogEntity o WHERE "
          + "(:keyword IS NULL OR o.username LIKE %:keyword% OR o.details LIKE %:keyword%) AND "
          + "(:action IS NULL OR o.action = :action)")
  Page<OperationLogEntity> searchLogs(
      @Param("keyword") String keyword, @Param("action") String action, Pageable pageable);

  @Query("SELECT o FROM OperationLogEntity o WHERE o.timestamp >= :startTime")
  Page<OperationLogEntity> findByTimestampAfter(
      @Param("startTime") OffsetDateTime startTime, Pageable pageable);

  @Query(
      "SELECT COUNT(DISTINCT o.userId) FROM OperationLogEntity o WHERE o.timestamp >= :startTime AND o.timestamp < :endTime")
  long countDistinctUsersByTimestampBetween(
      @Param("startTime") OffsetDateTime startTime, @Param("endTime") OffsetDateTime endTime);

  long countByTimestampAfter(OffsetDateTime startTime);

  long countByAction(String action);
}

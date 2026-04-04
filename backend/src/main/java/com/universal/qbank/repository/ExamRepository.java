package com.universal.qbank.repository;

import com.universal.qbank.entity.ExamEntity;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExamRepository
    extends JpaRepository<ExamEntity, Long>, JpaSpecificationExecutor<ExamEntity> {
  long countByStartTimeBetween(OffsetDateTime start, OffsetDateTime end);
}

package com.universal.qbank.repository;

import com.universal.qbank.entity.PaperEntity;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<PaperEntity, Long> {
  long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}

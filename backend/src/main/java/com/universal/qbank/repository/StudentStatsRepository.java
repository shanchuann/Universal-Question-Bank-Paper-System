package com.universal.qbank.repository;

import com.universal.qbank.entity.StudentStatsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentStatsRepository extends JpaRepository<StudentStatsEntity, Long> {
  Optional<StudentStatsEntity> findByUserId(String userId);
}

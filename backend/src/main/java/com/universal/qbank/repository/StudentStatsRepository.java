package com.universal.qbank.repository;

import com.universal.qbank.entity.StudentStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentStatsRepository extends JpaRepository<StudentStatsEntity, Long> {
    Optional<StudentStatsEntity> findByUserId(String userId);
}

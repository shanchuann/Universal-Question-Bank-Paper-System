package com.universal.qbank.repository;

import com.universal.qbank.entity.ExamPlanEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamPlanRepository extends JpaRepository<ExamPlanEntity, String> {

  List<ExamPlanEntity> findByCourseId(String courseId);

  List<ExamPlanEntity> findByStatus(String status);

  Page<ExamPlanEntity> findByCreatedBy(String createdBy, Pageable pageable);

  @Query(
      "SELECT e FROM ExamPlanEntity e WHERE e.status = 'PUBLISHED' "
          + "AND e.startTime <= :now AND e.endTime >= :now")
  List<ExamPlanEntity> findActiveExams(OffsetDateTime now);

  @Query(
      "SELECT e FROM ExamPlanEntity e WHERE e.status = 'PUBLISHED' "
          + "AND e.startTime > :now ORDER BY e.startTime ASC")
  List<ExamPlanEntity> findUpcomingExams(OffsetDateTime now);
}

package com.universal.qbank.repository;

import com.universal.qbank.entity.ExamEnrollmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamEnrollmentRepository extends JpaRepository<ExamEnrollmentEntity, String> {

  List<ExamEnrollmentEntity> findByExamPlanId(String examPlanId);

  List<ExamEnrollmentEntity> findByStudentId(String studentId);

  Optional<ExamEnrollmentEntity> findByExamPlanIdAndStudentId(String examPlanId, String studentId);

  List<ExamEnrollmentEntity> findByExamPlanIdAndStatus(String examPlanId, String status);

  long countByExamPlanId(String examPlanId);

  long countByExamPlanIdAndStatus(String examPlanId, String status);
}

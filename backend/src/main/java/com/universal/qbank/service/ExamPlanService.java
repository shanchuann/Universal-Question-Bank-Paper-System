package com.universal.qbank.service;

import com.universal.qbank.entity.*;
import com.universal.qbank.repository.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamPlanService {

  @Autowired private ExamPlanRepository examPlanRepository;

  @Autowired private ExamEnrollmentRepository examEnrollmentRepository;

  @Autowired private UserOrganizationRepository userOrganizationRepository;

  /** 获取考试计划列表 */
  public Page<ExamPlanEntity> getExamPlans(Pageable pageable) {
    return examPlanRepository.findAll(pageable);
  }

  /** 获取单个考试计划 */
  public Optional<ExamPlanEntity> getById(String id) {
    return examPlanRepository.findById(id);
  }

  /** 创建考试计划 */
  @Transactional
  public ExamPlanEntity create(ExamPlanEntity plan) {
    plan.setStatus("DRAFT");
    return examPlanRepository.save(plan);
  }

  /** 更新考试计划 */
  @Transactional
  public ExamPlanEntity update(String id, ExamPlanEntity plan) {
    ExamPlanEntity existing =
        examPlanRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));

    if (!"DRAFT".equals(existing.getStatus())) {
      throw new RuntimeException("Only draft plans can be updated");
    }

    existing.setName(plan.getName());
    existing.setPaperId(plan.getPaperId());
    existing.setCourseId(plan.getCourseId());
    existing.setExamType(plan.getExamType());
    existing.setStartTime(plan.getStartTime());
    existing.setEndTime(plan.getEndTime());
    existing.setDurationMins(plan.getDurationMins());
    existing.setMaxAttempts(plan.getMaxAttempts());
    existing.setPassScore(plan.getPassScore());
    existing.setAllowReview(plan.getAllowReview());
    existing.setShuffleQuestions(plan.getShuffleQuestions());
    existing.setShuffleOptions(plan.getShuffleOptions());

    return examPlanRepository.save(existing);
  }

  /** 发布考试 */
  @Transactional
  public void publish(String id) {
    ExamPlanEntity plan =
        examPlanRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));

    if (!"DRAFT".equals(plan.getStatus())) {
      throw new RuntimeException("Only draft plans can be published");
    }

    if (plan.getPaperId() == null) {
      throw new RuntimeException("Paper must be selected before publishing");
    }

    plan.setStatus("PUBLISHED");
    plan.setPublishedAt(OffsetDateTime.now());
    examPlanRepository.save(plan);
  }

  /** 取消考试 */
  @Transactional
  public void cancel(String id) {
    ExamPlanEntity plan =
        examPlanRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));

    if ("FINISHED".equals(plan.getStatus())) {
      throw new RuntimeException("Finished exams cannot be cancelled");
    }

    plan.setStatus("CANCELLED");
    examPlanRepository.save(plan);
  }

  /** 按班级批量报名 */
  @Transactional
  public int enrollByClass(String examPlanId, String classId) {
    List<UserOrganizationEntity> members = userOrganizationRepository.findByOrganizationId(classId);
    int count = 0;

    for (UserOrganizationEntity member : members) {
      Optional<ExamEnrollmentEntity> existing =
          examEnrollmentRepository.findByExamPlanIdAndStudentId(examPlanId, member.getUserId());

      if (existing.isEmpty()) {
        ExamEnrollmentEntity enrollment = new ExamEnrollmentEntity();
        enrollment.setExamPlanId(examPlanId);
        enrollment.setStudentId(member.getUserId());
        enrollment.setClassId(classId);
        enrollment.setStatus("ENROLLED");
        examEnrollmentRepository.save(enrollment);
        count++;
      }
    }

    return count;
  }

  /** 获取报名名单 */
  public List<ExamEnrollmentEntity> getEnrollments(String examPlanId) {
    return examEnrollmentRepository.findByExamPlanId(examPlanId);
  }

  /** 标记缺考 */
  @Transactional
  public void markAbsent(String enrollmentId) {
    ExamEnrollmentEntity enrollment =
        examEnrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(() -> new RuntimeException("Not found"));

    enrollment.setStatus("ABSENT");
    examEnrollmentRepository.save(enrollment);
  }

  /** 获取学生的考试列表 */
  public List<ExamEnrollmentEntity> getStudentExams(String studentId) {
    return examEnrollmentRepository.findByStudentId(studentId);
  }

  /** 获取当前进行中的考试 */
  public List<ExamPlanEntity> getActiveExams() {
    return examPlanRepository.findActiveExams(OffsetDateTime.now());
  }

  /** 获取即将开始的考试 */
  public List<ExamPlanEntity> getUpcomingExams() {
    return examPlanRepository.findUpcomingExams(OffsetDateTime.now());
  }
}

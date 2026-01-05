package com.universal.qbank.controller;

import com.universal.qbank.entity.ExamEnrollmentEntity;
import com.universal.qbank.entity.ExamPlanEntity;
import com.universal.qbank.service.ExamPlanService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exam-plans")
@CrossOrigin(origins = "*")
public class ExamPlanController {

  @Autowired private ExamPlanService examPlanService;

  /** 获取考试计划列表 */
  @GetMapping
  public ResponseEntity<Page<ExamPlanEntity>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(examPlanService.getExamPlans(PageRequest.of(page, size)));
  }

  /** 获取单个考试计划 */
  @GetMapping("/{id}")
  public ResponseEntity<ExamPlanEntity> getById(@PathVariable String id) {
    return examPlanService
        .getById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** 创建考试计划 */
  @PostMapping
  public ResponseEntity<ExamPlanEntity> create(@RequestBody ExamPlanEntity plan) {
    ExamPlanEntity created = examPlanService.create(plan);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /** 更新考试计划 */
  @PutMapping("/{id}")
  public ResponseEntity<ExamPlanEntity> update(
      @PathVariable String id, @RequestBody ExamPlanEntity plan) {
    ExamPlanEntity updated = examPlanService.update(id, plan);
    return ResponseEntity.ok(updated);
  }

  /** 发布考试 */
  @PostMapping("/{id}/publish")
  public ResponseEntity<Void> publish(@PathVariable String id) {
    examPlanService.publish(id);
    return ResponseEntity.ok().build();
  }

  /** 取消考试 */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<Void> cancel(@PathVariable String id) {
    examPlanService.cancel(id);
    return ResponseEntity.ok().build();
  }

  /** 获取报名名单 */
  @GetMapping("/{id}/enrollments")
  public ResponseEntity<List<ExamEnrollmentEntity>> getEnrollments(@PathVariable String id) {
    return ResponseEntity.ok(examPlanService.getEnrollments(id));
  }

  /** 按班级批量报名 */
  @PostMapping("/{id}/enroll-classes")
  public ResponseEntity<Map<String, Integer>> enrollByClasses(
      @PathVariable String id, @RequestBody Map<String, List<String>> request) {
    List<String> classIds = request.get("classIds");
    int total = 0;
    for (String classId : classIds) {
      total += examPlanService.enrollByClass(id, classId);
    }
    return ResponseEntity.ok(Map.of("enrolled", total));
  }

  /** 标记缺考 */
  @PostMapping("/{id}/enrollments/{enrollmentId}/absent")
  public ResponseEntity<Void> markAbsent(
      @PathVariable String id, @PathVariable String enrollmentId) {
    examPlanService.markAbsent(enrollmentId);
    return ResponseEntity.ok().build();
  }

  /** 获取当前进行中的考试 */
  @GetMapping("/active")
  public ResponseEntity<List<ExamPlanEntity>> getActiveExams() {
    return ResponseEntity.ok(examPlanService.getActiveExams());
  }

  /** 获取即将开始的考试 */
  @GetMapping("/upcoming")
  public ResponseEntity<List<ExamPlanEntity>> getUpcomingExams() {
    return ResponseEntity.ok(examPlanService.getUpcomingExams());
  }
}

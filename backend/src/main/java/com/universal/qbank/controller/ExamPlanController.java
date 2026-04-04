package com.universal.qbank.controller;

import com.universal.qbank.entity.ExamEnrollmentEntity;
import com.universal.qbank.entity.ExamPlanEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.UserRepository;
import com.universal.qbank.service.ExamPlanService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

  @Autowired private UserRepository userRepository;

  /** 报名信息DTO，包含学生详细信息 */
  public static class EnrollmentDTO {
    public String id;
    public String studentId;
    public String studentName;
    public String avatarUrl;
    public String status;
    public String statusLabel;

    public EnrollmentDTO(ExamEnrollmentEntity enrollment, UserEntity user) {
      this.id = enrollment.getId();
      this.studentId = enrollment.getStudentId();
      this.status = enrollment.getStatus();
      this.statusLabel = getStatusLabel(enrollment.getStatus());
      if (user != null) {
        this.studentName = user.getNickname() != null ? user.getNickname() : user.getUsername();
        this.avatarUrl = user.getAvatarUrl();
      } else {
        this.studentName = enrollment.getStudentId();
      }
    }

    private String getStatusLabel(String status) {
      if (status == null) return "未知";
      switch (status) {
        case "ENROLLED":
          return "已报名";
        case "EXEMPTED":
          return "免考";
        case "ABSENT":
          return "缺考";
        case "COMPLETED":
          return "已完成";
        default:
          return status;
      }
    }
  }

  /** 获取考试计划列表 */
  @GetMapping
  public ResponseEntity<Page<ExamPlanEntity>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(examPlanService.getExamPlans(PageRequest.of(page, size)));
  }

  /** 获取单个考试计划 */
  @GetMapping("/{id}")
  public ResponseEntity<?> getById(@PathVariable String id) {
    Optional<ExamPlanEntity> planOpt = examPlanService.getById(id);
    if (planOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    ExamPlanEntity plan = planOpt.get();
    List<String> classIds = examPlanService.getExamPlanClassIds(id);
    return ResponseEntity.ok(
        Map.of(
            "plan", plan,
            "classIds", classIds));
  }

  /** 创建考试计划 */
  @PostMapping
  public ResponseEntity<ExamPlanEntity> create(@RequestBody Map<String, Object> request) {
    ExamPlanEntity plan = new ExamPlanEntity();
    plan.setName((String) request.get("name"));
    if (request.get("paperId") != null) {
      plan.setPaperId(Long.valueOf(request.get("paperId").toString()));
    }
    plan.setExamType((String) request.getOrDefault("examType", "FORMAL"));
    plan.setDurationMins((Integer) request.getOrDefault("durationMins", 120));
    if (request.get("maxAttempts") != null) {
      plan.setMaxAttempts(Integer.valueOf(request.get("maxAttempts").toString()));
    }
    if (request.get("passScore") != null) {
      plan.setPassScore(new java.math.BigDecimal(request.get("passScore").toString()));
    }
    if (request.get("startTime") != null) {
      plan.setStartTime(java.time.OffsetDateTime.parse(request.get("startTime").toString()));
    }
    if (request.get("endTime") != null) {
      plan.setEndTime(java.time.OffsetDateTime.parse(request.get("endTime").toString()));
    }

    @SuppressWarnings("unchecked")
    List<String> classIds = (List<String>) request.get("classIds");

    ExamPlanEntity created = examPlanService.create(plan, classIds);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /** 更新考试计划 */
  @PutMapping("/{id}")
  public ResponseEntity<ExamPlanEntity> update(
      @PathVariable String id, @RequestBody Map<String, Object> request) {
    ExamPlanEntity plan = new ExamPlanEntity();
    plan.setName((String) request.get("name"));
    if (request.get("paperId") != null) {
      plan.setPaperId(Long.valueOf(request.get("paperId").toString()));
    }
    plan.setExamType((String) request.getOrDefault("examType", "FORMAL"));
    plan.setDurationMins((Integer) request.getOrDefault("durationMins", 120));
    if (request.get("maxAttempts") != null) {
      plan.setMaxAttempts(Integer.valueOf(request.get("maxAttempts").toString()));
    }
    if (request.get("passScore") != null) {
      plan.setPassScore(new java.math.BigDecimal(request.get("passScore").toString()));
    }
    if (request.get("startTime") != null) {
      plan.setStartTime(java.time.OffsetDateTime.parse(request.get("startTime").toString()));
    }
    if (request.get("endTime") != null) {
      plan.setEndTime(java.time.OffsetDateTime.parse(request.get("endTime").toString()));
    }

    ExamPlanEntity updated = examPlanService.update(id, plan);

    // 更新班级关联
    @SuppressWarnings("unchecked")
    List<String> classIds = (List<String>) request.get("classIds");
    if (classIds != null) {
      examPlanService.saveExamPlanClasses(id, classIds);
    }

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
  public ResponseEntity<List<EnrollmentDTO>> getEnrollments(@PathVariable String id) {
    List<ExamEnrollmentEntity> enrollments = examPlanService.getEnrollments(id);
    List<EnrollmentDTO> result =
        enrollments.stream()
            .map(
                enrollment -> {
                  UserEntity user = userRepository.findById(enrollment.getStudentId()).orElse(null);
                  return new EnrollmentDTO(enrollment, user);
                })
            .collect(Collectors.toList());
    return ResponseEntity.ok(result);
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

  /** 获取当前学生已报名的考试列表 */
  @GetMapping("/my-exams")
  public ResponseEntity<?> getMyExams(@RequestHeader("Authorization") String token) {
    String studentId = getUserIdFromToken(token);
    if (studentId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }

    List<ExamEnrollmentEntity> enrollments = examPlanService.getStudentExams(studentId);

    // 获取每个报名对应的考试计划详情
    List<Map<String, Object>> result =
        enrollments.stream()
            .map(
                enrollment -> {
                  Optional<ExamPlanEntity> planOpt =
                      examPlanService.getById(enrollment.getExamPlanId());
                  if (planOpt.isPresent()) {
                    ExamPlanEntity plan = planOpt.get();
                    return Map.<String, Object>of(
                        "enrollmentId", enrollment.getId(),
                        "enrollmentStatus", enrollment.getStatus(),
                        "examPlan", plan);
                  }
                  return null;
                })
            .filter(item -> item != null)
            .collect(Collectors.toList());

    return ResponseEntity.ok(result);
  }

  private String getUserIdFromToken(String token) {
    if (token == null || !token.startsWith("Bearer ")) {
      return null;
    }
    String tokenValue = token.substring(7);
    // Parse dummy JWT token format: "dummy-jwt-token-{userId}"
    if (tokenValue.startsWith("dummy-jwt-token-")) {
      return tokenValue.substring("dummy-jwt-token-".length());
    }
    return null;
  }
}

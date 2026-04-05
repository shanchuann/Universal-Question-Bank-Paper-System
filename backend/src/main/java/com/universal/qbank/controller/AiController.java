package com.universal.qbank.controller;

import com.universal.qbank.entity.AiAuditLogEntity;
import com.universal.qbank.entity.ExamEntity;
import com.universal.qbank.entity.ExamRecordEntity;
import com.universal.qbank.entity.PaperEntity;
import com.universal.qbank.entity.PaperItemEntity;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.ExamRepository;
import com.universal.qbank.repository.PaperRepository;
import com.universal.qbank.repository.QuestionRepository;
import com.universal.qbank.repository.UserRepository;
import com.universal.qbank.service.AiAuditLogService;
import com.universal.qbank.service.OllamaAiService;
import com.universal.qbank.service.StudentLearningContextService;
import com.universal.qbank.service.SystemConfigService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

  @Autowired private OllamaAiService ollamaAiService;

  @Autowired private UserRepository userRepository;

  @Autowired private QuestionRepository questionRepository;

  @Autowired private ExamRepository examRepository;

  @Autowired private PaperRepository paperRepository;

  @Autowired private StudentLearningContextService studentLearningContextService;

  @Autowired private AiAuditLogService aiAuditLogService;

  @Autowired private SystemConfigService systemConfigService;

  public static class AskRequest {
    public String question;
    public String context;
  }

  public static class GradeSuggestionRequest {
    public String questionId;
    public String studentAnswer;
    public Double maxScore;
    public String rubric;
    public String contextRef;
  }

  public static class BatchGradeSuggestionRequest {
    public Long examId;
    public String rubric;
    public Boolean includeAlreadyGraded;
    public Integer maxQuestions;
  }

  public static class AuditAcceptRequest {
    public List<String> auditIds;
  }

  private ResponseEntity<Map<String, Object>> unauthorized() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
  }

  private boolean isAiEnabledByAdmin() {
    return systemConfigService.getBooleanConfig(SystemConfigService.AI_ENABLED, false);
  }

  private ResponseEntity<Map<String, Object>> aiDisabled() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "AI 功能已被管理员关闭"));
  }

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return null;
  }

  private boolean hasRole(UserEntity user, String... roles) {
    if (user == null || user.getRole() == null) {
      return false;
    }
    for (String role : roles) {
      if (role.equalsIgnoreCase(user.getRole())) {
        return true;
      }
    }
    return false;
  }

  private String joinContext(String baseContext, String autoContext) {
    String base = baseContext == null ? "" : baseContext.trim();
    String auto = autoContext == null ? "" : autoContext.trim();
    if (base.isBlank()) {
      return auto;
    }
    if (auto.isBlank()) {
      return base;
    }
    return base + "\n\n" + auto;
  }

  private boolean isObjectiveQuestion(String type) {
    if (type == null) {
      return false;
    }
    String t = type.toUpperCase();
    return "SINGLE_CHOICE".equals(t) || "MULTI_CHOICE".equals(t) || "TRUE_FALSE".equals(t);
  }

  @GetMapping("/status")
  public ResponseEntity<?> status(@RequestHeader("Authorization") String token) {
    String userId = getUserIdFromToken(token);
    if (userId == null || userRepository.findById(userId).isEmpty()) {
      return unauthorized();
    }
    Map<String, Object> status = new LinkedHashMap<>(ollamaAiService.status());
    status.put("enabledByAdmin", isAiEnabledByAdmin());
    status.put("enabled", isAiEnabledByAdmin() && Boolean.TRUE.equals(status.get("enabled")));
    return ResponseEntity.ok(status);
  }

  @PostMapping("/teacher/ask")
  public ResponseEntity<?> askTeacher(
      @RequestHeader("Authorization") String token, @RequestBody AskRequest request) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }

    UserEntity user = userRepository.findById(userId).orElse(null);
    if (!hasRole(user, "TEACHER", "ADMIN")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "仅教师可使用该接口"));
    }
    if (!isAiEnabledByAdmin()) {
      return aiDisabled();
    }

    try {
      long start = System.currentTimeMillis();
      String answer = ollamaAiService.askTeacher(request.question, request.context);
      String auditId =
          aiAuditLogService.log(
              "TEACHER_ASK",
              user.getId(),
              user.getRole(),
              request.question,
              answer,
              request.context,
              null,
              String.valueOf(ollamaAiService.status().get("model")),
              true,
              null,
              System.currentTimeMillis() - start);
      return ResponseEntity.ok(Map.of("answer", answer, "auditId", auditId));
    } catch (Exception ex) {
      aiAuditLogService.log(
          "TEACHER_ASK",
          user.getId(),
          user.getRole(),
          request.question,
          null,
          request.context,
          null,
          String.valueOf(ollamaAiService.status().get("model")),
          false,
          ex.getMessage(),
          0L);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/student/ask")
  public ResponseEntity<?> askStudent(
      @RequestHeader("Authorization") String token, @RequestBody AskRequest request) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }

    UserEntity user = userRepository.findById(userId).orElse(null);
    if (!hasRole(user, "USER", "STUDENT", "TEACHER", "ADMIN")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限访问该接口"));
    }
    if (!isAiEnabledByAdmin()) {
      return aiDisabled();
    }

    try {
      Map<String, Object> weakContext = studentLearningContextService.buildWeakPointContext(userId);
      String autoContext = String.valueOf(weakContext.getOrDefault("autoContext", ""));
      String mergedContext = joinContext(request.context, autoContext);

      long start = System.currentTimeMillis();
      String answer = ollamaAiService.askStudent(request.question, mergedContext);
      String auditId =
          aiAuditLogService.log(
              "STUDENT_ASK",
              user.getId(),
              user.getRole(),
              request.question,
              answer,
              mergedContext,
              null,
              String.valueOf(ollamaAiService.status().get("model")),
              true,
              null,
              System.currentTimeMillis() - start);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("answer", answer);
      result.put("auditId", auditId);
      result.put("autoContext", autoContext);
      result.put("weakPoints", weakContext.get("weakPoints"));
      result.put("recentWrongQuestions", weakContext.get("recentWrongQuestions"));
      return ResponseEntity.ok(result);
    } catch (Exception ex) {
      aiAuditLogService.log(
          "STUDENT_ASK",
          user.getId(),
          user.getRole(),
          request.question,
          null,
          request.context,
          null,
          String.valueOf(ollamaAiService.status().get("model")),
          false,
          ex.getMessage(),
          0L);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/teacher/subjective-grade")
  public ResponseEntity<?> suggestSubjectiveGrade(
      @RequestHeader("Authorization") String token, @RequestBody GradeSuggestionRequest request) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }

    UserEntity user = userRepository.findById(userId).orElse(null);
    if (!hasRole(user, "TEACHER", "ADMIN")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "仅教师可使用该接口"));
    }
    if (!isAiEnabledByAdmin()) {
      return aiDisabled();
    }

    if (request.questionId == null || request.questionId.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "questionId 不能为空"));
    }

    QuestionEntity question = questionRepository.findById(request.questionId).orElse(null);
    if (question == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "题目不存在"));
    }

    try {
      long start = System.currentTimeMillis();
      Map<String, Object> suggestion =
          ollamaAiService.suggestSubjectiveScore(
              question, request.studentAnswer, request.maxScore, request.rubric);

      String auditId =
          aiAuditLogService.log(
              "TEACHER_SUBJECTIVE_SINGLE",
              user.getId(),
              user.getRole(),
              question.getStem(),
              String.valueOf(suggestion),
              request.rubric,
              request.contextRef,
              String.valueOf(ollamaAiService.status().get("model")),
              true,
              null,
              System.currentTimeMillis() - start);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("questionId", request.questionId);
      result.put("maxScore", request.maxScore == null ? 1.0 : request.maxScore);
      result.putAll(suggestion);
      result.put("auditId", auditId);
      return ResponseEntity.ok(result);
    } catch (Exception ex) {
      aiAuditLogService.log(
          "TEACHER_SUBJECTIVE_SINGLE",
          user.getId(),
          user.getRole(),
          request.questionId,
          null,
          request.rubric,
          request.contextRef,
          String.valueOf(ollamaAiService.status().get("model")),
          false,
          ex.getMessage(),
          0L);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/teacher/batch-subjective-grade")
  public ResponseEntity<?> batchSuggestSubjectiveGrades(
      @RequestHeader("Authorization") String token,
      @RequestBody BatchGradeSuggestionRequest request) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }

    UserEntity user = userRepository.findById(userId).orElse(null);
    if (!hasRole(user, "TEACHER", "ADMIN")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "仅教师可使用该接口"));
    }
    if (!isAiEnabledByAdmin()) {
      return aiDisabled();
    }

    if (request.examId == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "examId 不能为空"));
    }

    ExamEntity exam = examRepository.findById(request.examId).orElse(null);
    if (exam == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "考试不存在"));
    }

    PaperEntity paper = paperRepository.findById(exam.getPaperId()).orElse(null);
    if (paper == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "试卷不存在"));
    }

    Map<String, Double> maxScoreMap = new HashMap<>();
    if (paper.getItems() != null && !paper.getItems().isEmpty()) {
      for (PaperItemEntity item : paper.getItems()) {
        if ("QUESTION".equals(item.getItemType()) && item.getQuestionId() != null) {
          maxScoreMap.put(item.getQuestionId(), item.getScore() == null ? 1.0 : item.getScore());
        }
      }
    } else if (paper.getQuestionIds() != null) {
      for (String qId : paper.getQuestionIds()) {
        maxScoreMap.put(qId, 1.0);
      }
    }

    List<String> questionIds =
        exam.getRecords() == null
            ? List.of()
            : exam.getRecords().stream()
                .map(ExamRecordEntity::getQuestionId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    Map<String, QuestionEntity> questionMap =
        questionRepository.findAllById(questionIds).stream()
            .collect(java.util.stream.Collectors.toMap(QuestionEntity::getId, q -> q));

    boolean includeAlready = Boolean.TRUE.equals(request.includeAlreadyGraded);
    int limit =
        request.maxQuestions == null ? 60 : Math.max(1, Math.min(request.maxQuestions, 120));
    List<Map<String, Object>> suggestions = new ArrayList<>();

    if (exam.getRecords() != null) {
      for (ExamRecordEntity record : exam.getRecords()) {
        if (suggestions.size() >= limit) {
          break;
        }
        if (record.getQuestionId() == null) {
          continue;
        }
        QuestionEntity question = questionMap.get(record.getQuestionId());
        if (question == null || isObjectiveQuestion(question.getType())) {
          continue;
        }
        if (!includeAlready && record.getScore() != null) {
          continue;
        }

        Double maxScore = maxScoreMap.getOrDefault(record.getQuestionId(), 1.0);
        String contextRef = "exam:" + request.examId + "#q:" + record.getQuestionId();
        try {
          long start = System.currentTimeMillis();
          Map<String, Object> suggestion =
              ollamaAiService.suggestSubjectiveScore(
                  question, record.getUserAnswer(), maxScore, request.rubric);

          String auditId =
              aiAuditLogService.log(
                  "TEACHER_SUBJECTIVE_BATCH",
                  user.getId(),
                  user.getRole(),
                  question.getStem(),
                  String.valueOf(suggestion),
                  request.rubric,
                  contextRef,
                  String.valueOf(ollamaAiService.status().get("model")),
                  true,
                  null,
                  System.currentTimeMillis() - start);

          Map<String, Object> row = new LinkedHashMap<>();
          row.put("questionId", record.getQuestionId());
          row.put("studentAnswer", record.getUserAnswer());
          row.put("maxScore", maxScore);
          row.put("score", suggestion.get("score"));
          row.put("reason", suggestion.get("reason"));
          row.put("feedback", suggestion.get("feedback"));
          row.put("auditId", auditId);
          suggestions.add(row);
        } catch (Exception ex) {
          aiAuditLogService.log(
              "TEACHER_SUBJECTIVE_BATCH",
              user.getId(),
              user.getRole(),
              question.getStem(),
              null,
              request.rubric,
              contextRef,
              String.valueOf(ollamaAiService.status().get("model")),
              false,
              ex.getMessage(),
              0L);
        }
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("examId", request.examId);
    result.put("count", suggestions.size());
    result.put("suggestions", suggestions);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/audit/accept")
  public ResponseEntity<?> markAuditAccepted(
      @RequestHeader("Authorization") String token, @RequestBody AuditAcceptRequest request) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }
    int changed = aiAuditLogService.markAccepted(request.auditIds, userId);
    return ResponseEntity.ok(Map.of("changed", changed));
  }

  @GetMapping("/audit/recent")
  public ResponseEntity<?> recentAudit(@RequestHeader("Authorization") String token) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }
    List<AiAuditLogEntity> logs = aiAuditLogService.recentByUser(userId);
    return ResponseEntity.ok(logs);
  }

  @GetMapping("/audit/summary")
  public ResponseEntity<?> auditSummary(
      @RequestHeader("Authorization") String token,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int days) {
    String userId = getUserIdFromToken(token);
    if (userId == null) {
      return unauthorized();
    }
    UserEntity user = userRepository.findById(userId).orElse(null);
    if (!hasRole(user, "TEACHER", "ADMIN")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "仅教师可查看审计摘要"));
    }
    return ResponseEntity.ok(aiAuditLogService.summary(days));
  }
}

package com.universal.qbank.service;

import com.universal.qbank.api.generated.model.ManualGradeRequest;
import com.universal.qbank.api.generated.model.ManualGradeRequestGradesInner;
import com.universal.qbank.entity.*;
import com.universal.qbank.repository.ExamEnrollmentRepository;
import com.universal.qbank.repository.ExamPlanRepository;
import com.universal.qbank.repository.ExamRepository;
import com.universal.qbank.repository.PaperRepository;
import com.universal.qbank.repository.QuestionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ExamService {

  @Autowired private ExamRepository examRepository;

  @Autowired private PaperRepository paperRepository;

  @Autowired private ExamPlanRepository examPlanRepository;

  @Autowired private ExamEnrollmentRepository examEnrollmentRepository;

  @Autowired private QuestionRepository questionRepository;

  @Autowired private com.universal.qbank.repository.UserRepository userRepository;

  @Autowired private com.universal.qbank.repository.StudentStatsRepository studentStatsRepository;

  @Autowired private EmailService emailService;

  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new com.fasterxml.jackson.databind.ObjectMapper();

  public ExamEntity startExam(Long paperId, String userId, String type, String planId) {
    PaperEntity paper =
        paperRepository
            .findById(paperId)
            .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

    if (planId != null && !planId.isBlank()) {
      ExamPlanEntity plan =
          examPlanRepository
              .findById(planId)
              .orElseThrow(() -> new IllegalArgumentException("Exam plan not found"));

      if (plan.getPaperId() != null && !plan.getPaperId().equals(paperId)) {
        throw new IllegalStateException("Paper does not match exam plan");
      }

      String planStatus = plan.getStatus();
      if (!("PUBLISHED".equals(planStatus) || "ONGOING".equals(planStatus))) {
        throw new IllegalStateException("Exam plan is not open for entry");
      }

      int maxAttempts =
          plan.getMaxAttempts() == null || plan.getMaxAttempts() < 1 ? 1 : plan.getMaxAttempts();
      ExamEnrollmentEntity enrollment =
          examEnrollmentRepository
              .findByExamPlanIdAndStudentId(planId, userId)
              .orElseThrow(
                  () -> new IllegalStateException("Student is not enrolled in this exam plan"));

      int used = enrollment.getAttemptsUsed() == null ? 0 : enrollment.getAttemptsUsed();
      if (used >= maxAttempts) {
        throw new IllegalStateException("Max attempts reached for this exam");
      }

      enrollment.setAttemptsUsed(used + 1);
      examEnrollmentRepository.save(enrollment);
    }

    ExamEntity exam = new ExamEntity();
    exam.setPaperId(paperId);
    exam.setUserId(userId);
    exam.setType(type != null ? type : "EXAM");
    exam.setStartTime(OffsetDateTime.now());
    exam.setRandomSeed(System.currentTimeMillis());

    return examRepository.save(exam);
  }

  // 判断是否为客观题（可自动评分）
  private boolean isObjectiveQuestion(String type) {
    if (type == null) return false;
    String t = type.toUpperCase();
    return t.equals("SINGLE_CHOICE")
        || t.equals("MULTIPLE_CHOICE")
        || t.equals("MULTI_CHOICE")
        || t.equals("TRUE_FALSE");
  }

  public ExamEntity submitExam(
      Long examId, Map<String, String> answers, List<String> flaggedQuestions) {
    ExamEntity exam =
        examRepository
            .findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

    if (exam.getEndTime() != null) {
      throw new IllegalStateException("Exam already submitted");
    }

    PaperEntity paper =
        paperRepository
            .findById(exam.getPaperId())
            .orElseThrow(() -> new IllegalStateException("Paper not found"));

    List<QuestionEntity> questions = questionRepository.findAllById(paper.getQuestionIds());
    Map<String, QuestionEntity> questionMap =
        questions.stream().collect(Collectors.toMap(QuestionEntity::getId, q -> q));

    List<ExamRecordEntity> records = new ArrayList<>();
    int objectiveCorrectCount = 0;
    int objectiveTotal = 0;
    boolean hasSubjectiveQuestions = false;

    for (Map.Entry<String, String> entry : answers.entrySet()) {
      String qId = entry.getKey();
      String userAnswer = entry.getValue();

      QuestionEntity q = questionMap.get(qId);
      if (q == null) continue;

      ExamRecordEntity record = new ExamRecordEntity();
      record.setQuestionId(qId);
      record.setUserAnswer(userAnswer);
      if (flaggedQuestions != null && flaggedQuestions.contains(qId)) {
        record.setIsFlagged(true);
      }

      // 主观题不自动评分，留给老师阅卷
      if (!isObjectiveQuestion(q.getType())) {
        hasSubjectiveQuestions = true;
        record.setIsCorrect(null); // 待评分
        record.setScore(null);
        records.add(record);
        continue;
      }

      objectiveTotal++;
      boolean isCorrect = false;
      if (q.getOptionsJson() != null) {
        try {
          List<com.universal.qbank.api.generated.model.QuestionOption> opts =
              objectMapper.readValue(
                  q.getOptionsJson(),
                  new com.fasterxml.jackson.core.type.TypeReference<
                      List<com.universal.qbank.api.generated.model.QuestionOption>>() {});

          if ("MULTIPLE_CHOICE".equalsIgnoreCase(q.getType())
              || "MULTI_CHOICE".equalsIgnoreCase(q.getType())) {
            // Set based comparison
            Set<String> correctSet =
                opts.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(o -> o.getText().trim())
                    .collect(Collectors.toSet());

            if (userAnswer != null) {
              Set<String> userSet =
                  Arrays.stream(userAnswer.split(","))
                      .map(String::trim)
                      .collect(Collectors.toSet());
              isCorrect = userSet.equals(correctSet);
            }
          } else {
            // Standard string comparison (SINGLE_CHOICE, TRUE_FALSE)
            String correctAnswer =
                opts.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .map(com.universal.qbank.api.generated.model.QuestionOption::getText)
                    .collect(Collectors.joining(","));

            if (userAnswer != null) {
              String normalizedUser = userAnswer.trim();
              String normalizedCorrect = correctAnswer.trim();
              isCorrect = normalizedUser.equalsIgnoreCase(normalizedCorrect);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      record.setIsCorrect(isCorrect);
      if (isCorrect) objectiveCorrectCount++;

      records.add(record);
    }

    exam.setRecords(records);
    exam.setEndTime(OffsetDateTime.now());

    // 如果有主观题，设置为待阅卷状态，暂不计算总分
    if (hasSubjectiveQuestions) {
      exam.setGradingStatus("PENDING");
      exam.setScore(null); // 总分待阅卷后确定
    } else {
      // 纯客观题试卷，直接计算分数
      int score = objectiveTotal == 0 ? 0 : (objectiveCorrectCount * 100 / objectiveTotal);
      exam.setScore(score);
      exam.setGradingStatus("GRADED");
    }

    ExamEntity savedExam = examRepository.save(exam);

    // Update Student Stats (只统计客观题，主观题待阅卷后再统计)
    if (exam.getUserId() != null && objectiveTotal > 0) {
      StudentStatsEntity stats =
          studentStatsRepository.findByUserId(exam.getUserId()).orElse(new StudentStatsEntity());
      stats.setUserId(exam.getUserId());

      // Try to fetch nickname
      userRepository
          .findById(exam.getUserId())
          .ifPresent(
              u -> {
                stats.setNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
              });

      stats.setTotalQuestionsAnswered(stats.getTotalQuestionsAnswered() + objectiveTotal);
      stats.setCorrectAnswers(stats.getCorrectAnswers() + objectiveCorrectCount);

      java.time.LocalDate today = java.time.LocalDate.now();
      java.time.LocalDate lastDate = stats.getLastPracticeDate();

      if (lastDate == null) {
        stats.setCurrentStreak(1);
      } else if (!lastDate.isEqual(today)) {
        if (lastDate.plusDays(1).isEqual(today)) {
          stats.setCurrentStreak(stats.getCurrentStreak() + 1);
        } else {
          stats.setCurrentStreak(1);
        }
      }
      stats.setLastPracticeDate(today);
      studentStatsRepository.save(stats);
    }

    return savedExam;
  }

  public org.springframework.data.domain.Page<ExamEntity> listExams(
      Long paperId, String userId, int page, int size) {
    org.springframework.data.domain.Pageable pageable =
        org.springframework.data.domain.PageRequest.of(
            page, size, org.springframework.data.domain.Sort.by("startTime").descending());

    // 获取所有学生用户ID（只显示学生的答卷）
    List<String> studentUserIds =
        userRepository.findAll().stream()
            .filter(u -> "USER".equals(u.getRole()) || "STUDENT".equals(u.getRole()))
            .map(u -> u.getId())
            .collect(Collectors.toList());

    Specification<ExamEntity> spec =
        (root, query, cb) -> {
          List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
          if (paperId != null) {
            predicates.add(cb.equal(root.get("paperId"), paperId));
          }
          if (userId != null && !userId.isEmpty()) {
            predicates.add(cb.equal(root.get("userId"), userId));
          }
          // 只获取学生的答卷
          if (!studentUserIds.isEmpty()) {
            predicates.add(root.get("userId").in(studentUserIds));
          }
          return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

    return examRepository.findAll(spec, pageable);
  }

  public ExamEntity gradeExam(Long examId, ManualGradeRequest request) {
    ExamEntity exam =
        examRepository
            .findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

    PaperEntity paper = paperRepository.findById(exam.getPaperId()).orElseThrow();

    // Ensure records list is initialized
    if (exam.getRecords() == null) {
      exam.setRecords(new java.util.ArrayList<>());
    }

    // Build recordMap, creating records if they don't exist
    Map<String, ExamRecordEntity> recordMap =
        exam.getRecords().stream()
            .collect(Collectors.toMap(ExamRecordEntity::getQuestionId, r -> r));

    // Ensure all questions from paper have records
    if (paper.getItems() != null && !paper.getItems().isEmpty()) {
      for (PaperItemEntity item : paper.getItems()) {
        if ("QUESTION".equals(item.getItemType()) && !recordMap.containsKey(item.getQuestionId())) {
          ExamRecordEntity record = new ExamRecordEntity();
          record.setQuestionId(item.getQuestionId());
          exam.getRecords().add(record);
          recordMap.put(item.getQuestionId(), record);
        }
      }
    } else if (paper.getQuestionIds() != null) {
      for (String qId : paper.getQuestionIds()) {
        if (!recordMap.containsKey(qId)) {
          ExamRecordEntity record = new ExamRecordEntity();
          record.setQuestionId(qId);
          exam.getRecords().add(record);
          recordMap.put(qId, record);
        }
      }
    }

    if (request.getGrades() != null) {
      for (ManualGradeRequestGradesInner grade : request.getGrades()) {
        ExamRecordEntity record = recordMap.get(grade.getQuestionId());
        if (record != null) {
          record.setScore(grade.getScore() != null ? grade.getScore().doubleValue() : null);
          record.setNotes(grade.getNotes());
        }
      }
    }

    double totalMaxScore = 0;
    double totalUserScore = 0;

    Map<String, Double> maxScoreMap = new java.util.HashMap<>();
    if (paper.getItems() != null && !paper.getItems().isEmpty()) {
      for (PaperItemEntity item : paper.getItems()) {
        if ("QUESTION".equals(item.getItemType())) {
          maxScoreMap.put(item.getQuestionId(), item.getScore());
          totalMaxScore += item.getScore();
        }
      }
    } else {
      for (String qId : paper.getQuestionIds()) {
        maxScoreMap.put(qId, 1.0);
        totalMaxScore += 1.0;
      }
    }

    for (ExamRecordEntity record : exam.getRecords()) {
      if (record.getScore() != null) {
        totalUserScore += record.getScore();
      } else if (Boolean.TRUE.equals(record.getIsCorrect())) {
        Double max = maxScoreMap.getOrDefault(record.getQuestionId(), 1.0);
        totalUserScore += max;
        record.setScore(max);
      }
    }

    int percentage = totalMaxScore == 0 ? 0 : (int) ((totalUserScore / totalMaxScore) * 100);
    exam.setScore(percentage);
    exam.setGradingStatus("GRADED");

    ExamEntity savedExam = examRepository.save(exam);

    // 发送成绩通知邮件
    sendScoreNotificationEmail(savedExam, paper);

    return savedExam;
  }

  /** 发送成绩通知邮件给学生 */
  private void sendScoreNotificationEmail(ExamEntity exam, PaperEntity paper) {
    if (exam.getScoreNotified() != null && exam.getScoreNotified()) {
      return; // 已经发送过通知
    }

    if (exam.getUserId() == null) {
      return;
    }

    userRepository
        .findById(exam.getUserId())
        .ifPresent(
            user -> {
              if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String studentName =
                    user.getNickname() != null ? user.getNickname() : user.getUsername();
                String examTitle =
                    paper.getTitle() != null ? paper.getTitle() : "考试 #" + exam.getId();

                // 收集教师评语
                StringBuilder comments = new StringBuilder();
                if (exam.getRecords() != null) {
                  for (ExamRecordEntity record : exam.getRecords()) {
                    if (record.getNotes() != null && !record.getNotes().isEmpty()) {
                      comments.append("- ").append(record.getNotes()).append("\n");
                    }
                  }
                }

                emailService.sendScoreNotification(
                    user.getEmail(),
                    studentName,
                    examTitle,
                    exam.getScore() != null ? exam.getScore() : 0,
                    comments.toString());

                // 标记已发送
                exam.setScoreNotified(true);
                examRepository.save(exam);
              }
            });
  }

  public ExamEntity getExam(Long examId) {
    return examRepository
        .findById(examId)
        .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
  }

  /** 获取学生自己的考试记录 */
  public org.springframework.data.domain.Page<ExamEntity> getStudentExams(
      String userId, org.springframework.data.domain.Pageable pageable) {

    Specification<ExamEntity> spec =
        (root, query, cb) -> {
          List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("userId"), userId));
          // 只获取已提交的考试
          predicates.add(cb.isNotNull(root.get("endTime")));
          return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

    return examRepository.findAll(spec, pageable);
  }
}

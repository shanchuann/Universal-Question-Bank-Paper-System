package com.universal.qbank.service;

import com.universal.qbank.api.generated.model.ManualGradeRequest;
import com.universal.qbank.api.generated.model.ManualGradeRequestGradesInner;
import com.universal.qbank.entity.*;
import com.universal.qbank.repository.ExamRepository;
import com.universal.qbank.repository.PaperRepository;
import com.universal.qbank.repository.QuestionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ExamService {

  @Autowired private ExamRepository examRepository;

  @Autowired private PaperRepository paperRepository;

  @Autowired private QuestionRepository questionRepository;

  @Autowired private com.universal.qbank.repository.UserRepository userRepository;

  @Autowired private com.universal.qbank.repository.StudentStatsRepository studentStatsRepository;

  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new com.fasterxml.jackson.databind.ObjectMapper();

  public ExamEntity startExam(Long paperId, String userId, String type) {
    PaperEntity paper =
        paperRepository
            .findById(paperId)
            .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

    ExamEntity exam = new ExamEntity();
    exam.setPaperId(paperId);
    exam.setUserId(userId);
    exam.setType(type != null ? type : "EXAM");
    exam.setStartTime(OffsetDateTime.now());
    exam.setRandomSeed(System.currentTimeMillis());

    return examRepository.save(exam);
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
    int correctCount = 0;

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

      boolean isCorrect = false;
      if (q.getOptionsJson() != null) {
        try {
          List<com.universal.qbank.api.generated.model.QuestionOption> opts =
              objectMapper.readValue(
                  q.getOptionsJson(),
                  new com.fasterxml.jackson.core.type.TypeReference<
                      List<com.universal.qbank.api.generated.model.QuestionOption>>() {});
          String correctAnswer =
              opts.stream()
                  .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                  .map(com.universal.qbank.api.generated.model.QuestionOption::getText)
                  .collect(Collectors.joining(","));

          if (userAnswer != null) {
            isCorrect = userAnswer.equalsIgnoreCase(correctAnswer);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      record.setIsCorrect(isCorrect);
      if (isCorrect) correctCount++;

      records.add(record);
    }

    exam.setRecords(records);
    exam.setEndTime(OffsetDateTime.now());

    // Simple scoring: 100 * (correct / total)
    int total = questions.size();
    int score = total == 0 ? 0 : (correctCount * 100 / total);
    exam.setScore(score);

    ExamEntity savedExam = examRepository.save(exam);

    // Update Student Stats
    if (exam.getUserId() != null) {
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

      stats.setTotalQuestionsAnswered(stats.getTotalQuestionsAnswered() + total);
      stats.setCorrectAnswers(stats.getCorrectAnswers() + correctCount);

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

    Specification<ExamEntity> spec =
        (root, query, cb) -> {
          List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
          if (paperId != null) {
            predicates.add(cb.equal(root.get("paperId"), paperId));
          }
          if (userId != null && !userId.isEmpty()) {
            predicates.add(cb.equal(root.get("userId"), userId));
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

    Map<String, ExamRecordEntity> recordMap =
        exam.getRecords().stream()
            .collect(Collectors.toMap(ExamRecordEntity::getQuestionId, r -> r));

    if (request.getGrades() != null) {
      for (ManualGradeRequestGradesInner grade : request.getGrades()) {
        ExamRecordEntity record = recordMap.get(grade.getQuestionId());
        if (record != null) {
          record.setScore(grade.getScore() != null ? grade.getScore().doubleValue() : null);
          record.setNotes(grade.getNotes());
        }
      }
    }

    PaperEntity paper = paperRepository.findById(exam.getPaperId()).orElseThrow();

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

    return examRepository.save(exam);
  }

  public ExamEntity getExam(Long examId) {
    return examRepository
        .findById(examId)
        .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
  }
}

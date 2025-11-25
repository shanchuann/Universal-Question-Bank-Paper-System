package com.universal.qbank.service;

import com.universal.qbank.api.generated.model.*;
import com.universal.qbank.entity.ExamEntity;
import com.universal.qbank.entity.ExamRecordEntity;
import com.universal.qbank.entity.PaperEntity;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.ExamRepository;
import com.universal.qbank.repository.PaperRepository;
import com.universal.qbank.repository.QuestionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

  @Autowired private ExamRepository examRepository;
  @Autowired private PaperRepository paperRepository;
  @Autowired private QuestionRepository questionRepository;

  public ExamAnalyticsResponse getPaperAnalytics(Long paperId) {
    List<ExamEntity> exams =
        examRepository.findAll().stream()
            .filter(e -> e.getPaperId().equals(paperId) && e.getScore() != null)
            .collect(Collectors.toList());

    ExamAnalyticsResponse response = new ExamAnalyticsResponse();
    response.setPaperVersionId(String.valueOf(paperId));

    if (exams.isEmpty()) {
      response.setAverageScore(BigDecimal.ZERO);
      response.setHighestScore(BigDecimal.ZERO);
      response.setPassRate(BigDecimal.ZERO);
      return response;
    }

    // Basic Stats
    double totalScore = exams.stream().mapToInt(ExamEntity::getScore).sum();
    double maxScore = exams.stream().mapToInt(ExamEntity::getScore).max().orElse(0);
    long passCount = exams.stream().filter(e -> e.getScore() >= 60).count();

    response.setAverageScore(
        BigDecimal.valueOf(totalScore / exams.size()).setScale(2, RoundingMode.HALF_UP));
    response.setHighestScore(BigDecimal.valueOf(maxScore));
    response.setPassRate(
        BigDecimal.valueOf((double) passCount / exams.size()).setScale(2, RoundingMode.HALF_UP));

    // Score Distribution
    Map<String, Integer> distribution = new HashMap<>();
    String[] ranges = {"0-59", "60-69", "70-79", "80-89", "90-100"};
    for (String range : ranges) distribution.put(range, 0);

    for (ExamEntity exam : exams) {
      int score = exam.getScore();
      if (score < 60) distribution.put("0-59", distribution.get("0-59") + 1);
      else if (score < 70) distribution.put("60-69", distribution.get("60-69") + 1);
      else if (score < 80) distribution.put("70-79", distribution.get("70-79") + 1);
      else if (score < 90) distribution.put("80-89", distribution.get("80-89") + 1);
      else distribution.put("90-100", distribution.get("90-100") + 1);
    }

    List<ScoreBucket> buckets = new ArrayList<>();
    for (String range : ranges) {
      ScoreBucket bucket = new ScoreBucket();
      bucket.setRange(range);
      int count = distribution.get(range);
      // Calculate percentage
      BigDecimal percentage =
          BigDecimal.valueOf((double) count / exams.size() * 100).setScale(2, RoundingMode.HALF_UP);
      bucket.setPercentage(percentage);
      buckets.add(bucket);
    }
    response.setScoreDistribution(buckets);

    // Knowledge Mastery & Error Rates
    // Need to load questions
    PaperEntity paper = paperRepository.findById(paperId).orElseThrow();
    List<QuestionEntity> questions = questionRepository.findAllById(paper.getQuestionIds());
    Map<String, QuestionEntity> questionMap =
        questions.stream().collect(Collectors.toMap(QuestionEntity::getId, q -> q));

    // Map<KnowledgePoint, List<ScoreRatio>>
    Map<String, List<Double>> kpScores = new HashMap<>();
    // Map<QuestionId, CorrectCount>
    Map<String, Integer> questionCorrectCounts = new HashMap<>();
    Map<String, Integer> questionAttemptCounts = new HashMap<>();

    for (QuestionEntity q : questions) {
      questionCorrectCounts.put(q.getId(), 0);
      questionAttemptCounts.put(q.getId(), 0);
    }

    for (ExamEntity exam : exams) {
      if (exam.getRecords() == null) continue;
      for (ExamRecordEntity record : exam.getRecords()) {
        QuestionEntity q = questionMap.get(record.getQuestionId());
        if (q == null) continue;

        questionAttemptCounts.put(q.getId(), questionAttemptCounts.getOrDefault(q.getId(), 0) + 1);

        boolean isCorrect = Boolean.TRUE.equals(record.getIsCorrect());
        if (isCorrect) {
          questionCorrectCounts.put(
              q.getId(), questionCorrectCounts.getOrDefault(q.getId(), 0) + 1);
        }

        // For knowledge points
        double scoreRatio = isCorrect ? 1.0 : 0.0;

        if (q.getKnowledgePointIds() != null) {
          for (String kp : q.getKnowledgePointIds()) {
            kpScores.computeIfAbsent(kp, k -> new ArrayList<>()).add(scoreRatio);
          }
        }
      }
    }

    // Build Knowledge Metrics
    List<KnowledgeMetric> metrics = new ArrayList<>();
    for (Map.Entry<String, List<Double>> entry : kpScores.entrySet()) {
      KnowledgeMetric metric = new KnowledgeMetric();
      metric.setKnowledgePointId(entry.getKey());
      double avg = entry.getValue().stream().mapToDouble(d -> d).average().orElse(0.0);
      metric.setMasteryPercent(BigDecimal.valueOf(avg * 100).setScale(2, RoundingMode.HALF_UP));
      metrics.add(metric);
    }
    response.setKnowledgeMastery(metrics);

    // Build Error Rates
    List<QuestionErrorRate> errorRates = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : questionCorrectCounts.entrySet()) {
      String qId = entry.getKey();
      int correct = entry.getValue();
      int attempts = questionAttemptCounts.getOrDefault(qId, 0);

      if (attempts == 0) continue;

      QuestionErrorRate rate = new QuestionErrorRate();
      rate.setQuestionId(qId);
      rate.setAttempts(attempts);
      rate.setIncorrect(attempts - correct);

      // Error rate = 1 - (correct / attempts)
      double correctRate = (double) correct / attempts;
      rate.setErrorRate(
          BigDecimal.valueOf((1.0 - correctRate) * 100).setScale(2, RoundingMode.HALF_UP));

      errorRates.add(rate);
    }
    // Sort by error rate desc
    errorRates.sort((a, b) -> b.getErrorRate().compareTo(a.getErrorRate()));
    response.setErrorRates(errorRates);

    return response;
  }
}

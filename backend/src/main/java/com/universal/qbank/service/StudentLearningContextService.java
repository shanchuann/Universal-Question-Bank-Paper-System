package com.universal.qbank.service;

import com.universal.qbank.entity.ExamEntity;
import com.universal.qbank.entity.ExamRecordEntity;
import com.universal.qbank.entity.KnowledgePointEntity;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.ExamRepository;
import com.universal.qbank.repository.KnowledgePointRepository;
import com.universal.qbank.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentLearningContextService {

  @Autowired private ExamRepository examRepository;

  @Autowired private QuestionRepository questionRepository;

  @Autowired private KnowledgePointRepository knowledgePointRepository;

  public Map<String, Object> buildWeakPointContext(String userId) {
    List<ExamEntity> recentExams =
        examRepository.findTop30ByUserIdAndEndTimeIsNotNullOrderByEndTimeDesc(userId);

    Map<String, int[]> counters = new HashMap<>();
    Map<String, Integer> wrongQuestionCounter = new HashMap<>();
    List<String> touchedQuestionIds = new ArrayList<>();

    for (ExamEntity exam : recentExams) {
      if (exam.getRecords() == null) {
        continue;
      }
      for (ExamRecordEntity record : exam.getRecords()) {
        if (record.getQuestionId() == null) {
          continue;
        }
        touchedQuestionIds.add(record.getQuestionId());
      }
    }

    Map<String, QuestionEntity> questionMap =
        questionRepository.findAllById(touchedQuestionIds).stream()
            .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

    for (ExamEntity exam : recentExams) {
      if (exam.getRecords() == null) {
        continue;
      }

      for (ExamRecordEntity record : exam.getRecords()) {
        if (record.getQuestionId() == null || record.getIsCorrect() == null) {
          continue;
        }

        QuestionEntity question = questionMap.get(record.getQuestionId());
        if (question == null) {
          continue;
        }

        List<String> kpIds =
            question.getKnowledgePointIds() == null || question.getKnowledgePointIds().isEmpty()
                ? List.of("UNKNOWN")
                : question.getKnowledgePointIds();

        for (String kpId : kpIds) {
          int[] stat = counters.computeIfAbsent(kpId, k -> new int[] {0, 0});
          stat[1] += 1;
          if (Boolean.FALSE.equals(record.getIsCorrect())) {
            stat[0] += 1;
            wrongQuestionCounter.put(
                question.getId(), wrongQuestionCounter.getOrDefault(question.getId(), 0) + 1);
          }
        }
      }
    }

    List<String> kpIdList =
        counters.keySet().stream().filter(id -> !"UNKNOWN".equals(id)).collect(Collectors.toList());
    Map<String, String> kpNameMap =
        knowledgePointRepository.findAllById(kpIdList).stream()
            .collect(Collectors.toMap(KnowledgePointEntity::getId, KnowledgePointEntity::getName));

    List<Map<String, Object>> weakPoints =
        counters.entrySet().stream()
            .filter(e -> e.getValue()[1] > 0)
            .map(
                e -> {
                  String kpId = e.getKey();
                  int wrong = e.getValue()[0];
                  int total = e.getValue()[1];
                  double rate = total == 0 ? 0.0 : (wrong * 100.0 / total);

                  Map<String, Object> item = new LinkedHashMap<>();
                  item.put("knowledgePointId", kpId);
                  item.put("knowledgePointName", kpNameMap.getOrDefault(kpId, "未标注知识点"));
                  item.put("wrongCount", wrong);
                  item.put("attemptCount", total);
                  item.put("wrongRate", Math.round(rate * 100.0) / 100.0);
                  return item;
                })
            .sorted(
                Comparator.comparing((Map<String, Object> m) -> (Integer) m.get("wrongCount"))
                    .thenComparing(m -> (Double) m.get("wrongRate"))
                    .reversed())
            .limit(5)
            .collect(Collectors.toList());

    Set<String> topWrongQuestionIds =
        wrongQuestionCounter.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

    List<String> wrongQuestionStems =
        topWrongQuestionIds.stream()
            .map(questionMap::get)
            .filter(q -> q != null && q.getStem() != null)
            .map(q -> shortText(q.getStem(), 80))
            .collect(Collectors.toList());

    String autoContext = buildContextText(weakPoints, wrongQuestionStems);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("weakPoints", weakPoints);
    result.put("recentWrongQuestions", wrongQuestionStems);
    result.put("autoContext", autoContext);
    return result;
  }

  private String buildContextText(
      List<Map<String, Object>> weakPoints, List<String> wrongQuestionStems) {
    StringBuilder sb = new StringBuilder();
    sb.append("学生近期学习薄弱点：\n");

    if (weakPoints.isEmpty()) {
      sb.append("- 暂无明显薄弱点数据。\n");
    } else {
      for (Map<String, Object> item : weakPoints) {
        sb.append("- ")
            .append(item.get("knowledgePointName"))
            .append("（错误 ")
            .append(item.get("wrongCount"))
            .append("/")
            .append(item.get("attemptCount"))
            .append("，错误率 ")
            .append(item.get("wrongRate"))
            .append("%）\n");
      }
    }

    if (!wrongQuestionStems.isEmpty()) {
      sb.append("近期高频错题题干摘录：\n");
      for (String stem : wrongQuestionStems) {
        sb.append("- ").append(stem).append("\n");
      }
    }

    sb.append("请优先围绕这些薄弱点做解释和练习建议。\n");
    return sb.toString();
  }

  private String shortText(String text, int maxLen) {
    if (text == null) {
      return "";
    }
    String t = text.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    if (t.length() <= maxLen) {
      return t;
    }
    return t.substring(0, maxLen) + "...";
  }
}

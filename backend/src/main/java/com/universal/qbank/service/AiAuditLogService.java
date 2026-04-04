package com.universal.qbank.service;

import com.universal.qbank.entity.AiAuditLogEntity;
import com.universal.qbank.repository.AiAuditLogRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAuditLogService {

  @Autowired private AiAuditLogRepository aiAuditLogRepository;

  public String log(
      String feature,
      String userId,
      String userRole,
      String prompt,
      String response,
      String context,
      String contextRef,
      String modelName,
      boolean success,
      String errorMessage,
      long latencyMs) {
    AiAuditLogEntity entity = new AiAuditLogEntity();
    entity.setFeature(feature);
    entity.setUserId(userId);
    entity.setUserRole(userRole);
    entity.setPromptText(cut(prompt, 5000));
    entity.setResponseText(cut(response, 8000));
    entity.setContextText(cut(context, 3000));
    entity.setContextRef(cut(contextRef, 120));
    entity.setModelName(cut(modelName, 100));
    entity.setSuccess(success);
    entity.setErrorMessage(cut(errorMessage, 1000));
    entity.setLatencyMs(latencyMs);
    entity.setAccepted(false);
    return aiAuditLogRepository.save(entity).getId();
  }

  @Transactional
  public int markAccepted(List<String> ids, String operatorUserId) {
    if (ids == null || ids.isEmpty()) {
      return 0;
    }
    int changed = 0;
    for (String id : ids) {
      if (id == null || id.isBlank()) {
        continue;
      }
      AiAuditLogEntity log = aiAuditLogRepository.findById(id).orElse(null);
      if (log == null) {
        continue;
      }
      if (!operatorUserId.equals(log.getUserId())) {
        continue;
      }
      if (!Boolean.TRUE.equals(log.getAccepted())) {
        log.setAccepted(true);
        aiAuditLogRepository.save(log);
        changed++;
      }
    }
    return changed;
  }

  public List<AiAuditLogEntity> recentByUser(String userId) {
    return aiAuditLogRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId);
  }

  public Map<String, Object> summary(int days) {
    int safeDays = Math.max(1, Math.min(days, 180));
    OffsetDateTime since = OffsetDateTime.now().minusDays(safeDays);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("days", safeDays);
    result.put("since", since);

    result.put("teacherAsk", summarizeFeature("TEACHER_ASK", since));
    result.put("studentAsk", summarizeFeature("STUDENT_ASK", since));
    result.put("subjectiveSingle", summarizeFeature("TEACHER_SUBJECTIVE_SINGLE", since));
    result.put("subjectiveBatch", summarizeFeature("TEACHER_SUBJECTIVE_BATCH", since));
    result.put("autoSubjective", summarizeFeature("AUTO_SUBJECTIVE_GRADE", since));
    return result;
  }

  private Map<String, Object> summarizeFeature(String feature, OffsetDateTime since) {
    long total = aiAuditLogRepository.countByFeatureAndCreatedAtAfter(feature, since);
    long accepted =
        aiAuditLogRepository.countByFeatureAndAcceptedTrueAndCreatedAtAfter(feature, since);
    double rate = total == 0 ? 0.0 : (accepted * 100.0 / total);

    Map<String, Object> item = new LinkedHashMap<>();
    item.put("feature", feature);
    item.put("total", total);
    item.put("accepted", accepted);
    item.put("acceptRate", Math.round(rate * 100.0) / 100.0);
    return item;
  }

  private String cut(String value, int maxLen) {
    if (value == null) {
      return null;
    }
    if (value.length() <= maxLen) {
      return value;
    }
    return value.substring(0, maxLen);
  }
}

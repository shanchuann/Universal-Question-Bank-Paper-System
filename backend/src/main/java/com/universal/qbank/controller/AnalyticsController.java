package com.universal.qbank.controller;

import com.universal.qbank.api.generated.AnalyticsApi;
import com.universal.qbank.api.generated.model.ExamAnalyticsResponse;
import com.universal.qbank.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController implements AnalyticsApi {

  @Autowired private AnalyticsService analyticsService;

  @Override
  public ResponseEntity<ExamAnalyticsResponse> apiAnalyticsExamsPaperVersionIdSummaryGet(
      String paperVersionId) {
    try {
      Long paperId = Long.parseLong(paperVersionId);
      return ResponseEntity.ok(analyticsService.getPaperAnalytics(paperId));
    } catch (NumberFormatException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}

package com.universal.qbank.controller;

import com.universal.qbank.entity.StudentStatsEntity;
import com.universal.qbank.service.StudentStatsService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
public class StudentStatsController {

  @Autowired private StudentStatsService studentStatsService;

  @GetMapping("/me")
  public ResponseEntity<StudentStatsEntity> getMyStats(@RequestParam String userId) {
    return ResponseEntity.ok(studentStatsService.getStatsByUserId(userId));
  }

  @GetMapping("/leaderboard")
  public ResponseEntity<List<StudentStatsEntity>> getLeaderboard(
      @RequestParam(defaultValue = "5") int limit) {
    return ResponseEntity.ok(studentStatsService.getLeaderboard(limit));
  }
}

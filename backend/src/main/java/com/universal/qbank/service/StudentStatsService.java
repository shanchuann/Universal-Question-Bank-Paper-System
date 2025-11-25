package com.universal.qbank.service;

import com.universal.qbank.entity.StudentStatsEntity;
import com.universal.qbank.repository.StudentStatsRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class StudentStatsService {

  @Autowired private StudentStatsRepository studentStatsRepository;

  public StudentStatsEntity getStatsByUserId(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      return new StudentStatsEntity();
    }
    return studentStatsRepository
        .findByUserId(userId)
        .orElseGet(
            () -> {
              StudentStatsEntity newStats = new StudentStatsEntity();
              newStats.setUserId(userId);
              // Do not save here. Only save when there is actual data update (e.g. exam submission)
              return newStats;
            });
  }

  public List<StudentStatsEntity> getLeaderboard(int limit) {
    // Sort by correct answers descending, then total questions descending
    // Only include students who have answered at least one question
    return studentStatsRepository
        .findByTotalQuestionsAnsweredGreaterThan(
            0L,
            PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "correctAnswers")
                    .and(Sort.by(Sort.Direction.DESC, "totalQuestionsAnswered"))))
        .getContent();
  }
}

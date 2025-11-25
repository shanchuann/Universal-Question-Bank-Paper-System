package com.universal.qbank.service;

import com.universal.qbank.entity.StudentStatsEntity;
import com.universal.qbank.repository.StudentStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentStatsService {

    @Autowired
    private StudentStatsRepository studentStatsRepository;

    public StudentStatsEntity getStatsByUserId(String userId) {
        return studentStatsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    StudentStatsEntity newStats = new StudentStatsEntity();
                    newStats.setUserId(userId);
                    return studentStatsRepository.save(newStats);
                });
    }

    public List<StudentStatsEntity> getLeaderboard(int limit) {
        // Sort by correct answers descending, then total questions descending
        return studentStatsRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "correctAnswers")
                        .and(Sort.by(Sort.Direction.DESC, "totalQuestionsAnswered")))
        ).getContent();
    }
}

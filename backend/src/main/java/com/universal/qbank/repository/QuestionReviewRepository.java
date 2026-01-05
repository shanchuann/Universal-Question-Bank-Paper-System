package com.universal.qbank.repository;

import com.universal.qbank.entity.QuestionReviewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionReviewRepository extends JpaRepository<QuestionReviewEntity, String> {

  List<QuestionReviewEntity> findByQuestionIdOrderByCreatedAtDesc(String questionId);

  List<QuestionReviewEntity> findByReviewerId(String reviewerId);
}

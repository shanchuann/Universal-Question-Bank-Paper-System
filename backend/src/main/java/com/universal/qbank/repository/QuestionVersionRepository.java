package com.universal.qbank.repository;

import com.universal.qbank.entity.QuestionVersionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionVersionRepository extends JpaRepository<QuestionVersionEntity, String> {

  List<QuestionVersionEntity> findByQuestionIdOrderByVersionDesc(String questionId);

  Optional<QuestionVersionEntity> findByQuestionIdAndVersion(String questionId, Integer version);

  Optional<QuestionVersionEntity> findTopByQuestionIdOrderByVersionDesc(String questionId);
}

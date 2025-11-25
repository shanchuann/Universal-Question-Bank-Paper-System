package com.universal.qbank.repository;

import com.universal.qbank.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository
    extends JpaRepository<QuestionEntity, String>, JpaSpecificationExecutor<QuestionEntity> {}

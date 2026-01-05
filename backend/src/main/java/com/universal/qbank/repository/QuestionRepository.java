package com.universal.qbank.repository;

import com.universal.qbank.entity.QuestionEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository
    extends JpaRepository<QuestionEntity, String>, JpaSpecificationExecutor<QuestionEntity> {

  Page<QuestionEntity> findByStatus(String status, Pageable pageable);

  List<QuestionEntity> findByCreatedBy(String createdBy);

  Page<QuestionEntity> findByCreatedByAndStatus(String createdBy, String status, Pageable pageable);

  /** 查询指定组织的题目 + 共用题库（organizationId为null） */
  @Query("SELECT q FROM QuestionEntity q WHERE q.organizationId = :orgId OR q.organizationId IS NULL")
  Page<QuestionEntity> findByOrganizationIdOrPublic(@Param("orgId") String orgId, Pageable pageable);

  /** 查询指定组织的题目 */
  Page<QuestionEntity> findByOrganizationId(String organizationId, Pageable pageable);

  /** 查询共用题库（organizationId为null） */
  Page<QuestionEntity> findByOrganizationIdIsNull(Pageable pageable);
}

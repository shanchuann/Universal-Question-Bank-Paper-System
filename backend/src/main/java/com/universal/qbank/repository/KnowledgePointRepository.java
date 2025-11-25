package com.universal.qbank.repository;

import com.universal.qbank.entity.KnowledgePointEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgePointRepository extends JpaRepository<KnowledgePointEntity, String> {
  List<KnowledgePointEntity> findBySubjectIdOrderBySortOrderAsc(String subjectId);
  List<KnowledgePointEntity> findByParentIdOrderBySortOrderAsc(String parentId);
}

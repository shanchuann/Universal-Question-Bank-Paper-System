package com.universal.qbank.repository;

import com.universal.qbank.entity.ExportTaskEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportTaskRepository extends JpaRepository<ExportTaskEntity, String> {

  Page<ExportTaskEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

  List<ExportTaskEntity> findByStatus(String status);

  List<ExportTaskEntity> findByPaperId(Long paperId);
}

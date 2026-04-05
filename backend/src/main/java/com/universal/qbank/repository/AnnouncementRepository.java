package com.universal.qbank.repository;

import com.universal.qbank.entity.AnnouncementEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, String> {

  Page<AnnouncementEntity> findByStatus(String status, Pageable pageable);

  Page<AnnouncementEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<AnnouncementEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

  List<AnnouncementEntity> findByStatusOrderByPriorityDescPublishedAtDesc(String status);

  long countByStatus(String status);
}

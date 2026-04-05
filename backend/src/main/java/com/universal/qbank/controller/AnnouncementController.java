package com.universal.qbank.controller;

import com.universal.qbank.entity.AnnouncementEntity;
import com.universal.qbank.service.AnnouncementService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

  @Autowired private AnnouncementService announcementService;

  /** 获取所有已发布的公告（供普通用户查看） */
  @GetMapping
  public ResponseEntity<List<AnnouncementEntity>> getPublishedAnnouncements() {
    List<AnnouncementEntity> announcements = announcementService.getPublishedAnnouncements();
    return ResponseEntity.ok(announcements);
  }

  /** 获取公告详情并增加浏览次数 */
  @GetMapping("/{id}")
  public ResponseEntity<?> getAnnouncementDetail(@PathVariable String id) {
    Optional<AnnouncementEntity> optional = announcementService.getAnnouncementById(id);
    if (optional.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    AnnouncementEntity announcement = optional.get();

    // 只返回已发布的公告
    if (!"PUBLISHED".equals(announcement.getStatus())) {
      return ResponseEntity.notFound().build();
    }

    // 增加浏览次数
    announcementService.incrementViewCount(id);

    return ResponseEntity.ok(announcement);
  }
}

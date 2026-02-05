package com.universal.qbank.service;

import com.universal.qbank.entity.AnnouncementEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserService userService;

    /**
     * 创建公告
     */
    public AnnouncementEntity createAnnouncement(String title, String content, String priority, String authorId) {
        AnnouncementEntity announcement = new AnnouncementEntity();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setPriority(priority != null ? priority : "NORMAL");
        announcement.setStatus("DRAFT");
        announcement.setAuthorId(authorId);
        
        Optional<UserEntity> user = userService.getUserById(authorId);
        user.ifPresent(u -> announcement.setAuthorName(u.getUsername()));
        
        return announcementRepository.save(announcement);
    }

    /**
     * 更新公告
     */
    public AnnouncementEntity updateAnnouncement(String id, String title, String content, String priority) {
        Optional<AnnouncementEntity> optional = announcementRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Announcement not found");
        }
        
        AnnouncementEntity announcement = optional.get();
        if (title != null) announcement.setTitle(title);
        if (content != null) announcement.setContent(content);
        if (priority != null) announcement.setPriority(priority);
        
        return announcementRepository.save(announcement);
    }

    /**
     * 发布公告
     */
    public AnnouncementEntity publishAnnouncement(String id) {
        Optional<AnnouncementEntity> optional = announcementRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Announcement not found");
        }
        
        AnnouncementEntity announcement = optional.get();
        announcement.setStatus("PUBLISHED");
        announcement.setPublishedAt(OffsetDateTime.now());
        
        return announcementRepository.save(announcement);
    }

    /**
     * 归档公告
     */
    public AnnouncementEntity archiveAnnouncement(String id) {
        Optional<AnnouncementEntity> optional = announcementRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Announcement not found");
        }
        
        AnnouncementEntity announcement = optional.get();
        announcement.setStatus("ARCHIVED");
        announcement.setArchivedAt(OffsetDateTime.now());
        
        return announcementRepository.save(announcement);
    }

    /**
     * 删除公告
     */
    public void deleteAnnouncement(String id) {
        announcementRepository.deleteById(id);
    }

    /**
     * 获取公告列表
     */
    public Page<AnnouncementEntity> getAnnouncements(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        if (status != null && !status.isEmpty()) {
            return announcementRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 获取已发布的公告（用于前台展示）
     */
    public List<AnnouncementEntity> getPublishedAnnouncements() {
        return announcementRepository.findByStatusOrderByPriorityDescPublishedAtDesc("PUBLISHED");
    }

    /**
     * 获取公告详情
     */
    public Optional<AnnouncementEntity> getAnnouncementById(String id) {
        return announcementRepository.findById(id);
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount(String id) {
        Optional<AnnouncementEntity> optional = announcementRepository.findById(id);
        if (optional.isPresent()) {
            AnnouncementEntity announcement = optional.get();
            announcement.setViewCount(announcement.getViewCount() + 1);
            announcementRepository.save(announcement);
        }
    }

    /**
     * 统计各状态公告数量
     */
    public long countByStatus(String status) {
        return announcementRepository.countByStatus(status);
    }
}

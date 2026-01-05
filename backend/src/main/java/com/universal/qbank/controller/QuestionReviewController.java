package com.universal.qbank.controller;

import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.entity.QuestionReviewEntity;
import com.universal.qbank.entity.QuestionVersionEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.QuestionRepository;
import com.universal.qbank.repository.UserRepository;
import com.universal.qbank.service.QuestionReviewService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionReviewController {

  @Autowired private QuestionReviewService questionReviewService;
  @Autowired private QuestionRepository questionRepository;
  @Autowired private UserRepository userRepository;

  /** 获取学生创建的题目（用于审核） */
  @GetMapping("/student-questions")
  public ResponseEntity<Map<String, Object>> getStudentQuestions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String status) {
    
    // 获取所有学生（非教师、非管理员）的ID列表
    List<String> studentIds = userRepository.findAll().stream()
        .filter(u -> "USER".equals(u.getRole()) || "STUDENT".equals(u.getRole()))
        .map(UserEntity::getId)
        .toList();
    
    Specification<QuestionEntity> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      
      // 只查询学生创建的题目
      if (!studentIds.isEmpty()) {
        predicates.add(root.get("createdBy").in(studentIds));
      } else {
        // 如果没有学生，返回空结果
        predicates.add(cb.equal(cb.literal(1), cb.literal(0)));
      }
      
      // 状态过滤
      if (status != null && !status.isEmpty()) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      
      return cb.and(predicates.toArray(new Predicate[0]));
    };
    
    Page<QuestionEntity> pagedResult = questionRepository.findAll(
        spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    
    // 构建用户ID到昵称的映射
    Map<String, String> userNicknameMap = userRepository.findAll().stream()
        .collect(java.util.stream.Collectors.toMap(
            UserEntity::getId,
            u -> u.getNickname() != null ? u.getNickname() : u.getUsername(),
            (a, b) -> a
        ));
    
    // 将题目转换为包含昵称的Map
    List<Map<String, Object>> contentWithNickname = pagedResult.getContent().stream()
        .map(q -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", q.getId());
          map.put("stem", q.getStem());
          map.put("type", q.getType());
          map.put("difficulty", q.getDifficulty());
          map.put("status", q.getStatus());
          map.put("version", q.getVersion());
          map.put("createdBy", q.getCreatedBy());
          map.put("createdAt", q.getCreatedAt());
          map.put("optionsJson", q.getOptionsJson());
          map.put("answerSchema", q.getAnswerSchema());
          map.put("analysis", q.getAnalysis());
          map.put("reviewNotes", q.getReviewNotes());
          // 添加创建者昵称
          String creatorNickname = q.getCreatedBy() != null ? userNicknameMap.get(q.getCreatedBy()) : null;
          map.put("creatorNickname", creatorNickname != null ? creatorNickname : q.getCreatedBy());
          return map;
        })
        .toList();
    
    Map<String, Object> response = new HashMap<>();
    response.put("content", contentWithNickname);
    response.put("totalElements", pagedResult.getTotalElements());
    response.put("totalPages", pagedResult.getTotalPages());
    
    return ResponseEntity.ok(response);
  }

  /** 获取待审核题目列表 */
  @GetMapping("/pending-review")
  public ResponseEntity<Page<QuestionEntity>> getPendingReview(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        questionReviewService.getPendingReviewQuestions(PageRequest.of(page, size)));
  }

  /** 提交审核 */
  @PostMapping("/{id}/submit-review")
  public ResponseEntity<Void> submitReview(
      @PathVariable String id, @RequestBody Map<String, String> request) {
    String submitterId = request.get("submitterId");
    questionReviewService.submitForReview(id, submitterId);
    return ResponseEntity.ok().build();
  }

  /** 审核通过 */
  @PostMapping("/{id}/approve")
  public ResponseEntity<Void> approve(
      @PathVariable String id, @RequestBody Map<String, String> request) {
    String reviewerId = request.get("reviewerId");
    String notes = request.get("notes");
    questionReviewService.approve(id, reviewerId, notes);
    return ResponseEntity.ok().build();
  }

  /** 审核退回 */
  @PostMapping("/{id}/reject")
  public ResponseEntity<Void> reject(
      @PathVariable String id, @RequestBody Map<String, String> request) {
    String reviewerId = request.get("reviewerId");
    String notes = request.get("notes");
    questionReviewService.reject(id, reviewerId, notes);
    return ResponseEntity.ok().build();
  }

  /** 获取版本历史 */
  @GetMapping("/{id}/versions")
  public ResponseEntity<List<QuestionVersionEntity>> getVersions(@PathVariable String id) {
    return ResponseEntity.ok(questionReviewService.getVersionHistory(id));
  }

  /** 获取审核记录 */
  @GetMapping("/{id}/reviews")
  public ResponseEntity<List<QuestionReviewEntity>> getReviews(@PathVariable String id) {
    return ResponseEntity.ok(questionReviewService.getReviewHistory(id));
  }
}

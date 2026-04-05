package com.universal.qbank.controller;

import com.universal.qbank.entity.StudentStatsEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.UserRepository;
import com.universal.qbank.service.OrganizationService;
import com.universal.qbank.service.StudentStatsService;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StudentStatsController {

  @Autowired private StudentStatsService studentStatsService;
  @Autowired private UserRepository userRepository;
  @Autowired private OrganizationService organizationService;

  @GetMapping("/me")
  public ResponseEntity<StudentStatsEntity> getMyStats(@RequestParam String userId) {
    return ResponseEntity.ok(studentStatsService.getStatsByUserId(userId));
  }

  @GetMapping("/leaderboard")
  public ResponseEntity<List<StudentStatsEntity>> getLeaderboard(
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) String orgId,
      Authentication authentication) {

    // 获取当前用户
    String username = authentication != null ? authentication.getName() : null;
    UserEntity currentUser =
        username != null ? userRepository.findByUsername(username).orElse(null) : null;

    // 如果是教师或管理员，可以查看所有班级
    if (currentUser != null
        && ("TEACHER".equals(currentUser.getRole()) || "ADMIN".equals(currentUser.getRole()))) {
      if (orgId != null && !orgId.isEmpty()) {
        return ResponseEntity.ok(studentStatsService.getLeaderboardByOrganization(orgId, limit));
      }
      return ResponseEntity.ok(studentStatsService.getLeaderboard(limit));
    }

    // 学生必须指定班级，且只能查看自己加入的班级
    if (orgId == null || orgId.isEmpty()) {
      return ResponseEntity.ok(Collections.emptyList());
    }

    // 验证学生是否属于该班级
    if (currentUser != null) {
      boolean isMember = organizationService.isUserMemberOfOrganization(currentUser.getId(), orgId);
      if (!isMember) {
        return ResponseEntity.ok(Collections.emptyList());
      }
    }

    return ResponseEntity.ok(studentStatsService.getLeaderboardByOrganization(orgId, limit));
  }
}

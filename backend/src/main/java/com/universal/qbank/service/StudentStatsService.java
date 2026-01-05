package com.universal.qbank.service;

import com.universal.qbank.entity.StudentStatsEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.StudentStatsRepository;
import com.universal.qbank.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class StudentStatsService {

  @Autowired private StudentStatsRepository studentStatsRepository;
  @Autowired private UserRepository userRepository;

  public StudentStatsEntity getStatsByUserId(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      return new StudentStatsEntity();
    }
    return studentStatsRepository
        .findByUserId(userId)
        .orElseGet(
            () -> {
              StudentStatsEntity newStats = new StudentStatsEntity();
              newStats.setUserId(userId);
              // Do not save here. Only save when there is actual data update (e.g. exam submission)
              return newStats;
            });
  }

  /** 获取学生排行榜（只包含学生角色），并填充昵称 */
  public List<StudentStatsEntity> getLeaderboard(int limit) {
    List<StudentStatsEntity> stats = studentStatsRepository.findStudentLeaderboard(PageRequest.of(0, limit));
    fillNicknames(stats);
    return stats;
  }

  /** 获取指定班级的学生排行榜，并填充昵称 */
  public List<StudentStatsEntity> getLeaderboardByOrganization(String orgId, int limit) {
    List<StudentStatsEntity> stats = studentStatsRepository.findStudentLeaderboardByOrganization(orgId, PageRequest.of(0, limit));
    fillNicknames(stats);
    return stats;
  }

  /** 填充用户昵称 */
  private void fillNicknames(List<StudentStatsEntity> stats) {
    if (stats.isEmpty()) return;
    
    List<String> userIds = stats.stream()
        .map(StudentStatsEntity::getUserId)
        .collect(Collectors.toList());
    
    List<UserEntity> users = userRepository.findAllById(userIds);
    Map<String, String> nicknameMap = users.stream()
        .collect(Collectors.toMap(
            UserEntity::getId,
            u -> u.getNickname() != null ? u.getNickname() : u.getUsername()
        ));
    
    for (StudentStatsEntity stat : stats) {
      String nickname = nicknameMap.get(stat.getUserId());
      stat.setNickname(nickname != null ? nickname : stat.getUserId());
    }
  }
}

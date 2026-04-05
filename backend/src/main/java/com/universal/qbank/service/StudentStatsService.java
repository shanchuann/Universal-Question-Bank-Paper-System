package com.universal.qbank.service;

import com.universal.qbank.entity.StudentStatsEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.entity.UserOrganizationEntity;
import com.universal.qbank.repository.StudentStatsRepository;
import com.universal.qbank.repository.UserOrganizationRepository;
import com.universal.qbank.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class StudentStatsService {

  @Autowired private StudentStatsRepository studentStatsRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private UserOrganizationRepository userOrganizationRepository;

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
    List<StudentStatsEntity> stats =
        studentStatsRepository.findStudentLeaderboard(PageRequest.of(0, limit));
    fillNicknames(stats);
    return stats;
  }

  /** 获取指定班级的学生排行榜，并填充昵称（仅在班级成员范围内统计） */
  public List<StudentStatsEntity> getLeaderboardByOrganization(String orgId, int limit) {
    // 获取班级所有成员
    List<UserOrganizationEntity> members = userOrganizationRepository.findByOrganizationId(orgId);
    List<String> memberUserIds =
        members.stream().map(UserOrganizationEntity::getUserId).collect(Collectors.toList());

    if (memberUserIds.isEmpty()) {
      return new ArrayList<>();
    }

    // 获取只是学生角色的用户ID
    List<UserEntity> users = userRepository.findAllById(memberUserIds);
    Set<String> studentUserIds =
        users.stream()
            .filter(u -> "USER".equals(u.getRole()) || "STUDENT".equals(u.getRole()))
            .map(UserEntity::getId)
            .collect(Collectors.toSet());

    if (studentUserIds.isEmpty()) {
      return new ArrayList<>();
    }

    List<StudentStatsEntity> persistedStats =
        studentStatsRepository.findByUserIdIn(new ArrayList<>(studentUserIds));
    Map<String, StudentStatsEntity> persistedStatsMap = new HashMap<>();
    for (StudentStatsEntity stats : persistedStats) {
      persistedStatsMap.put(stats.getUserId(), stats);
    }

    // 组装班级内统计，未作答学生采用临时零值，不写入数据库
    List<StudentStatsEntity> allStats = new ArrayList<>();
    for (String userId : studentUserIds) {
      StudentStatsEntity stats = persistedStatsMap.get(userId);
      if (stats == null) {
        stats = new StudentStatsEntity();
        stats.setUserId(userId);
        stats.setTotalQuestionsAnswered(0L);
        stats.setCorrectAnswers(0L);
        stats.setCurrentStreak(0);
      }
      allStats.add(stats);
    }

    // 按正确答题数和总答题数排序
    allStats.sort(
        (a, b) -> {
          int cmp =
              Long.compare(
                  b.getCorrectAnswers() != null ? b.getCorrectAnswers() : 0,
                  a.getCorrectAnswers() != null ? a.getCorrectAnswers() : 0);
          if (cmp != 0) return cmp;
          return Long.compare(
              b.getTotalQuestionsAnswered() != null ? b.getTotalQuestionsAnswered() : 0,
              a.getTotalQuestionsAnswered() != null ? a.getTotalQuestionsAnswered() : 0);
        });

    // 限制返回数量并填充昵称
    List<StudentStatsEntity> result = allStats.stream().limit(limit).collect(Collectors.toList());
    fillNicknames(result);
    return result;
  }

  /** 填充用户昵称 */
  private void fillNicknames(List<StudentStatsEntity> stats) {
    if (stats.isEmpty()) return;

    List<String> userIds =
        stats.stream().map(StudentStatsEntity::getUserId).collect(Collectors.toList());

    List<UserEntity> users = userRepository.findAllById(userIds);
    Map<String, String> nicknameMap =
        users.stream()
            .collect(
                Collectors.toMap(
                    UserEntity::getId,
                    u -> u.getNickname() != null ? u.getNickname() : u.getUsername()));

    for (StudentStatsEntity stat : stats) {
      String nickname = nicknameMap.get(stat.getUserId());
      stat.setNickname(nickname != null ? nickname : stat.getUserId());
    }
  }

  /** 初始化学生统计记录（用于新加入班级的学生） */
  public StudentStatsEntity initializeStudentStats(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      return new StudentStatsEntity();
    }
    return studentStatsRepository
        .findByUserId(userId)
        .orElseGet(
            () -> {
              StudentStatsEntity newStats = new StudentStatsEntity();
              newStats.setUserId(userId);
              newStats.setTotalQuestionsAnswered(0L);
              newStats.setCorrectAnswers(0L);
              newStats.setCurrentStreak(0);
              return studentStatsRepository.save(newStats);
            });
  }
}

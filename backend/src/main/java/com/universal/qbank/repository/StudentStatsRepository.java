package com.universal.qbank.repository;

import com.universal.qbank.entity.StudentStatsEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentStatsRepository extends JpaRepository<StudentStatsEntity, Long> {
  Optional<StudentStatsEntity> findByUserId(String userId);

  Page<StudentStatsEntity> findByTotalQuestionsAnsweredGreaterThan(Long count, Pageable pageable);

  /** 只获取学生角色的排行榜（排除教师和管理员） */
  @Query("SELECT s FROM StudentStatsEntity s WHERE s.totalQuestionsAnswered > 0 " +
         "AND s.userId IN (SELECT u.id FROM UserEntity u WHERE u.role = 'USER' OR u.role = 'STUDENT') " +
         "ORDER BY s.correctAnswers DESC, s.totalQuestionsAnswered DESC")
  List<StudentStatsEntity> findStudentLeaderboard(Pageable pageable);

  /** 获取指定班级的学生排行榜 */
  @Query("SELECT s FROM StudentStatsEntity s WHERE s.totalQuestionsAnswered > 0 " +
         "AND s.userId IN (SELECT uo.userId FROM UserOrganizationEntity uo WHERE uo.organizationId = :orgId) " +
         "AND s.userId IN (SELECT u.id FROM UserEntity u WHERE u.role = 'USER' OR u.role = 'STUDENT') " +
         "ORDER BY s.correctAnswers DESC, s.totalQuestionsAnswered DESC")
  List<StudentStatsEntity> findStudentLeaderboardByOrganization(@Param("orgId") String orgId, Pageable pageable);
}

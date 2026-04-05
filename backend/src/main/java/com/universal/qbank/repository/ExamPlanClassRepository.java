package com.universal.qbank.repository;

import com.universal.qbank.entity.ExamPlanClassEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamPlanClassRepository
    extends JpaRepository<ExamPlanClassEntity, ExamPlanClassEntity.ExamPlanClassId> {

  /** 根据考试计划ID获取关联的班级 */
  List<ExamPlanClassEntity> findByExamPlanId(String examPlanId);

  /** 根据班级ID获取关联的考试计划 */
  List<ExamPlanClassEntity> findByClassId(String classId);

  /** 删除考试计划的所有班级关联 */
  void deleteByExamPlanId(String examPlanId);

  /** 获取某班级关联的所有考试计划ID */
  @Query("SELECT e.examPlanId FROM ExamPlanClassEntity e WHERE e.classId = :classId")
  List<String> findExamPlanIdsByClassId(@Param("classId") String classId);

  /** 检查是否存在关联 */
  boolean existsByExamPlanIdAndClassId(String examPlanId, String classId);
}

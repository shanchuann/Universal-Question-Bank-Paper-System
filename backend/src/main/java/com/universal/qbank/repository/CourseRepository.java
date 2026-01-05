package com.universal.qbank.repository;

import com.universal.qbank.entity.CourseEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, String> {

  List<CourseEntity> findByDepartmentId(String departmentId);

  List<CourseEntity> findByStatus(String status);

  boolean existsByCode(String code);
}

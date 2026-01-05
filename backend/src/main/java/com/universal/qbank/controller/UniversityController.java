package com.universal.qbank.controller;

import com.universal.qbank.entity.OrganizationEntity;
import com.universal.qbank.service.OrganizationService;
import com.universal.qbank.service.UniversityDataService;
import com.universal.qbank.service.UniversityDataService.Department;
import com.universal.qbank.service.UniversityDataService.University;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 高校数据API - 提供高校和院系数据
 */
@RestController
@RequestMapping("/api/universities")
@CrossOrigin(origins = "*")
public class UniversityController {

  @Autowired private UniversityDataService universityDataService;
  @Autowired private OrganizationService organizationService;

  /** 获取所有高校列表 */
  @GetMapping
  public ResponseEntity<List<University>> getAllUniversities() {
    return ResponseEntity.ok(universityDataService.getAllUniversities());
  }

  /** 搜索高校 */
  @GetMapping("/search")
  public ResponseEntity<List<University>> searchUniversities(@RequestParam String keyword) {
    return ResponseEntity.ok(universityDataService.searchUniversities(keyword));
  }

  /** 获取所有省份列表 */
  @GetMapping("/provinces")
  public ResponseEntity<List<String>> getAllProvinces() {
    return ResponseEntity.ok(universityDataService.getAllProvinces());
  }

  /** 按省份获取高校列表 */
  @GetMapping("/by-province/{province}")
  public ResponseEntity<List<University>> getUniversitiesByProvince(@PathVariable String province) {
    return ResponseEntity.ok(universityDataService.getUniversitiesByProvince(province));
  }

  /** 获取指定高校信息 */
  @GetMapping("/{code}")
  public ResponseEntity<University> getUniversity(@PathVariable String code) {
    University uni = universityDataService.getUniversity(code);
    if (uni == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(uni);
  }

  /** 获取指定高校的院系列表 */
  @GetMapping("/{code}/departments")
  public ResponseEntity<List<Department>> getDepartments(@PathVariable String code) {
    return ResponseEntity.ok(universityDataService.getDepartments(code));
  }

  /** 从高校模板创建完整组织架构 */
  @PostMapping("/{code}/create-organization")
  public ResponseEntity<?> createOrganizationFromUniversity(
      @PathVariable String code,
      @RequestBody Map<String, Object> request) {
    
    University uni = universityDataService.getUniversity(code);
    if (uni == null) {
      Map<String, String> error = new HashMap<>();
      error.put("message", "未找到该高校");
      return ResponseEntity.badRequest().body(error);
    }

    // 获取要创建的院系和班级信息
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> departmentClasses = 
        (List<Map<String, Object>>) request.get("departments");

    if (departmentClasses == null || departmentClasses.isEmpty()) {
      Map<String, String> error = new HashMap<>();
      error.put("message", "请至少选择一个院系");
      return ResponseEntity.badRequest().body(error);
    }

    try {
      // 1. 创建学校
      OrganizationEntity school = new OrganizationEntity();
      school.setName(uni.getName());
      school.setCode(uni.getCode());
      school.setType("SCHOOL");
      school.setStatus("ACTIVE");
      school.setSortOrder(0);
      school = organizationService.create(school);

      int deptOrder = 0;
      int totalClasses = 0;

      // 2. 创建院系和班级
      for (Map<String, Object> deptInfo : departmentClasses) {
        String deptCode = (String) deptInfo.get("code");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> classes = (List<Map<String, String>>) deptInfo.get("classes");

        // 找到对应的院系
        Department dept = uni.getDepartments().stream()
            .filter(d -> d.getCode().equals(deptCode))
            .findFirst()
            .orElse(null);

        if (dept == null) continue;

        // 创建院系
        OrganizationEntity deptEntity = new OrganizationEntity();
        deptEntity.setName(dept.getName());
        deptEntity.setCode(dept.getCode());
        deptEntity.setType("DEPARTMENT");
        deptEntity.setParentId(school.getId());
        deptEntity.setStatus("ACTIVE");
        deptEntity.setSortOrder(deptOrder++);
        deptEntity = organizationService.create(deptEntity);

        // 创建班级
        if (classes != null) {
          int classOrder = 0;
          for (Map<String, String> classInfo : classes) {
            String className = classInfo.get("name");
            String classCode = classInfo.get("code");

            OrganizationEntity classEntity = new OrganizationEntity();
            classEntity.setName(className);
            classEntity.setCode(classCode != null ? classCode : dept.getCode() + "_" + className);
            classEntity.setType("CLASS");
            classEntity.setParentId(deptEntity.getId());
            classEntity.setStatus("ACTIVE");
            classEntity.setSortOrder(classOrder++);
            // 创建时自动生成邀请码
            organizationService.create(classEntity);
            totalClasses++;
          }
        }
      }

      Map<String, Object> result = new HashMap<>();
      result.put("school", school);
      result.put("message", String.format("成功创建 %s，包含 %d 个院系，%d 个班级", 
          uni.getName(), departmentClasses.size(), totalClasses));

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("message", "创建组织失败: " + e.getMessage());
      return ResponseEntity.badRequest().body(error);
    }
  }

  /** 快速创建班级（在已有院系下） */
  @PostMapping("/quick-create-class")
  public ResponseEntity<?> quickCreateClass(@RequestBody Map<String, Object> request) {
    String departmentId = (String) request.get("departmentId");
    String className = (String) request.get("className");
    String classCode = (String) request.get("classCode");

    if (departmentId == null || className == null) {
      Map<String, String> error = new HashMap<>();
      error.put("message", "院系ID和班级名称不能为空");
      return ResponseEntity.badRequest().body(error);
    }

    try {
      OrganizationEntity classEntity = new OrganizationEntity();
      classEntity.setName(className);
      classEntity.setCode(classCode != null ? classCode : "CLASS_" + System.currentTimeMillis());
      classEntity.setType("CLASS");
      classEntity.setParentId(departmentId);
      classEntity.setStatus("ACTIVE");
      classEntity.setSortOrder(0);

      OrganizationEntity created = organizationService.create(classEntity);

      Map<String, Object> result = new HashMap<>();
      result.put("class", created);
      result.put("inviteCode", created.getInviteCode());
      result.put("message", "班级创建成功");

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      Map<String, String> error = new HashMap<>();
      error.put("message", "创建班级失败: " + e.getMessage());
      return ResponseEntity.badRequest().body(error);
    }
  }
}

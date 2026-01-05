package com.universal.qbank.controller;

import com.universal.qbank.entity.OrganizationEntity;
import com.universal.qbank.service.OrganizationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@CrossOrigin(origins = "*")
public class OrganizationController {

  @Autowired private OrganizationService organizationService;

  /** 获取组织树 */
  @GetMapping
  public ResponseEntity<List<OrganizationEntity>> getTree() {
    return ResponseEntity.ok(organizationService.getOrganizationTree());
  }

  /** 按类型获取组织列表 */
  @GetMapping("/by-type/{type}")
  public ResponseEntity<List<OrganizationEntity>> getByType(@PathVariable String type) {
    return ResponseEntity.ok(organizationService.getByType(type));
  }

  /** 获取有学生成员的班级列表（用于排行榜等） */
  @GetMapping("/classes-with-members")
  public ResponseEntity<List<OrganizationEntity>> getClassesWithMembers() {
    return ResponseEntity.ok(organizationService.getClassesWithMembers());
  }

  /** 获取单个组织 */
  @GetMapping("/{id}")
  public ResponseEntity<OrganizationEntity> getById(@PathVariable String id) {
    return organizationService
        .getById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /** 创建组织 */
  @PostMapping
  public ResponseEntity<OrganizationEntity> create(
      @RequestBody OrganizationEntity org,
      @RequestHeader(value = "Authorization", required = false) String token) {
    String userId = getUserIdFromToken(token);
    org.setCreatedBy(userId);
    OrganizationEntity created = organizationService.create(org);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /** 更新组织 */
  @PutMapping("/{id}")
  public ResponseEntity<OrganizationEntity> update(
      @PathVariable String id, @RequestBody OrganizationEntity org) {
    OrganizationEntity updated = organizationService.update(id, org);
    return ResponseEntity.ok(updated);
  }

  /** 删除组织 */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    organizationService.delete(id);
    return ResponseEntity.noContent().build();
  }

  /** 刷新组织邀请码 (教师/管理员) */
  @PostMapping("/{id}/refresh-invite-code")
  public ResponseEntity<Map<String, String>> refreshInviteCode(@PathVariable String id) {
    String newCode = organizationService.refreshInviteCode(id);
    Map<String, String> response = new HashMap<>();
    response.put("inviteCode", newCode);
    return ResponseEntity.ok(response);
  }

  /** 通过邀请码加入组织 (学生) */
  @PostMapping("/join")
  public ResponseEntity<?> joinByInviteCode(@RequestBody Map<String, String> request,
      @RequestHeader(value = "Authorization", required = false) String token) {
    String inviteCode = request.get("inviteCode");
    if (inviteCode == null || inviteCode.trim().isEmpty()) {
      Map<String, String> error = new HashMap<>();
      error.put("message", "请输入班级识别码");
      return ResponseEntity.badRequest().body(error);
    }
    
    // 从Authorization token中获取userId
    String userId = getUserIdFromToken(token);
    
    try {
      OrganizationEntity org = organizationService.joinByInviteCode(userId, inviteCode.trim());
      return ResponseEntity.ok(org);
    } catch (RuntimeException e) {
      Map<String, String> error = new HashMap<>();
      error.put("message", e.getMessage());
      return ResponseEntity.badRequest().body(error);
    }
  }

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return "current-user";
  }
}

/** 用户组织控制器 */
@RestController
@RequestMapping("/api/my-organizations")
@CrossOrigin(origins = "*")
class MyOrganizationController {

  @Autowired private OrganizationService organizationService;

  /** 获取当前用户加入的组织（包含层级信息） */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> getMyOrganizations(
      @RequestHeader(value = "Authorization", required = false) String token) {
    String userId = getUserIdFromToken(token);
    List<Map<String, Object>> result = organizationService.getUserOrganizationsWithDetails(userId);
    return ResponseEntity.ok(result);
  }

  /** 退出组织 */
  @PostMapping("/{orgId}/leave")
  public ResponseEntity<Void> leaveOrganization(
      @PathVariable String orgId,
      @RequestHeader(value = "Authorization", required = false) String token) {
    String userId = getUserIdFromToken(token);
    organizationService.leaveOrganization(userId, orgId);
    return ResponseEntity.ok().build();
  }

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return "current-user";
  }
}

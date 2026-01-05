package com.universal.qbank.service;

import com.universal.qbank.entity.OrganizationEntity;
import com.universal.qbank.entity.UserOrganizationEntity;
import com.universal.qbank.repository.OrganizationRepository;
import com.universal.qbank.repository.UserOrganizationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private UserOrganizationRepository userOrganizationRepository;

  private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int INVITE_CODE_LENGTH = 6;

  /** 获取组织树 */
  public List<OrganizationEntity> getOrganizationTree() {
    List<OrganizationEntity> all = organizationRepository.findAll();
    Map<String, List<OrganizationEntity>> byParent =
        all.stream()
            .filter(o -> o.getParentId() != null)
            .collect(Collectors.groupingBy(OrganizationEntity::getParentId));

    List<OrganizationEntity> roots =
        all.stream().filter(o -> o.getParentId() == null).collect(Collectors.toList());

    roots.forEach(root -> buildChildren(root, byParent));
    return roots;
  }

  private void buildChildren(
      OrganizationEntity parent, Map<String, List<OrganizationEntity>> byParent) {
    List<OrganizationEntity> children =
        byParent.getOrDefault(parent.getId(), new ArrayList<>());
    children.sort((a, b) -> {
      int sa = a.getSortOrder() != null ? a.getSortOrder() : 0;
      int sb = b.getSortOrder() != null ? b.getSortOrder() : 0;
      return Integer.compare(sa, sb);
    });
    parent.setChildren(children);
    children.forEach(child -> buildChildren(child, byParent));
  }

  /** 获取单个组织 */
  public Optional<OrganizationEntity> getById(String id) {
    return organizationRepository.findById(id);
  }

  /** 创建组织 */
  @Transactional
  public OrganizationEntity create(OrganizationEntity org) {
    // 如果是班级类型，自动生成邀请码
    if ("CLASS".equals(org.getType())) {
      org.setInviteCode(generateInviteCode());
    }
    return organizationRepository.save(org);
  }

  /** 更新组织 */
  @Transactional
  public OrganizationEntity update(String id, OrganizationEntity org) {
    OrganizationEntity existing =
        organizationRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    existing.setName(org.getName());
    existing.setCode(org.getCode());
    existing.setType(org.getType());
    existing.setParentId(org.getParentId());
    existing.setSortOrder(org.getSortOrder());
    existing.setStatus(org.getStatus());
    // 只有在原来没有创建者的情况下才允许设置
    if (existing.getCreatedBy() == null && org.getCreatedBy() != null) {
      existing.setCreatedBy(org.getCreatedBy());
    }
    return organizationRepository.save(existing);
  }

  /** 删除组织 */
  @Transactional
  public void delete(String id) {
    organizationRepository.deleteById(id);
  }

  /** 按类型获取组织列表 */
  public List<OrganizationEntity> getByType(String type) {
    return organizationRepository.findByTypeAndStatus(type, "ACTIVE");
  }

  /** 获取有学生成员的班级列表 */
  public List<OrganizationEntity> getClassesWithMembers() {
    List<OrganizationEntity> allClasses = organizationRepository.findByTypeAndStatus("CLASS", "ACTIVE");
    return allClasses.stream()
        .filter(org -> userOrganizationRepository.countByOrganizationId(org.getId()) > 0)
        .collect(Collectors.toList());
  }

  /** 生成邀请码 */
  public String generateInviteCode() {
    Random random = new Random();
    StringBuilder code = new StringBuilder();
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
      code.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
    }
    return code.toString();
  }

  /** 刷新组织邀请码 */
  @Transactional
  public String refreshInviteCode(String orgId) {
    OrganizationEntity org = organizationRepository.findById(orgId)
        .orElseThrow(() -> new RuntimeException("组织不存在"));
    String newCode = generateInviteCode();
    org.setInviteCode(newCode);
    organizationRepository.save(org);
    return newCode;
  }

  /** 通过邀请码加入组织 */
  @Transactional
  public OrganizationEntity joinByInviteCode(String userId, String inviteCode) {
    OrganizationEntity org = organizationRepository.findByInviteCode(inviteCode.toUpperCase())
        .orElseThrow(() -> new RuntimeException("无效的识别码"));
    
    if (!"ACTIVE".equals(org.getStatus())) {
      throw new RuntimeException("该班级已停用");
    }
    
    // 检查是否已加入
    if (userOrganizationRepository.existsByUserIdAndOrganizationId(userId, org.getId())) {
      throw new RuntimeException("您已加入该班级");
    }

    // 创建用户-组织关联
    UserOrganizationEntity uo = new UserOrganizationEntity();
    uo.setUserId(userId);
    uo.setOrganizationId(org.getId());
    uo.setRoleInOrg("MEMBER");
    userOrganizationRepository.save(uo);

    return org;
  }

  /** 获取用户加入的组织列表 */
  public List<OrganizationEntity> getUserOrganizations(String userId) {
    List<UserOrganizationEntity> userOrgs = userOrganizationRepository.findByUserId(userId);
    List<String> orgIds = userOrgs.stream()
        .map(UserOrganizationEntity::getOrganizationId)
        .collect(Collectors.toList());
    return organizationRepository.findAllById(orgIds);
  }

  /** 获取用户加入的组织列表（包含详细信息）- 同时包含用户创建的班级 */
  public List<Map<String, Object>> getUserOrganizationsWithDetails(String userId) {
    // 获取用户加入的组织
    List<UserOrganizationEntity> userOrgs = userOrganizationRepository.findByUserId(userId);
    List<String> joinedOrgIds = userOrgs.stream()
        .map(UserOrganizationEntity::getOrganizationId)
        .collect(Collectors.toList());
    
    // 获取用户创建的组织
    List<OrganizationEntity> createdOrgs = organizationRepository.findByCreatedBy(userId);
    List<String> createdOrgIds = createdOrgs.stream()
        .map(OrganizationEntity::getId)
        .collect(Collectors.toList());
    
    // 合并去重
    java.util.Set<String> allOrgIds = new java.util.HashSet<>(joinedOrgIds);
    allOrgIds.addAll(createdOrgIds);
    
    List<OrganizationEntity> orgs = organizationRepository.findAllById(allOrgIds);
    
    // 创建用户角色映射（加入的组织）
    Map<String, String> roleMap = userOrgs.stream()
        .collect(Collectors.toMap(
            UserOrganizationEntity::getOrganizationId, 
            uo -> uo.getRoleInOrg() != null ? uo.getRoleInOrg() : "MEMBER"
        ));
    
    // 创建用户加入时间映射
    Map<String, java.time.OffsetDateTime> joinTimeMap = userOrgs.stream()
        .collect(Collectors.toMap(
            UserOrganizationEntity::getOrganizationId, 
            uo -> uo.getJoinedAt() != null ? uo.getJoinedAt() : java.time.OffsetDateTime.now()
        ));
    
    return orgs.stream().map(org -> {
      Map<String, Object> result = new java.util.LinkedHashMap<>();
      result.put("id", org.getId());
      result.put("name", org.getName());
      result.put("code", org.getCode());
      result.put("type", org.getType());
      result.put("status", org.getStatus());
      result.put("inviteCode", org.getInviteCode());
      
      // 判断用户角色：创建者为 OWNER，否则取加入时的角色
      boolean isCreator = userId.equals(org.getCreatedBy());
      if (isCreator) {
        result.put("memberRole", "OWNER");
        result.put("joinedAt", org.getCreatedAt());
      } else {
        result.put("memberRole", roleMap.get(org.getId()));
        result.put("joinedAt", joinTimeMap.get(org.getId()));
      }
      result.put("isCreator", isCreator);
      
      // 获取父级组织信息（学院、学校）
      if (org.getParentId() != null) {
        organizationRepository.findById(org.getParentId()).ifPresent(parent -> {
          result.put("parentName", parent.getName());
          result.put("parentType", parent.getType());
          
          // 如果父级是学院，继续获取学校
          if (parent.getParentId() != null) {
            organizationRepository.findById(parent.getParentId()).ifPresent(grandParent -> {
              result.put("schoolName", grandParent.getName());
            });
          } else if ("SCHOOL".equals(parent.getType())) {
            result.put("schoolName", parent.getName());
          }
        });
      }
      
      // 获取班级成员数量
      long memberCount = userOrganizationRepository.countByOrganizationId(org.getId());
      result.put("memberCount", memberCount);
      
      return result;
    }).collect(Collectors.toList());
  }

  /** 退出组织 */
  @Transactional
  public void leaveOrganization(String userId, String orgId) {
    userOrganizationRepository.deleteByUserIdAndOrganizationId(userId, orgId);
  }
}

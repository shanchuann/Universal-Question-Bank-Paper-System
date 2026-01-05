package com.universal.qbank.security;

import com.universal.qbank.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限检查切面
 * 
 * 拦截带有 @RequiredPermission 和 @RequiredRole 注解的方法，执行权限验证
 */
@Aspect
@Component
public class PermissionAspect {

  @Autowired
  private PermissionService permissionService;

  /**
   * 拦截 @RequiredPermission 注解
   */
  @Before("@annotation(requiredPermission)")
  public void checkPermission(JoinPoint joinPoint, RequiredPermission requiredPermission) {
    String userId = getCurrentUserId();
    if (userId == null) {
      throw new SecurityException("未登录，请先登录");
    }

    String[] requiredPerms = requiredPermission.value();
    boolean requireAll = requiredPermission.requireAll();
    
    Set<String> userPermissions = permissionService.getUserPermissions(userId);

    if (requireAll) {
      // 需要满足所有权限
      boolean hasAll = Arrays.stream(requiredPerms)
          .allMatch(userPermissions::contains);
      if (!hasAll) {
        throw new SecurityException("权限不足：需要以下所有权限 " + Arrays.toString(requiredPerms));
      }
    } else {
      // 满足任一权限即可
      boolean hasAny = Arrays.stream(requiredPerms)
          .anyMatch(userPermissions::contains);
      if (!hasAny) {
        throw new SecurityException("权限不足：需要以下任一权限 " + Arrays.toString(requiredPerms));
      }
    }
  }

  /**
   * 拦截 @RequiredRole 注解
   */
  @Before("@annotation(requiredRole)")
  public void checkRole(JoinPoint joinPoint, RequiredRole requiredRole) {
    String userId = getCurrentUserId();
    if (userId == null) {
      throw new SecurityException("未登录，请先登录");
    }

    String[] requiredRoles = requiredRole.value();
    boolean requireAll = requiredRole.requireAll();

    if (requireAll) {
      boolean hasAll = Arrays.stream(requiredRoles)
          .allMatch(role -> permissionService.hasRole(userId, role));
      if (!hasAll) {
        throw new SecurityException("权限不足：需要以下所有角色 " + Arrays.toString(requiredRoles));
      }
    } else {
      boolean hasAny = Arrays.stream(requiredRoles)
          .anyMatch(role -> permissionService.hasRole(userId, role));
      if (!hasAny) {
        throw new SecurityException("权限不足：需要以下任一角色 " + Arrays.toString(requiredRoles));
      }
    }
  }

  /**
   * 从请求中获取当前用户ID
   * 
   * 通常从JWT token或session中获取
   */
  private String getCurrentUserId() {
    ServletRequestAttributes attributes = 
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return null;
    }

    HttpServletRequest request = attributes.getRequest();
    
    // 尝试从请求属性中获取（通常由JWT过滤器设置）
    Object userId = request.getAttribute("userId");
    if (userId != null) {
      return userId.toString();
    }

    // 尝试从header中获取（用于测试或特定场景）
    String userIdHeader = request.getHeader("X-User-Id");
    if (userIdHeader != null && !userIdHeader.isEmpty()) {
      return userIdHeader;
    }

    // 尝试从Authorization header解析（简化版，实际应该解析JWT）
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      // 这里应该解析JWT token获取用户ID
      // 目前为了兼容性，返回null让系统使用默认行为
      return null;
    }

    return null;
  }
}

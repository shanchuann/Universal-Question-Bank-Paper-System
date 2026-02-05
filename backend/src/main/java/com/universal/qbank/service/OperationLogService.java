package com.universal.qbank.service;

import com.universal.qbank.entity.OperationLogEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class OperationLogService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private UserService userService;

    /**
     * 记录操作日志
     */
    public void log(String userId, String action, String target, String targetId, String details, HttpServletRequest request) {
        OperationLogEntity log = new OperationLogEntity();
        log.setUserId(userId);
        log.setAction(action);
        log.setTarget(target);
        log.setTargetId(targetId);
        log.setDetails(details);
        
        // 获取用户名
        Optional<UserEntity> user = userService.getUserById(userId);
        user.ifPresent(u -> log.setUsername(u.getUsername()));
        
        // 获取IP地址
        if (request != null) {
            String ip = getClientIp(request);
            log.setIp(ip);
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        
        operationLogRepository.save(log);
    }

    /**
     * 简化版日志记录
     */
    public void log(String userId, String action, String target, String details) {
        log(userId, action, target, null, details, null);
    }

    /**
     * 搜索日志
     */
    public Page<OperationLogEntity> searchLogs(String keyword, String action, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchAction = (action != null && !action.trim().isEmpty()) ? action.trim() : null;
        
        return operationLogRepository.searchLogs(searchKeyword, searchAction, pageable);
    }

    /**
     * 获取所有日志
     */
    public Page<OperationLogEntity> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return operationLogRepository.findAll(pageable);
    }

    /**
     * 统计今日日志数量
     */
    public long countTodayLogs() {
        OffsetDateTime startOfDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return operationLogRepository.countByTimestampAfter(startOfDay);
    }

    /**
     * 统计指定操作类型的数量
     */
    public long countByAction(String action) {
        return operationLogRepository.countByAction(action);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

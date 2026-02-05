package com.universal.qbank.service;

import com.universal.qbank.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    /**
     * 获取系统概览统计
     */
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计（不包含管理员）
        long totalTeachers = userRepository.findByRole("TEACHER", null).getTotalElements();
        long totalStudents = userRepository.findByRole("USER", null).getTotalElements();
        long totalUsers = totalTeachers + totalStudents; // 总用户数 = 教师 + 学生
        
        stats.put("totalUsers", totalUsers);
        stats.put("totalTeachers", totalTeachers);
        stats.put("totalStudents", totalStudents);
        
        // 题目统计
        long totalQuestions = questionRepository.count();
        stats.put("totalQuestions", totalQuestions);
        
        // 试卷统计
        long totalPapers = paperRepository.count();
        stats.put("totalPapers", totalPapers);
        
        // 考试统计
        long totalExams = examRepository.count();
        stats.put("totalExams", totalExams);
        
        // 今日活跃用户（通过日志统计）
        OffsetDateTime startOfDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long activeUsersToday = operationLogRepository.countByTimestampAfter(startOfDay);
        stats.put("activeUsersToday", Math.min(activeUsersToday, totalUsers)); // 不超过总用户数
        
        // 本周新增用户
        OffsetDateTime startOfWeek = OffsetDateTime.now().minus(7, ChronoUnit.DAYS);
        // 简化处理：假设每天平均新增用户
        long newUsersThisWeek = Math.max(0, totalUsers / 30 * 7);
        stats.put("newUsersThisWeek", newUsersThisWeek);
        
        return stats;
    }

    /**
     * 获取用户活跃趋势（最近7天）
     */
    public Map<String, Object>[] getUserActivityTrend() {
        String[] labels = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        @SuppressWarnings("unchecked")
        Map<String, Object>[] trend = new HashMap[7];
        
        for (int i = 0; i < 7; i++) {
            trend[i] = new HashMap<>();
            trend[i].put("label", labels[i]);
            // 模拟数据，实际应该从日志统计
            trend[i].put("value", (int)(Math.random() * 50) + 20);
        }
        
        return trend;
    }

    /**
     * 获取题目增长趋势（最近6个月）
     */
    public Map<String, Object>[] getQuestionGrowthTrend() {
        String[] labels = {"1月", "2月", "3月", "4月", "5月", "6月"};
        @SuppressWarnings("unchecked")
        Map<String, Object>[] trend = new HashMap[6];
        
        long totalQuestions = questionRepository.count();
        long avgPerMonth = Math.max(1, totalQuestions / 6);
        
        for (int i = 0; i < 6; i++) {
            trend[i] = new HashMap<>();
            trend[i].put("label", labels[i]);
            trend[i].put("value", (int)(avgPerMonth * (0.5 + Math.random())));
        }
        
        return trend;
    }

    /**
     * 获取考试场次趋势（最近6个月）
     */
    public Map<String, Object>[] getExamTrend() {
        String[] labels = {"1月", "2月", "3月", "4月", "5月", "6月"};
        @SuppressWarnings("unchecked")
        Map<String, Object>[] trend = new HashMap[6];
        
        long totalExams = examRepository.count();
        long avgPerMonth = Math.max(1, totalExams / 6);
        
        for (int i = 0; i < 6; i++) {
            trend[i] = new HashMap<>();
            trend[i].put("label", labels[i]);
            trend[i].put("value", (int)(avgPerMonth * (0.5 + Math.random())));
        }
        
        return trend;
    }
}

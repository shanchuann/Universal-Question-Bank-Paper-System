package com.universal.qbank.service;

import com.universal.qbank.repository.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

  private static final List<String> USER_ROLES = Arrays.asList("TEACHER", "USER");

  @Autowired private UserRepository userRepository;

  @Autowired private QuestionRepository questionRepository;

  @Autowired private PaperRepository paperRepository;

  @Autowired private ExamRepository examRepository;

  @Autowired private OperationLogRepository operationLogRepository;

  /** 获取系统概览统计 */
  public Map<String, Object> getOverviewStats(String range) {
    Map<String, Object> stats = new HashMap<>();
    TimeRange timeRange = resolveTimeRange(range);

    // 用户统计（不包含管理员）
    long totalTeachers = userRepository.countByRoleIn(List.of("TEACHER"));
    long totalStudents = userRepository.countByRoleIn(List.of("USER"));
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

    // 活跃用户（按选择时间范围统计，去重）
    long activeUsersInRange =
        operationLogRepository.countDistinctUsersByTimestampBetween(
            timeRange.start(), timeRange.end());
    stats.put("activeUsersToday", Math.min(activeUsersInRange, totalUsers));

    // 新增用户（按选择时间范围）
    long newUsersInRange =
        userRepository.countByRoleInAndCreatedAtBetween(
            USER_ROLES, timeRange.start(), timeRange.end());
    stats.put("newUsersThisWeek", newUsersInRange);

    return stats;
  }

  /** 获取用户活跃趋势（最近7天） */
  public List<Map<String, Object>> getUserActivityTrend(String range) {
    TimeRange timeRange = resolveTimeRange(range);
    List<TimeBucket> buckets = buildBuckets(timeRange);
    List<Map<String, Object>> trend = new ArrayList<>();

    for (TimeBucket bucket : buckets) {
      long value =
          operationLogRepository.countDistinctUsersByTimestampBetween(bucket.start(), bucket.end());
      trend.add(toTrendItem(bucket.label(), value));
    }
    return trend;
  }

  /** 获取题目增长趋势（最近6个月） */
  public List<Map<String, Object>> getQuestionGrowthTrend(String range) {
    TimeRange timeRange = resolveTimeRange(range);
    List<TimeBucket> buckets = buildBuckets(timeRange);
    List<Map<String, Object>> trend = new ArrayList<>();

    for (TimeBucket bucket : buckets) {
      long value = questionRepository.countByCreatedAtBetween(bucket.start(), bucket.end());
      trend.add(toTrendItem(bucket.label(), value));
    }
    return trend;
  }

  /** 获取考试场次趋势（最近6个月） */
  public List<Map<String, Object>> getExamTrend(String range) {
    TimeRange timeRange = resolveTimeRange(range);
    List<TimeBucket> buckets = buildBuckets(timeRange);
    List<Map<String, Object>> trend = new ArrayList<>();

    for (TimeBucket bucket : buckets) {
      long value = examRepository.countByStartTimeBetween(bucket.start(), bucket.end());
      trend.add(toTrendItem(bucket.label(), value));
    }
    return trend;
  }

  private TimeRange resolveTimeRange(String range) {
    OffsetDateTime end = OffsetDateTime.now();
    int days =
        switch (range == null ? "30d" : range.toLowerCase()) {
          case "7d" -> 7;
          case "90d" -> 90;
          case "180d" -> 180;
          default -> 30;
        };
    return new TimeRange(end.minusDays(days), end, days);
  }

  private List<TimeBucket> buildBuckets(TimeRange range) {
    if (range.days() <= 31) {
      return buildDailyBuckets(range.start(), range.end());
    }
    return buildMonthlyBuckets(range.start(), range.end());
  }

  private List<TimeBucket> buildDailyBuckets(OffsetDateTime start, OffsetDateTime end) {
    List<TimeBucket> buckets = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
    LocalDate cursor = start.toLocalDate();
    LocalDate endDate = end.toLocalDate();

    while (!cursor.isAfter(endDate)) {
      OffsetDateTime bucketStart = cursor.atStartOfDay().atOffset(start.getOffset());
      OffsetDateTime bucketEnd = bucketStart.plusDays(1);
      buckets.add(new TimeBucket(bucketStart, bucketEnd, cursor.format(formatter)));
      cursor = cursor.plusDays(1);
    }
    return buckets;
  }

  private List<TimeBucket> buildMonthlyBuckets(OffsetDateTime start, OffsetDateTime end) {
    List<TimeBucket> buckets = new ArrayList<>();
    YearMonth cursor = YearMonth.from(start);
    YearMonth endMonth = YearMonth.from(end);

    while (!cursor.isAfter(endMonth)) {
      OffsetDateTime bucketStart = cursor.atDay(1).atStartOfDay().atOffset(start.getOffset());
      OffsetDateTime bucketEnd = bucketStart.plusMonths(1);
      buckets.add(new TimeBucket(bucketStart, bucketEnd, cursor.getMonthValue() + "月"));
      cursor = cursor.plusMonths(1);
    }
    return buckets;
  }

  private Map<String, Object> toTrendItem(String label, long value) {
    Map<String, Object> item = new HashMap<>();
    item.put("label", label);
    item.put("value", value);
    return item;
  }

  private record TimeRange(OffsetDateTime start, OffsetDateTime end, int days) {}

  private record TimeBucket(OffsetDateTime start, OffsetDateTime end, String label) {}
}

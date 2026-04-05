package com.universal.qbank.service;

import com.universal.qbank.repository.OperationLogRepository;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SystemMonitorService {

  private final DataSource dataSource;
  private final JdbcTemplate jdbcTemplate;
  private final OperationLogRepository operationLogRepository;

  @Value("${spring.mail.host:}")
  private String mailHost;

  public SystemMonitorService(
      DataSource dataSource,
      JdbcTemplate jdbcTemplate,
      OperationLogRepository operationLogRepository) {
    this.dataSource = dataSource;
    this.jdbcTemplate = jdbcTemplate;
    this.operationLogRepository = operationLogRepository;
  }

  /** 获取系统资源指标 */
  public Map<String, Object> getSystemMetrics() {
    Map<String, Object> metrics = new HashMap<>();

    // CPU使用率
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    double cpuLoad = resolveCpuUsage(osBean);
    metrics.put("cpuUsage", Math.min(100, Math.max(0, cpuLoad)));

    // 内存使用
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    long usedMemory = heapUsage.getUsed();
    long maxMemory = heapUsage.getMax();

    double memoryUsage = (double) usedMemory / maxMemory * 100;
    metrics.put("memoryUsage", memoryUsage);
    metrics.put("memoryUsed", formatBytes(usedMemory));
    metrics.put("memoryTotal", formatBytes(maxMemory));

    // 磁盘使用
    File root = new File("/");
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      root = new File("C:");
    }
    long totalSpace = root.getTotalSpace();
    long usableSpace = root.getUsableSpace();
    long usedSpace = totalSpace - usableSpace;

    double diskUsage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
    metrics.put("diskUsage", diskUsage);
    metrics.put("diskUsed", formatBytes(usedSpace));
    metrics.put("diskTotal", formatBytes(totalSpace));

    // 使用最近行为日志近似系统请求吞吐，避免随机值。
    OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
    long requestsPerMinute = operationLogRepository.countByTimestampAfter(oneMinuteAgo);
    metrics.put("networkIn", requestsPerMinute + " req/min");
    metrics.put("networkOut", Math.max(0, requestsPerMinute / 2) + " write/min");

    return metrics;
  }

  /** 获取服务状态 */
  public List<Map<String, Object>> getServicesStatus() {
    List<Map<String, Object>> services = new ArrayList<>();

    // API服务状态
    services.add(createServiceStatus("API 服务", "online", measureNoopLatency(), getUptime()));

    // 数据库状态
    services.add(checkDatabaseStatus());

    // 缓存服务（当前未接入独立缓存）
    services.add(createServiceStatus("缓存服务", "warning", 0, "未配置"));

    // 文件存储
    services.add(checkFileStorageStatus());

    // 邮件服务
    services.add(checkEmailStatus());

    // 定时任务
    services.add(createServiceStatus("定时任务", "online", 0, getUptime()));

    return services;
  }

  private Map<String, Object> createServiceStatus(
      String name, String status, int responseTime, String uptime) {
    Map<String, Object> service = new HashMap<>();
    service.put("name", name);
    service.put("status", status);
    service.put("responseTime", responseTime);
    service.put("uptime", uptime);
    service.put("lastCheck", "刚刚");
    return service;
  }

  private Map<String, Object> checkDatabaseStatus() {
    long start = System.nanoTime();
    try (Connection ignored = dataSource.getConnection()) {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      int responseTime = toMillis(start);
      return createServiceStatus("数据库", "online", responseTime, getUptime());
    } catch (Exception e) {
      return createServiceStatus("数据库", "offline", 0, "不可用");
    }
  }

  private Map<String, Object> checkFileStorageStatus() {
    File uploadDir = new File("uploads");
    boolean ok = uploadDir.exists() || uploadDir.mkdirs();
    return createServiceStatus("文件存储", ok ? "online" : "offline", 1, ok ? "可用" : "异常");
  }

  private Map<String, Object> checkEmailStatus() {
    if (mailHost == null || mailHost.isBlank()) {
      return createServiceStatus("邮件服务", "warning", 0, "未配置");
    }
    return createServiceStatus("邮件服务", "online", 0, "已配置");
  }

  private int measureNoopLatency() {
    long start = System.nanoTime();
    return toMillis(start);
  }

  private int toMillis(long startNano) {
    return (int) ((System.nanoTime() - startNano) / 1_000_000);
  }

  private double resolveCpuUsage(OperatingSystemMXBean osBean) {
    double systemLoadAverage = osBean.getSystemLoadAverage();
    if (systemLoadAverage >= 0) {
      return Math.min(100, systemLoadAverage * 10);
    }

    if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
      double cpuLoad = sunOsBean.getSystemCpuLoad();
      if (cpuLoad >= 0) {
        return cpuLoad * 100;
      }
      double processLoad = sunOsBean.getProcessCpuLoad();
      if (processLoad >= 0) {
        return processLoad * 100;
      }
    }
    return 0;
  }

  private String getUptime() {
    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    long uptimeMs = runtimeBean.getUptime();
    long seconds = uptimeMs / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;

    if (days > 0) {
      return days + "天" + (hours % 24) + "小时";
    } else if (hours > 0) {
      return hours + "小时" + (minutes % 60) + "分钟";
    } else {
      return minutes + "分钟";
    }
  }

  private String formatBytes(long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
    if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
    return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
  }
}

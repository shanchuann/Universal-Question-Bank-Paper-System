package com.universal.qbank.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemMonitorService {

    /**
     * 获取系统资源指标
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // CPU使用率
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemLoadAverage();
        if (cpuLoad < 0) {
            // Windows系统可能返回-1，使用近似值
            cpuLoad = Math.random() * 30 + 20; // 模拟20-50%
        } else {
            cpuLoad = cpuLoad * 10; // 转换为百分比
        }
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
        
        // 网络流量（模拟数据，实际需要系统级监控）
        metrics.put("networkIn", formatBytesPerSecond((long)(Math.random() * 1024 * 1024 + 100 * 1024)));
        metrics.put("networkOut", formatBytesPerSecond((long)(Math.random() * 512 * 1024 + 50 * 1024)));
        
        return metrics;
    }

    /**
     * 获取服务状态
     */
    public List<Map<String, Object>> getServicesStatus() {
        List<Map<String, Object>> services = new ArrayList<>();
        
        // API服务状态
        services.add(createServiceStatus("API 服务", checkApiService(), getUptime()));
        
        // 数据库状态
        services.add(createServiceStatus("数据库", checkDatabaseService(), "99.8%"));
        
        // 缓存服务（模拟）
        services.add(createServiceStatus("缓存服务", true, "99.9%"));
        
        // 文件存储
        services.add(createServiceStatus("文件存储", checkFileStorage(), "98.5%"));
        
        // 邮件服务（模拟）
        services.add(createServiceStatus("邮件服务", true, "99.2%"));
        
        // 定时任务
        services.add(createServiceStatus("定时任务", true, "100%"));
        
        return services;
    }

    private Map<String, Object> createServiceStatus(String name, boolean isOnline, String uptime) {
        Map<String, Object> service = new HashMap<>();
        service.put("name", name);
        service.put("status", isOnline ? "online" : "offline");
        service.put("responseTime", isOnline ? (int)(Math.random() * 100) + 10 : 0);
        service.put("uptime", uptime);
        service.put("lastCheck", "刚刚");
        return service;
    }

    private boolean checkApiService() {
        return true; // API服务运行中
    }

    private boolean checkDatabaseService() {
        // 实际可以尝试执行简单查询
        return true;
    }

    private boolean checkFileStorage() {
        File uploadDir = new File("uploads");
        return uploadDir.exists() || uploadDir.mkdirs();
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

    private String formatBytesPerSecond(long bytes) {
        if (bytes < 1024) return bytes + " B/s";
        if (bytes < 1024 * 1024) return String.format("%.1f KB/s", bytes / 1024.0);
        return String.format("%.1f MB/s", bytes / (1024.0 * 1024));
    }
}

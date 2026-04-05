package com.universal.qbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.controller.dto.SystemSettingsUpdateRequest;
import com.universal.qbank.entity.AnnouncementEntity;
import com.universal.qbank.entity.OperationLogEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.service.AnnouncementService;
import com.universal.qbank.service.OllamaAiService;
import com.universal.qbank.service.OllamaStartupService;
import com.universal.qbank.service.OperationLogService;
import com.universal.qbank.service.StatisticsService;
import com.universal.qbank.service.SystemConfigService;
import com.universal.qbank.service.SystemMonitorService;
import com.universal.qbank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  @Autowired private UserService userService;

  @Autowired private SystemConfigService systemConfigService;

  @Autowired private OperationLogService operationLogService;

  @Autowired private AnnouncementService announcementService;

  @Autowired private StatisticsService statisticsService;

  @Autowired private SystemMonitorService systemMonitorService;

  @Autowired private OllamaAiService ollamaAiService;

  @Autowired private OllamaStartupService ollamaStartupService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private boolean isAdmin(String token) {
    String userId = getUserIdFromToken(token);
    log.debug("Check admin access for userId={}", userId);
    if (userId == null) return false;

    // Allow fallback admin user
    if ("admin".equals(userId)) {
      log.debug("Access granted for fallback admin");
      return true;
    }

    Optional<UserEntity> user = userService.getUserById(userId);
    log.debug("User found in DB: {}", user.isPresent());
    return user.isPresent() && "ADMIN".equals(user.get().getRole());
  }

  private void requireAdmin(String token) {
    if (!isAdmin(token)) {
      throw new SecurityException("Access denied");
    }
  }

  private String getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token != null && token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return null;
  }

  @GetMapping("/users")
  public ResponseEntity<?> listUsers(
      @RequestHeader("Authorization") String token,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    requireAdmin(token);
    Page<UserEntity> users = userService.getUsers(role, PageRequest.of(page, size));
    return ResponseEntity.ok(users);
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<?> deleteUser(
      @RequestHeader("Authorization") String token, @PathVariable String id) {
    requireAdmin(token);
    userService.deleteUser(id);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/users/{id}/status")
  public ResponseEntity<?> updateUserStatus(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      @RequestBody Map<String, String> payload) {
    requireAdmin(token);
    String status = payload.get("status");
    UserEntity user = userService.updateUserStatus(id, status);
    return ResponseEntity.ok(user);
  }

  @PutMapping("/users/{id}/role")
  public ResponseEntity<?> updateUserRole(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      @RequestBody Map<String, String> payload) {
    requireAdmin(token);
    String role = payload.get("role");
    UserEntity user = userService.updateUserRole(id, role);
    return ResponseEntity.ok(user);
  }

  @GetMapping("/system/status")
  public ResponseEntity<?> getSystemStatus(@RequestHeader("Authorization") String token) {
    requireAdmin(token);
    boolean enabled = systemConfigService.isSystemEnabled();
    return ResponseEntity.ok(Map.of("enabled", enabled));
  }

  @PutMapping("/system/status")
  public ResponseEntity<?> setSystemStatus(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, Boolean> payload) {
    requireAdmin(token);
    Boolean enabled = payload.get("enabled");
    systemConfigService.setSystemEnabled(enabled);
    return ResponseEntity.ok(Map.of("enabled", enabled));
  }

  // ==================== 系统设置 API ====================

  @GetMapping("/system/settings")
  public ResponseEntity<?> getSystemSettings(@RequestHeader("Authorization") String token) {
    requireAdmin(token);
    Map<String, Object> settings = systemConfigService.getAllSettings();
    return ResponseEntity.ok(settings);
  }

  @PutMapping("/system/settings")
  public ResponseEntity<?> updateSystemSettings(
      @RequestHeader("Authorization") String token,
      @Valid @RequestBody SystemSettingsUpdateRequest settings,
      HttpServletRequest request) {
    requireAdmin(token);
    systemConfigService.updateAllSettings(settings.toSettingsMap());
    ollamaStartupService.syncOllamaWithAiSetting();

    String userId = getUserIdFromToken(token);
    operationLogService.log(userId, "UPDATE", "系统设置", null, "更新系统设置", request);

    return ResponseEntity.ok(systemConfigService.getAllSettings());
  }

  @GetMapping("/ai/models")
  public ResponseEntity<?> getAiModels(@RequestHeader("Authorization") String token) {
    requireAdmin(token);
    String currentModel = systemConfigService.getConfig(SystemConfigService.AI_MODEL);
    List<Map<String, Object>> modelItems = ollamaAiService.getModelCatalog(currentModel);

    List<String> installedModels =
        modelItems.stream()
            .filter(item -> Boolean.TRUE.equals(item.get("installed")))
            .map(item -> String.valueOf(item.get("name")))
            .toList();

    Map<String, Object> result = new HashMap<>();
    result.put("models", installedModels);
    result.put("modelItems", modelItems);
    result.put("recommendedModels", ollamaAiService.getRecommendedModels(currentModel));
    result.put("currentModel", currentModel);
    return ResponseEntity.ok(result);
  }

  @PutMapping("/ai/model")
  public ResponseEntity<?> setAiModel(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    requireAdmin(token);
    String model = payload.get("model");
    if (model == null || model.isBlank()) {
      throw new IllegalArgumentException("model 不能为空");
    }
    systemConfigService.setConfig(SystemConfigService.AI_MODEL, model.trim());
    return ResponseEntity.ok(Map.of("model", model.trim()));
  }

  @PostMapping("/ai/model/pull")
  public ResponseEntity<?> pullAiModel(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) {
    requireAdmin(token);
    String model = payload.get("model");
    if (model == null || model.isBlank()) {
      throw new IllegalArgumentException("model 不能为空");
    }
    Map<String, Object> result = ollamaAiService.pullModel(model.trim());
    return ResponseEntity.ok(result);
  }

  @PostMapping(value = "/ai/model/pull/progress", produces = "application/x-ndjson")
  public ResponseEntity<StreamingResponseBody> pullAiModelWithProgress(
      @RequestHeader("Authorization") String token, @RequestBody Map<String, Object> payload) {
    requireAdmin(token);

    String model = String.valueOf(payload.getOrDefault("model", "")).trim();
    boolean autoApply =
        Boolean.parseBoolean(String.valueOf(payload.getOrDefault("autoApply", true)));

    if (model.isBlank()) {
      throw new IllegalArgumentException("model 不能为空");
    }

    StreamingResponseBody stream =
        outputStream -> {
          var writer =
              new java.io.BufferedWriter(
                  new java.io.OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
          try {
            ollamaAiService.pullModelStreaming(
                model,
                progress -> {
                  try {
                    writer.write(objectMapper.writeValueAsString(progress));
                    writer.newLine();
                    writer.flush();
                  } catch (Exception ignore) {
                    // ignore push failures
                  }
                });

            if (autoApply) {
              systemConfigService.setConfig(SystemConfigService.AI_MODEL, model);
            }

            Map<String, Object> done = new HashMap<>();
            done.put("type", "done");
            done.put("model", model);
            done.put("autoApplied", autoApply);
            writer.write(objectMapper.writeValueAsString(done));
            writer.newLine();
            writer.flush();
          } catch (Exception ex) {
            Map<String, Object> error = new HashMap<>();
            error.put("type", "error");
            error.put("model", model);
            error.put("message", ex.getMessage());
            writer.write(objectMapper.writeValueAsString(error));
            writer.newLine();
            writer.flush();
          }
        };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/x-ndjson"))
        .body(stream);
  }

  // ==================== 操作日志 API ====================

  @GetMapping("/logs")
  public ResponseEntity<?> getLogs(
      @RequestHeader("Authorization") String token,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String action,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    requireAdmin(token);
    Page<OperationLogEntity> logs = operationLogService.searchLogs(keyword, action, page, size);
    return ResponseEntity.ok(logs);
  }

  @GetMapping("/logs/export")
  public ResponseEntity<?> exportLogs(
      @RequestHeader("Authorization") String token,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String action,
      HttpServletRequest request) {
    requireAdmin(token);

    Page<OperationLogEntity> page = operationLogService.searchLogs(keyword, action, 0, 5000);
    StringBuilder csv = new StringBuilder();
    csv.append("时间,用户,用户ID,操作类型,目标,IP,详情\n");

    for (OperationLogEntity log : page.getContent()) {
      csv.append(csvField(String.valueOf(log.getTimestamp())))
          .append(',')
          .append(csvField(log.getUsername()))
          .append(',')
          .append(csvField(log.getUserId()))
          .append(',')
          .append(csvField(log.getAction()))
          .append(',')
          .append(csvField(log.getTarget()))
          .append(',')
          .append(csvField(log.getIp()))
          .append(',')
          .append(csvField(log.getDetails()))
          .append('\n');
    }

    String userId = getUserIdFromToken(token);
    operationLogService.log(userId, "EXPORT", "操作日志", null, "导出操作日志", request);

    byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=operation_logs.csv")
        .body(bytes);
  }

  private String csvField(String value) {
    if (value == null) {
      return "\"\"";
    }
    String escaped = value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    return "\"" + escaped + "\"";
  }

  // ==================== 数据统计 API ====================

  @GetMapping("/statistics")
  public ResponseEntity<?> getStatistics(
      @RequestHeader("Authorization") String token,
      @RequestParam(defaultValue = "30d") String range) {
    requireAdmin(token);
    Map<String, Object> stats = statisticsService.getOverviewStats(range);
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/statistics/trends")
  public ResponseEntity<?> getStatisticsTrends(
      @RequestHeader("Authorization") String token,
      @RequestParam(defaultValue = "30d") String range) {
    requireAdmin(token);
    Map<String, Object> trends = new HashMap<>();
    trends.put("userTrend", statisticsService.getUserActivityTrend(range));
    trends.put("questionTrend", statisticsService.getQuestionGrowthTrend(range));
    trends.put("examTrend", statisticsService.getExamTrend(range));
    return ResponseEntity.ok(trends);
  }

  // ==================== 系统监控 API ====================

  @GetMapping("/monitor")
  public ResponseEntity<?> getSystemMonitor(@RequestHeader("Authorization") String token) {
    requireAdmin(token);
    Map<String, Object> monitorData = new HashMap<>();
    monitorData.put("metrics", systemMonitorService.getSystemMetrics());
    monitorData.put("services", systemMonitorService.getServicesStatus());
    return ResponseEntity.ok(monitorData);
  }

  // ==================== 公告管理 API ====================

  @GetMapping("/announcements")
  public ResponseEntity<?> getAnnouncements(
      @RequestHeader("Authorization") String token,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    requireAdmin(token);
    Page<AnnouncementEntity> announcements =
        announcementService.getAnnouncements(status, page, size);
    return ResponseEntity.ok(announcements);
  }

  @PostMapping("/announcements")
  public ResponseEntity<?> createAnnouncement(
      @RequestHeader("Authorization") String token,
      @RequestBody Map<String, String> payload,
      HttpServletRequest request) {
    requireAdmin(token);
    String userId = getUserIdFromToken(token);
    AnnouncementEntity announcement =
        announcementService.createAnnouncement(
            payload.get("title"), payload.get("content"), payload.get("priority"), userId);

    // 如果指定发布，则立即发布
    if ("PUBLISHED".equals(payload.get("status"))) {
      announcement = announcementService.publishAnnouncement(announcement.getId());
    }

    // 记录操作日志
    operationLogService.log(
        userId, "CREATE", "公告", announcement.getId(), "创建公告: " + announcement.getTitle(), request);

    return ResponseEntity.ok(announcement);
  }

  @PutMapping("/announcements/{id}")
  public ResponseEntity<?> updateAnnouncement(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      @RequestBody Map<String, String> payload,
      HttpServletRequest request) {
    requireAdmin(token);
    AnnouncementEntity announcement =
        announcementService.updateAnnouncement(
            id, payload.get("title"), payload.get("content"), payload.get("priority"));

    // 如果指定发布，则发布
    if ("PUBLISHED".equals(payload.get("status"))) {
      announcement = announcementService.publishAnnouncement(id);
    }

    String userId = getUserIdFromToken(token);
    operationLogService.log(
        userId, "UPDATE", "公告", id, "更新公告: " + announcement.getTitle(), request);

    return ResponseEntity.ok(announcement);
  }

  @PutMapping("/announcements/{id}/publish")
  public ResponseEntity<?> publishAnnouncement(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      HttpServletRequest request) {
    requireAdmin(token);
    AnnouncementEntity announcement = announcementService.publishAnnouncement(id);

    String userId = getUserIdFromToken(token);
    operationLogService.log(
        userId, "UPDATE", "公告", id, "发布公告: " + announcement.getTitle(), request);

    return ResponseEntity.ok(announcement);
  }

  @PutMapping("/announcements/{id}/archive")
  public ResponseEntity<?> archiveAnnouncement(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      HttpServletRequest request) {
    requireAdmin(token);
    AnnouncementEntity announcement = announcementService.archiveAnnouncement(id);

    String userId = getUserIdFromToken(token);
    operationLogService.log(
        userId, "UPDATE", "公告", id, "归档公告: " + announcement.getTitle(), request);

    return ResponseEntity.ok(announcement);
  }

  @DeleteMapping("/announcements/{id}")
  public ResponseEntity<?> deleteAnnouncement(
      @RequestHeader("Authorization") String token,
      @PathVariable String id,
      HttpServletRequest request) {
    requireAdmin(token);

    Optional<AnnouncementEntity> optional = announcementService.getAnnouncementById(id);
    String title = optional.map(AnnouncementEntity::getTitle).orElse("未知");

    announcementService.deleteAnnouncement(id);

    String userId = getUserIdFromToken(token);
    operationLogService.log(userId, "DELETE", "公告", id, "删除公告: " + title, request);

    return ResponseEntity.ok().build();
  }

  // ==================== 公告前台展示 API（无需登录） ====================

  @GetMapping("/announcements/public")
  public ResponseEntity<?> getPublicAnnouncements() {
    List<AnnouncementEntity> announcements = announcementService.getPublishedAnnouncements();
    return ResponseEntity.ok(announcements);
  }
}

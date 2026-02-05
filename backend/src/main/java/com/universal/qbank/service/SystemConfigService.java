package com.universal.qbank.service;

import com.universal.qbank.entity.SystemConfigEntity;
import com.universal.qbank.repository.SystemConfigRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemConfigService {

  @Autowired private SystemConfigRepository systemConfigRepository;

  // 配置键常量
  public static final String SYSTEM_ENABLED = "SYSTEM_ENABLED";
  public static final String MAINTENANCE_MODE = "MAINTENANCE_MODE";
  public static final String MAINTENANCE_MESSAGE = "MAINTENANCE_MESSAGE";
  public static final String ALLOW_REGISTRATION = "ALLOW_REGISTRATION";
  public static final String REQUIRE_EMAIL_VERIFICATION = "REQUIRE_EMAIL_VERIFICATION";
  public static final String SESSION_TIMEOUT = "SESSION_TIMEOUT";
  public static final String MAX_LOGIN_ATTEMPTS = "MAX_LOGIN_ATTEMPTS";
  public static final String PASSWORD_MIN_LENGTH = "PASSWORD_MIN_LENGTH";
  public static final String ALLOW_PASSWORD_RESET = "ALLOW_PASSWORD_RESET";
  public static final String MAX_FILE_SIZE = "MAX_FILE_SIZE";
  public static final String ALLOWED_FILE_TYPES = "ALLOWED_FILE_TYPES";
  public static final String EXAM_AUTO_SAVE_INTERVAL = "EXAM_AUTO_SAVE_INTERVAL";
  public static final String SHOW_LEADERBOARD = "SHOW_LEADERBOARD";
  public static final String ENABLE_NOTIFICATIONS = "ENABLE_NOTIFICATIONS";
  public static final String SYSTEM_EMAIL = "SYSTEM_EMAIL";
  public static final String SITE_NAME = "SITE_NAME";
  public static final String SITE_DESCRIPTION = "SITE_DESCRIPTION";

  // 默认值
  private static final Map<String, String> DEFAULTS = new HashMap<>();
  static {
    DEFAULTS.put(SYSTEM_ENABLED, "true");
    DEFAULTS.put(MAINTENANCE_MODE, "false");
    DEFAULTS.put(MAINTENANCE_MESSAGE, "系统正在维护中，请稍后再试...");
    DEFAULTS.put(ALLOW_REGISTRATION, "true");
    DEFAULTS.put(REQUIRE_EMAIL_VERIFICATION, "false");
    DEFAULTS.put(SESSION_TIMEOUT, "30");
    DEFAULTS.put(MAX_LOGIN_ATTEMPTS, "5");
    DEFAULTS.put(PASSWORD_MIN_LENGTH, "6");
    DEFAULTS.put(ALLOW_PASSWORD_RESET, "true");
    DEFAULTS.put(MAX_FILE_SIZE, "10");
    DEFAULTS.put(ALLOWED_FILE_TYPES, "jpg,jpeg,png,gif,pdf,doc,docx");
    DEFAULTS.put(EXAM_AUTO_SAVE_INTERVAL, "60");
    DEFAULTS.put(SHOW_LEADERBOARD, "true");
    DEFAULTS.put(ENABLE_NOTIFICATIONS, "true");
    DEFAULTS.put(SYSTEM_EMAIL, "admin@example.com");
    DEFAULTS.put(SITE_NAME, "UQBank 题库系统");
    DEFAULTS.put(SITE_DESCRIPTION, "通用题库与组卷系统");
  }

  public boolean isSystemEnabled() {
    return getBooleanConfig(SYSTEM_ENABLED, true);
  }

  public void setSystemEnabled(boolean enabled) {
    setConfig(SYSTEM_ENABLED, String.valueOf(enabled));
  }

  // 获取单个配置
  public String getConfig(String key) {
    return systemConfigRepository
        .findById(key)
        .map(SystemConfigEntity::getConfigValue)
        .orElse(DEFAULTS.get(key));
  }

  public boolean getBooleanConfig(String key, boolean defaultValue) {
    return systemConfigRepository
        .findById(key)
        .map(config -> Boolean.parseBoolean(config.getConfigValue()))
        .orElse(defaultValue);
  }

  public int getIntConfig(String key, int defaultValue) {
    return systemConfigRepository
        .findById(key)
        .map(config -> {
          try {
            return Integer.parseInt(config.getConfigValue());
          } catch (NumberFormatException e) {
            return defaultValue;
          }
        })
        .orElse(defaultValue);
  }

  // 设置单个配置
  public void setConfig(String key, String value) {
    SystemConfigEntity config = new SystemConfigEntity(key, value);
    systemConfigRepository.save(config);
  }

  // 获取所有系统设置
  public Map<String, Object> getAllSettings() {
    Map<String, Object> settings = new HashMap<>();
    settings.put("systemEnabled", getBooleanConfig(SYSTEM_ENABLED, true));
    settings.put("maintenanceMode", getBooleanConfig(MAINTENANCE_MODE, false));
    settings.put("maintenanceMessage", getConfig(MAINTENANCE_MESSAGE));
    settings.put("allowRegistration", getBooleanConfig(ALLOW_REGISTRATION, true));
    settings.put("requireEmailVerification", getBooleanConfig(REQUIRE_EMAIL_VERIFICATION, false));
    settings.put("sessionTimeout", getIntConfig(SESSION_TIMEOUT, 30));
    settings.put("maxLoginAttempts", getIntConfig(MAX_LOGIN_ATTEMPTS, 5));
    settings.put("passwordMinLength", getIntConfig(PASSWORD_MIN_LENGTH, 6));
    settings.put("allowPasswordReset", getBooleanConfig(ALLOW_PASSWORD_RESET, true));
    settings.put("maxFileSize", getIntConfig(MAX_FILE_SIZE, 10));
    settings.put("allowedFileTypes", getConfig(ALLOWED_FILE_TYPES));
    settings.put("examAutoSaveInterval", getIntConfig(EXAM_AUTO_SAVE_INTERVAL, 60));
    settings.put("showLeaderboard", getBooleanConfig(SHOW_LEADERBOARD, true));
    settings.put("enableNotifications", getBooleanConfig(ENABLE_NOTIFICATIONS, true));
    settings.put("systemEmail", getConfig(SYSTEM_EMAIL));
    settings.put("siteName", getConfig(SITE_NAME));
    settings.put("siteDescription", getConfig(SITE_DESCRIPTION));
    return settings;
  }

  // 批量更新设置
  public void updateAllSettings(Map<String, Object> settings) {
    if (settings.containsKey("systemEnabled")) {
      setConfig(SYSTEM_ENABLED, String.valueOf(settings.get("systemEnabled")));
    }
    if (settings.containsKey("maintenanceMode")) {
      setConfig(MAINTENANCE_MODE, String.valueOf(settings.get("maintenanceMode")));
    }
    if (settings.containsKey("maintenanceMessage")) {
      setConfig(MAINTENANCE_MESSAGE, String.valueOf(settings.get("maintenanceMessage")));
    }
    if (settings.containsKey("allowRegistration")) {
      setConfig(ALLOW_REGISTRATION, String.valueOf(settings.get("allowRegistration")));
    }
    if (settings.containsKey("requireEmailVerification")) {
      setConfig(REQUIRE_EMAIL_VERIFICATION, String.valueOf(settings.get("requireEmailVerification")));
    }
    if (settings.containsKey("sessionTimeout")) {
      setConfig(SESSION_TIMEOUT, String.valueOf(settings.get("sessionTimeout")));
    }
    if (settings.containsKey("maxLoginAttempts")) {
      setConfig(MAX_LOGIN_ATTEMPTS, String.valueOf(settings.get("maxLoginAttempts")));
    }
    if (settings.containsKey("passwordMinLength")) {
      setConfig(PASSWORD_MIN_LENGTH, String.valueOf(settings.get("passwordMinLength")));
    }
    if (settings.containsKey("allowPasswordReset")) {
      setConfig(ALLOW_PASSWORD_RESET, String.valueOf(settings.get("allowPasswordReset")));
    }
    if (settings.containsKey("maxFileSize")) {
      setConfig(MAX_FILE_SIZE, String.valueOf(settings.get("maxFileSize")));
    }
    if (settings.containsKey("allowedFileTypes")) {
      setConfig(ALLOWED_FILE_TYPES, String.valueOf(settings.get("allowedFileTypes")));
    }
    if (settings.containsKey("examAutoSaveInterval")) {
      setConfig(EXAM_AUTO_SAVE_INTERVAL, String.valueOf(settings.get("examAutoSaveInterval")));
    }
    if (settings.containsKey("showLeaderboard")) {
      setConfig(SHOW_LEADERBOARD, String.valueOf(settings.get("showLeaderboard")));
    }
    if (settings.containsKey("enableNotifications")) {
      setConfig(ENABLE_NOTIFICATIONS, String.valueOf(settings.get("enableNotifications")));
    }
    if (settings.containsKey("systemEmail")) {
      setConfig(SYSTEM_EMAIL, String.valueOf(settings.get("systemEmail")));
    }
    if (settings.containsKey("siteName")) {
      setConfig(SITE_NAME, String.valueOf(settings.get("siteName")));
    }
    if (settings.containsKey("siteDescription")) {
      setConfig(SITE_DESCRIPTION, String.valueOf(settings.get("siteDescription")));
    }
  }
}

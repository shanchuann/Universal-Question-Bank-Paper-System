package com.universal.qbank.service;

import com.universal.qbank.entity.SystemConfigEntity;
import com.universal.qbank.repository.SystemConfigRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemConfigService {

  private final SystemConfigRepository systemConfigRepository;

  // 配置键常量
  public static final String SYSTEM_ENABLED = SystemConfigKey.SYSTEM_ENABLED.key();
  public static final String MAINTENANCE_MODE = SystemConfigKey.MAINTENANCE_MODE.key();
  public static final String MAINTENANCE_MESSAGE = SystemConfigKey.MAINTENANCE_MESSAGE.key();
  public static final String ALLOW_REGISTRATION = SystemConfigKey.ALLOW_REGISTRATION.key();
  public static final String REQUIRE_EMAIL_VERIFICATION =
      SystemConfigKey.REQUIRE_EMAIL_VERIFICATION.key();
  public static final String SESSION_TIMEOUT = SystemConfigKey.SESSION_TIMEOUT.key();
  public static final String MAX_LOGIN_ATTEMPTS = SystemConfigKey.MAX_LOGIN_ATTEMPTS.key();
  public static final String PASSWORD_MIN_LENGTH = SystemConfigKey.PASSWORD_MIN_LENGTH.key();
  public static final String ALLOW_PASSWORD_RESET = SystemConfigKey.ALLOW_PASSWORD_RESET.key();
  public static final String MAX_FILE_SIZE = SystemConfigKey.MAX_FILE_SIZE.key();
  public static final String ALLOWED_FILE_TYPES = SystemConfigKey.ALLOWED_FILE_TYPES.key();
  public static final String EXAM_AUTO_SAVE_INTERVAL =
      SystemConfigKey.EXAM_AUTO_SAVE_INTERVAL.key();
  public static final String SHOW_LEADERBOARD = SystemConfigKey.SHOW_LEADERBOARD.key();
  public static final String ENABLE_NOTIFICATIONS = SystemConfigKey.ENABLE_NOTIFICATIONS.key();
  public static final String AI_ENABLED = SystemConfigKey.AI_ENABLED.key();
  public static final String AI_ASSISTANT_ENABLED = SystemConfigKey.AI_ASSISTANT_ENABLED.key();
  public static final String AI_AUTO_GRADING_ENABLED =
      SystemConfigKey.AI_AUTO_GRADING_ENABLED.key();
  public static final String AI_MODEL = SystemConfigKey.AI_MODEL.key();
  public static final String SYSTEM_EMAIL = SystemConfigKey.SYSTEM_EMAIL.key();
  public static final String SITE_NAME = SystemConfigKey.SITE_NAME.key();
  public static final String SITE_DESCRIPTION = SystemConfigKey.SITE_DESCRIPTION.key();
  public static final String SITE_LOGO_URL = SystemConfigKey.SITE_LOGO_URL.key();
  public static final String COPYRIGHT_TEXT = SystemConfigKey.COPYRIGHT_TEXT.key();

  // 默认值
  private static final Map<String, String> DEFAULTS;

  static {
    Map<String, String> defaults = new HashMap<>();
    for (SystemConfigKey key : SystemConfigKey.values()) {
      defaults.put(key.key(), key.defaultValue());
    }
    DEFAULTS = Collections.unmodifiableMap(defaults);
  }

  public SystemConfigService(SystemConfigRepository systemConfigRepository) {
    this.systemConfigRepository = systemConfigRepository;
  }

  public boolean isSystemEnabled() {
    return getBooleanConfig(SystemConfigKey.SYSTEM_ENABLED);
  }

  public void setSystemEnabled(boolean enabled) {
    setConfig(SystemConfigKey.SYSTEM_ENABLED, String.valueOf(enabled));
  }

  public String getConfig(SystemConfigKey key) {
    return getConfig(key.key());
  }

  public boolean getBooleanConfig(SystemConfigKey key) {
    return getBooleanConfig(key.key(), Boolean.parseBoolean(key.defaultValue()));
  }

  public int getIntConfig(SystemConfigKey key) {
    return getIntConfig(key.key(), parseIntOrDefault(key.defaultValue(), 0));
  }

  public void setConfig(SystemConfigKey key, String value) {
    setConfig(key.key(), value);
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
        .map(
            config -> {
              try {
                return Integer.parseInt(config.getConfigValue());
              } catch (NumberFormatException e) {
                return defaultValue;
              }
            })
        .orElse(defaultValue);
  }

  private int parseIntOrDefault(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception ignored) {
      return defaultValue;
    }
  }

  // 设置单个配置
  public void setConfig(String key, String value) {
    SystemConfigEntity config = new SystemConfigEntity(key, value);
    systemConfigRepository.save(config);
  }

  // 获取所有系统设置
  public Map<String, Object> getAllSettings() {
    Map<String, Object> settings = new HashMap<>();
    settings.put("systemEnabled", getBooleanConfig(SystemConfigKey.SYSTEM_ENABLED));
    settings.put("maintenanceMode", getBooleanConfig(SystemConfigKey.MAINTENANCE_MODE));
    settings.put("maintenanceMessage", getConfig(SystemConfigKey.MAINTENANCE_MESSAGE));
    settings.put("allowRegistration", getBooleanConfig(SystemConfigKey.ALLOW_REGISTRATION));
    settings.put(
        "requireEmailVerification", getBooleanConfig(SystemConfigKey.REQUIRE_EMAIL_VERIFICATION));
    settings.put("sessionTimeout", getIntConfig(SystemConfigKey.SESSION_TIMEOUT));
    settings.put("maxLoginAttempts", getIntConfig(SystemConfigKey.MAX_LOGIN_ATTEMPTS));
    settings.put("passwordMinLength", getIntConfig(SystemConfigKey.PASSWORD_MIN_LENGTH));
    settings.put("allowPasswordReset", getBooleanConfig(SystemConfigKey.ALLOW_PASSWORD_RESET));
    settings.put("maxFileSize", getIntConfig(SystemConfigKey.MAX_FILE_SIZE));
    settings.put("allowedFileTypes", getConfig(SystemConfigKey.ALLOWED_FILE_TYPES));
    settings.put("examAutoSaveInterval", getIntConfig(SystemConfigKey.EXAM_AUTO_SAVE_INTERVAL));
    settings.put("showLeaderboard", getBooleanConfig(SystemConfigKey.SHOW_LEADERBOARD));
    settings.put("enableNotifications", getBooleanConfig(SystemConfigKey.ENABLE_NOTIFICATIONS));
    settings.put("aiEnabled", getBooleanConfig(SystemConfigKey.AI_ENABLED));
    settings.put("aiAssistantEnabled", getBooleanConfig(SystemConfigKey.AI_ASSISTANT_ENABLED));
    settings.put("aiAutoGradingEnabled", getBooleanConfig(SystemConfigKey.AI_AUTO_GRADING_ENABLED));
    settings.put("aiModel", getConfig(SystemConfigKey.AI_MODEL));
    settings.put("systemEmail", getConfig(SystemConfigKey.SYSTEM_EMAIL));
    settings.put("siteName", getConfig(SystemConfigKey.SITE_NAME));
    settings.put("siteDescription", getConfig(SystemConfigKey.SITE_DESCRIPTION));
    settings.put("siteLogoUrl", getConfig(SystemConfigKey.SITE_LOGO_URL));
    settings.put("copyrightText", getConfig(SystemConfigKey.COPYRIGHT_TEXT));
    return settings;
  }

  public Map<String, Object> getPublicSettings() {
    Map<String, Object> settings = new HashMap<>();
    settings.put("siteName", getConfig(SystemConfigKey.SITE_NAME));
    settings.put("siteDescription", getConfig(SystemConfigKey.SITE_DESCRIPTION));
    settings.put("siteLogoUrl", getConfig(SystemConfigKey.SITE_LOGO_URL));
    settings.put("copyrightText", getConfig(SystemConfigKey.COPYRIGHT_TEXT));
    settings.put("maintenanceMode", getBooleanConfig(SystemConfigKey.MAINTENANCE_MODE));
    settings.put("maintenanceMessage", getConfig(SystemConfigKey.MAINTENANCE_MESSAGE));
    settings.put("aiEnabled", getBooleanConfig(SystemConfigKey.AI_ENABLED));
    settings.put("aiAssistantEnabled", getBooleanConfig(SystemConfigKey.AI_ASSISTANT_ENABLED));
    return settings;
  }

  private void updateIfPresent(Map<String, Object> settings, String field, SystemConfigKey key) {
    if (settings.containsKey(field)) {
      setConfig(key, String.valueOf(settings.get(field)));
    }
  }

  // 批量更新设置
  public void updateAllSettings(Map<String, Object> settings) {
    updateIfPresent(settings, "systemEnabled", SystemConfigKey.SYSTEM_ENABLED);
    updateIfPresent(settings, "maintenanceMode", SystemConfigKey.MAINTENANCE_MODE);
    updateIfPresent(settings, "maintenanceMessage", SystemConfigKey.MAINTENANCE_MESSAGE);
    updateIfPresent(settings, "allowRegistration", SystemConfigKey.ALLOW_REGISTRATION);
    updateIfPresent(
        settings, "requireEmailVerification", SystemConfigKey.REQUIRE_EMAIL_VERIFICATION);
    updateIfPresent(settings, "sessionTimeout", SystemConfigKey.SESSION_TIMEOUT);
    updateIfPresent(settings, "maxLoginAttempts", SystemConfigKey.MAX_LOGIN_ATTEMPTS);
    updateIfPresent(settings, "passwordMinLength", SystemConfigKey.PASSWORD_MIN_LENGTH);
    updateIfPresent(settings, "allowPasswordReset", SystemConfigKey.ALLOW_PASSWORD_RESET);
    updateIfPresent(settings, "maxFileSize", SystemConfigKey.MAX_FILE_SIZE);
    updateIfPresent(settings, "allowedFileTypes", SystemConfigKey.ALLOWED_FILE_TYPES);
    updateIfPresent(settings, "examAutoSaveInterval", SystemConfigKey.EXAM_AUTO_SAVE_INTERVAL);
    updateIfPresent(settings, "showLeaderboard", SystemConfigKey.SHOW_LEADERBOARD);
    updateIfPresent(settings, "enableNotifications", SystemConfigKey.ENABLE_NOTIFICATIONS);
    updateIfPresent(settings, "aiEnabled", SystemConfigKey.AI_ENABLED);
    updateIfPresent(settings, "aiAssistantEnabled", SystemConfigKey.AI_ASSISTANT_ENABLED);
    updateIfPresent(settings, "aiAutoGradingEnabled", SystemConfigKey.AI_AUTO_GRADING_ENABLED);
    updateIfPresent(settings, "aiModel", SystemConfigKey.AI_MODEL);
    updateIfPresent(settings, "systemEmail", SystemConfigKey.SYSTEM_EMAIL);
    updateIfPresent(settings, "siteName", SystemConfigKey.SITE_NAME);
    updateIfPresent(settings, "siteDescription", SystemConfigKey.SITE_DESCRIPTION);
    updateIfPresent(settings, "siteLogoUrl", SystemConfigKey.SITE_LOGO_URL);
    updateIfPresent(settings, "copyrightText", SystemConfigKey.COPYRIGHT_TEXT);
  }
}

package com.universal.qbank.service;

/** 系统配置键与默认值定义。 */
public enum SystemConfigKey {
  SYSTEM_ENABLED("SYSTEM_ENABLED", "true"),
  MAINTENANCE_MODE("MAINTENANCE_MODE", "false"),
  MAINTENANCE_MESSAGE("MAINTENANCE_MESSAGE", "系统正在维护中，请稍后再试..."),
  ALLOW_REGISTRATION("ALLOW_REGISTRATION", "true"),
  REQUIRE_EMAIL_VERIFICATION("REQUIRE_EMAIL_VERIFICATION", "false"),
  SESSION_TIMEOUT("SESSION_TIMEOUT", "30"),
  MAX_LOGIN_ATTEMPTS("MAX_LOGIN_ATTEMPTS", "5"),
  PASSWORD_MIN_LENGTH("PASSWORD_MIN_LENGTH", "6"),
  ALLOW_PASSWORD_RESET("ALLOW_PASSWORD_RESET", "true"),
  MAX_FILE_SIZE("MAX_FILE_SIZE", "10"),
  ALLOWED_FILE_TYPES("ALLOWED_FILE_TYPES", "jpg,jpeg,png,gif,pdf,doc,docx"),
  EXAM_AUTO_SAVE_INTERVAL("EXAM_AUTO_SAVE_INTERVAL", "60"),
  SHOW_LEADERBOARD("SHOW_LEADERBOARD", "true"),
  ENABLE_NOTIFICATIONS("ENABLE_NOTIFICATIONS", "true"),
  AI_ENABLED("AI_ENABLED", "false"),
  AI_ASSISTANT_ENABLED("AI_ASSISTANT_ENABLED", "true"),
  AI_AUTO_GRADING_ENABLED("AI_AUTO_GRADING_ENABLED", "false"),
  AI_MODEL("AI_MODEL", "gemma4"),
  SYSTEM_EMAIL("SYSTEM_EMAIL", "admin@example.com"),
  SITE_NAME("SITE_NAME", "UQBank 题库系统"),
  SITE_DESCRIPTION("SITE_DESCRIPTION", "通用题库与组卷系统"),
  SITE_LOGO_URL("SITE_LOGO_URL", ""),
  COPYRIGHT_TEXT("COPYRIGHT_TEXT", "© UQBank 题库系统");

  private final String key;
  private final String defaultValue;

  SystemConfigKey(String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public String key() {
    return key;
  }

  public String defaultValue() {
    return defaultValue;
  }
}

package com.universal.qbank.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class SystemSettingsUpdateRequest {

  private Boolean systemEnabled;
  private Boolean maintenanceMode;

  @Size(max = 200, message = "maintenanceMessage 长度不能超过 200")
  private String maintenanceMessage;

  private Boolean allowRegistration;
  private Boolean requireEmailVerification;

  @Min(value = 5, message = "sessionTimeout 不能小于 5")
  @Max(value = 1440, message = "sessionTimeout 不能大于 1440")
  private Integer sessionTimeout;

  @Min(value = 1, message = "maxLoginAttempts 不能小于 1")
  @Max(value = 20, message = "maxLoginAttempts 不能大于 20")
  private Integer maxLoginAttempts;

  @Min(value = 6, message = "passwordMinLength 不能小于 6")
  @Max(value = 64, message = "passwordMinLength 不能大于 64")
  private Integer passwordMinLength;

  private Boolean allowPasswordReset;

  @Min(value = 1, message = "maxFileSize 不能小于 1")
  @Max(value = 100, message = "maxFileSize 不能大于 100")
  private Integer maxFileSize;

  @Size(max = 200, message = "allowedFileTypes 长度不能超过 200")
  private String allowedFileTypes;

  @Min(value = 10, message = "examAutoSaveInterval 不能小于 10")
  @Max(value = 3600, message = "examAutoSaveInterval 不能大于 3600")
  private Integer examAutoSaveInterval;

  private Boolean showLeaderboard;
  private Boolean enableNotifications;
  private Boolean aiEnabled;
  private Boolean aiAssistantEnabled;
  private Boolean aiAutoGradingEnabled;

  @Size(max = 64, message = "aiModel 长度不能超过 64")
  private String aiModel;

  @Email(message = "systemEmail 格式不正确")
  @Size(max = 120, message = "systemEmail 长度不能超过 120")
  private String systemEmail;

  @Size(max = 120, message = "siteName 长度不能超过 120")
  private String siteName;

  @Size(max = 300, message = "siteDescription 长度不能超过 300")
  private String siteDescription;

  @Size(max = 500, message = "siteLogoUrl 长度不能超过 500")
  private String siteLogoUrl;

  @Size(max = 120, message = "copyrightText 长度不能超过 120")
  private String copyrightText;

  public Map<String, Object> toSettingsMap() {
    Map<String, Object> settings = new LinkedHashMap<>();
    putIfNotNull(settings, "systemEnabled", systemEnabled);
    putIfNotNull(settings, "maintenanceMode", maintenanceMode);
    putIfNotNull(settings, "maintenanceMessage", maintenanceMessage);
    putIfNotNull(settings, "allowRegistration", allowRegistration);
    putIfNotNull(settings, "requireEmailVerification", requireEmailVerification);
    putIfNotNull(settings, "sessionTimeout", sessionTimeout);
    putIfNotNull(settings, "maxLoginAttempts", maxLoginAttempts);
    putIfNotNull(settings, "passwordMinLength", passwordMinLength);
    putIfNotNull(settings, "allowPasswordReset", allowPasswordReset);
    putIfNotNull(settings, "maxFileSize", maxFileSize);
    putIfNotNull(settings, "allowedFileTypes", allowedFileTypes);
    putIfNotNull(settings, "examAutoSaveInterval", examAutoSaveInterval);
    putIfNotNull(settings, "showLeaderboard", showLeaderboard);
    putIfNotNull(settings, "enableNotifications", enableNotifications);
    putIfNotNull(settings, "aiEnabled", aiEnabled);
    putIfNotNull(settings, "aiAssistantEnabled", aiAssistantEnabled);
    putIfNotNull(settings, "aiAutoGradingEnabled", aiAutoGradingEnabled);
    putIfNotNull(settings, "aiModel", aiModel);
    putIfNotNull(settings, "systemEmail", systemEmail);
    putIfNotNull(settings, "siteName", siteName);
    putIfNotNull(settings, "siteDescription", siteDescription);
    putIfNotNull(settings, "siteLogoUrl", siteLogoUrl);
    putIfNotNull(settings, "copyrightText", copyrightText);
    return settings;
  }

  private void putIfNotNull(Map<String, Object> settings, String key, Object value) {
    if (value != null) {
      settings.put(key, value);
    }
  }

  public Boolean getSystemEnabled() {
    return systemEnabled;
  }

  public void setSystemEnabled(Boolean systemEnabled) {
    this.systemEnabled = systemEnabled;
  }

  public Boolean getMaintenanceMode() {
    return maintenanceMode;
  }

  public void setMaintenanceMode(Boolean maintenanceMode) {
    this.maintenanceMode = maintenanceMode;
  }

  public String getMaintenanceMessage() {
    return maintenanceMessage;
  }

  public void setMaintenanceMessage(String maintenanceMessage) {
    this.maintenanceMessage = maintenanceMessage;
  }

  public Boolean getAllowRegistration() {
    return allowRegistration;
  }

  public void setAllowRegistration(Boolean allowRegistration) {
    this.allowRegistration = allowRegistration;
  }

  public Boolean getRequireEmailVerification() {
    return requireEmailVerification;
  }

  public void setRequireEmailVerification(Boolean requireEmailVerification) {
    this.requireEmailVerification = requireEmailVerification;
  }

  public Integer getSessionTimeout() {
    return sessionTimeout;
  }

  public void setSessionTimeout(Integer sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  public Integer getMaxLoginAttempts() {
    return maxLoginAttempts;
  }

  public void setMaxLoginAttempts(Integer maxLoginAttempts) {
    this.maxLoginAttempts = maxLoginAttempts;
  }

  public Integer getPasswordMinLength() {
    return passwordMinLength;
  }

  public void setPasswordMinLength(Integer passwordMinLength) {
    this.passwordMinLength = passwordMinLength;
  }

  public Boolean getAllowPasswordReset() {
    return allowPasswordReset;
  }

  public void setAllowPasswordReset(Boolean allowPasswordReset) {
    this.allowPasswordReset = allowPasswordReset;
  }

  public Integer getMaxFileSize() {
    return maxFileSize;
  }

  public void setMaxFileSize(Integer maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  public String getAllowedFileTypes() {
    return allowedFileTypes;
  }

  public void setAllowedFileTypes(String allowedFileTypes) {
    this.allowedFileTypes = allowedFileTypes;
  }

  public Integer getExamAutoSaveInterval() {
    return examAutoSaveInterval;
  }

  public void setExamAutoSaveInterval(Integer examAutoSaveInterval) {
    this.examAutoSaveInterval = examAutoSaveInterval;
  }

  public Boolean getShowLeaderboard() {
    return showLeaderboard;
  }

  public void setShowLeaderboard(Boolean showLeaderboard) {
    this.showLeaderboard = showLeaderboard;
  }

  public Boolean getEnableNotifications() {
    return enableNotifications;
  }

  public void setEnableNotifications(Boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

  public Boolean getAiEnabled() {
    return aiEnabled;
  }

  public void setAiEnabled(Boolean aiEnabled) {
    this.aiEnabled = aiEnabled;
  }

  public Boolean getAiAssistantEnabled() {
    return aiAssistantEnabled;
  }

  public void setAiAssistantEnabled(Boolean aiAssistantEnabled) {
    this.aiAssistantEnabled = aiAssistantEnabled;
  }

  public Boolean getAiAutoGradingEnabled() {
    return aiAutoGradingEnabled;
  }

  public void setAiAutoGradingEnabled(Boolean aiAutoGradingEnabled) {
    this.aiAutoGradingEnabled = aiAutoGradingEnabled;
  }

  public String getAiModel() {
    return aiModel;
  }

  public void setAiModel(String aiModel) {
    this.aiModel = aiModel;
  }

  public String getSystemEmail() {
    return systemEmail;
  }

  public void setSystemEmail(String systemEmail) {
    this.systemEmail = systemEmail;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public String getSiteDescription() {
    return siteDescription;
  }

  public void setSiteDescription(String siteDescription) {
    this.siteDescription = siteDescription;
  }

  public String getSiteLogoUrl() {
    return siteLogoUrl;
  }

  public void setSiteLogoUrl(String siteLogoUrl) {
    this.siteLogoUrl = siteLogoUrl;
  }

  public String getCopyrightText() {
    return copyrightText;
  }

  public void setCopyrightText(String copyrightText) {
    this.copyrightText = copyrightText;
  }
}

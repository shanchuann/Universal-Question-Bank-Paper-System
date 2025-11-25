package com.universal.qbank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_config")
public class SystemConfigEntity {
  @Id private String configKey;

  @Column(length = 1024)
  private String configValue;

  public SystemConfigEntity() {}

  public SystemConfigEntity(String configKey, String configValue) {
    this.configKey = configKey;
    this.configValue = configValue;
  }

  public String getConfigKey() {
    return configKey;
  }

  public void setConfigKey(String configKey) {
    this.configKey = configKey;
  }

  public String getConfigValue() {
    return configValue;
  }

  public void setConfigValue(String configValue) {
    this.configValue = configValue;
  }
}

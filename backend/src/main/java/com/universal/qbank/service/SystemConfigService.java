package com.universal.qbank.service;

import com.universal.qbank.entity.SystemConfigEntity;
import com.universal.qbank.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemConfigService {

  @Autowired private SystemConfigRepository systemConfigRepository;

  public boolean isSystemEnabled() {
    return systemConfigRepository
        .findById("SYSTEM_ENABLED")
        .map(config -> Boolean.parseBoolean(config.getConfigValue()))
        .orElse(true); // Default to true
  }

  public void setSystemEnabled(boolean enabled) {
    SystemConfigEntity config = new SystemConfigEntity("SYSTEM_ENABLED", String.valueOf(enabled));
    systemConfigRepository.save(config);
  }
}

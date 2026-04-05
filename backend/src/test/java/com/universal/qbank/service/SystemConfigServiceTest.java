package com.universal.qbank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.universal.qbank.entity.SystemConfigEntity;
import com.universal.qbank.repository.SystemConfigRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

  @Mock private SystemConfigRepository systemConfigRepository;

  private SystemConfigService systemConfigService;

  @BeforeEach
  void setUp() {
    systemConfigService = new SystemConfigService(systemConfigRepository);
  }

  @Test
  void shouldReturnDefaultWhenConfigMissing() {
    when(systemConfigRepository.findById(SystemConfigKey.AI_MODEL.key()))
        .thenReturn(Optional.empty());

    String value = systemConfigService.getConfig(SystemConfigKey.AI_MODEL);

    assertEquals("gemma4", value);
  }

  @Test
  void shouldFallbackToDefaultIntWhenValueInvalid() {
    when(systemConfigRepository.findById(SystemConfigKey.SESSION_TIMEOUT.key()))
        .thenReturn(
            Optional.of(new SystemConfigEntity(SystemConfigKey.SESSION_TIMEOUT.key(), "abc")));

    int value = systemConfigService.getIntConfig(SystemConfigKey.SESSION_TIMEOUT);

    assertEquals(30, value);
  }

  @Test
  void updateAllSettingsShouldPersistMappedKeys() {
    systemConfigService.updateAllSettings(
        Map.of(
            "aiEnabled", true,
            "aiModel", "qwen3-vl:8b",
            "systemEmail", "ops@example.com"));

    ArgumentCaptor<SystemConfigEntity> captor = ArgumentCaptor.forClass(SystemConfigEntity.class);
    verify(systemConfigRepository, times(3)).save(captor.capture());

    List<SystemConfigEntity> saved = captor.getAllValues();
    assertTrue(
        saved.stream()
            .anyMatch(
                it ->
                    SystemConfigKey.AI_ENABLED.key().equals(it.getConfigKey())
                        && "true".equals(it.getConfigValue())));
    assertTrue(
        saved.stream()
            .anyMatch(
                it ->
                    SystemConfigKey.AI_MODEL.key().equals(it.getConfigKey())
                        && "qwen3-vl:8b".equals(it.getConfigValue())));
    assertTrue(
        saved.stream()
            .anyMatch(
                it ->
                    SystemConfigKey.SYSTEM_EMAIL.key().equals(it.getConfigKey())
                        && "ops@example.com".equals(it.getConfigValue())));
  }
}

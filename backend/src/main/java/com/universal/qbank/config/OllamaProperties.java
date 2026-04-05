package com.universal.qbank.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 统一维护 Ollama 相关配置，避免在多个服务中散落 @Value。 */
@Component
@ConfigurationProperties(prefix = "ai.ollama")
public class OllamaProperties {

  private boolean enabled = true;
  private String baseUrl = "http://localhost:11434";
  private String model = "gemma4";
  private long timeoutMs = 60000L;
  private List<String> modelCandidates =
      new ArrayList<>(
          List.of(
              "gemma4:latest",
              "gemma4:e4b",
              "qwen3-vl:8b",
              "deepseek-r1:8b",
              "codegemma:7b",
              "deepseek-coder:6.7b",
              "llama2:latest"));

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public long getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(long timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public List<String> getModelCandidates() {
    return modelCandidates;
  }

  public void setModelCandidates(List<String> modelCandidates) {
    this.modelCandidates = modelCandidates;
  }

  public String normalizedBaseUrl() {
    String value = baseUrl == null ? "" : baseUrl.trim();
    if (value.isBlank()) {
      return "http://localhost:11434";
    }
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }

  public List<String> normalizedCandidates() {
    if (modelCandidates == null || modelCandidates.isEmpty()) {
      return List.of();
    }
    return modelCandidates.stream()
        .map(value -> value == null ? "" : value.trim())
        .filter(value -> !value.isBlank())
        .distinct()
        .collect(Collectors.toList());
  }

  public long effectiveTimeoutMs() {
    // 避免无效或过小超时导致频繁误判。
    return Math.max(1000L, timeoutMs);
  }
}

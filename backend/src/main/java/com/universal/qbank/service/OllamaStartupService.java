package com.universal.qbank.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OllamaStartupService {

  @Value("${ai.ollama.enabled:false}")
  private boolean ollamaEnabled;

  @Value("${ai.ollama.base-url:http://localhost:11434}")
  private String ollamaBaseUrl;

  @Value("${ai.ollama.auto-start:true}")
  private boolean autoStartFromEnv;

  private final SystemConfigService systemConfigService;

  public OllamaStartupService(SystemConfigService systemConfigService) {
    this.systemConfigService = systemConfigService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void tryStartOllama() {
    if (!ollamaEnabled || !autoStartFromEnv) {
      return;
    }
    if (!systemConfigService.getBooleanConfig(SystemConfigService.AI_ENABLED, false)) {
      return;
    }
    if (!systemConfigService.getBooleanConfig(SystemConfigService.AI_AUTO_START_OLLAMA, true)) {
      return;
    }
    if (isOllamaReachable()) {
      return;
    }

    try {
      ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
      pb.redirectErrorStream(true);
      pb.start();
      System.out.println("AI enabled: attempted to start Ollama by command: ollama serve");
    } catch (Exception ex) {
      System.out.println("AI enabled but failed to auto-start Ollama: " + ex.getMessage());
    }
  }

  private boolean isOllamaReachable() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(trimSlash(ollamaBaseUrl) + "/api/tags"))
              .timeout(Duration.ofSeconds(2))
              .GET()
              .build();
      HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() >= 200 && response.statusCode() < 500;
    } catch (Exception ignored) {
      return false;
    }
  }

  private String trimSlash(String value) {
    if (value == null || value.isBlank()) {
      return "http://localhost:11434";
    }
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }
}

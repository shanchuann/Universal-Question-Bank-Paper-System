package com.universal.qbank.service;

import com.universal.qbank.config.OllamaProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OllamaStartupService {

  private static final Logger log = LoggerFactory.getLogger(OllamaStartupService.class);

  private volatile Process managedOllamaProcess;
  private final Path managedOllamaPidFile =
      Path.of(System.getProperty("java.io.tmpdir"), "uqbank-ollama-managed.pid");

  private final SystemConfigService systemConfigService;
  private final OllamaProperties ollamaProperties;

  public OllamaStartupService(
      SystemConfigService systemConfigService, OllamaProperties ollamaProperties) {
    this.systemConfigService = systemConfigService;
    this.ollamaProperties = ollamaProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void syncOllamaWithAiSettingOnStartup() {
    syncOllamaWithAiSetting();
  }

  public void syncOllamaWithAiSetting() {
    if (!ollamaProperties.isEnabled()) {
      return;
    }
    boolean aiEnabled = systemConfigService.getBooleanConfig(SystemConfigService.AI_ENABLED, false);
    if (aiEnabled) {
      tryStartOllamaIfNeeded();
    } else {
      tryStopOllamaIfNeeded();
    }
  }

  private void tryStartOllamaIfNeeded() {
    if (isOllamaReachable()) {
      return;
    }

    Optional<Long> existingPid = readManagedPid();
    if (existingPid.isPresent() && isProcessAlive(existingPid.get())) {
      log.info("AI enabled: managed Ollama process is already running, pid={}", existingPid.get());
      return;
    }

    try {
      ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
      pb.redirectErrorStream(true);
      Process process = pb.start();
      managedOllamaProcess = process;
      writeManagedPid(process.pid());
      log.info(
          "AI enabled: started managed Ollama process with command: ollama serve, pid={}",
          process.pid());
    } catch (Exception ex) {
      log.warn("AI enabled but failed to start Ollama: {}", ex.getMessage());
    }
  }

  private void tryStopOllamaIfNeeded() {
    Optional<Long> managedPid = readManagedPid();
    if (managedPid.isEmpty()) {
      log.info("AI disabled: no managed Ollama process found, skip stopping");
      return;
    }

    long pid = managedPid.get();
    if (!isProcessAlive(pid)) {
      clearManagedPid();
      log.info("AI disabled: managed Ollama pid already exited, pid={}", pid);
      return;
    }

    try {
      ProcessBuilder pb = buildKillByPidCommand(pid);
      pb.redirectErrorStream(true);
      Process process = pb.start();
      boolean finished = process.waitFor(3, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
      }

      if (!isProcessAlive(pid)) {
        clearManagedPid();
        log.info("AI disabled: stopped managed Ollama process, pid={}", pid);
        return;
      }

      log.warn("AI disabled: failed to stop managed Ollama process, pid={}", pid);
    } catch (Exception ex) {
      log.warn("AI disabled but failed to stop Ollama: {}", ex.getMessage());
    }
  }

  private ProcessBuilder buildKillByPidCommand(long pid) {
    String osName = System.getProperty("os.name", "").toLowerCase();
    if (osName.contains("win")) {
      return new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid));
    }
    return new ProcessBuilder("kill", "-TERM", String.valueOf(pid));
  }

  private Optional<Long> readManagedPid() {
    try {
      if (managedOllamaProcess != null && managedOllamaProcess.isAlive()) {
        return Optional.of(managedOllamaProcess.pid());
      }
      if (!Files.exists(managedOllamaPidFile)) {
        return Optional.empty();
      }
      String raw = Files.readString(managedOllamaPidFile).trim();
      if (raw.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(Long.parseLong(raw));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }

  private void writeManagedPid(long pid) {
    try {
      Files.writeString(
          managedOllamaPidFile,
          String.valueOf(pid),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE);
    } catch (Exception ignored) {
      // ignore pid file errors and rely on in-memory process handle
    }
  }

  private void clearManagedPid() {
    managedOllamaProcess = null;
    try {
      Files.deleteIfExists(managedOllamaPidFile);
    } catch (Exception ignored) {
      // ignore cleanup errors
    }
  }

  private boolean isProcessAlive(long pid) {
    return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
  }

  private boolean isOllamaReachable() {
    try {
      HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(ollamaProperties.normalizedBaseUrl() + "/api/tags"))
              .timeout(Duration.ofSeconds(2))
              .GET()
              .build();
      HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() >= 200 && response.statusCode() < 500;
    } catch (Exception ignored) {
      return false;
    }
  }
}

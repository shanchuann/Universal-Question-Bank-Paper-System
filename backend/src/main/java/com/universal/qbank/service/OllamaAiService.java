package com.universal.qbank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.config.OllamaProperties;
import com.universal.qbank.entity.QuestionEntity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OllamaAiService {

  private static final String DEFAULT_FALLBACK_MESSAGE = "AI 服务暂不可用，请稍后重试";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SystemConfigService systemConfigService;
  private final OllamaProperties ollamaProperties;

  public OllamaAiService(
      SystemConfigService systemConfigService, OllamaProperties ollamaProperties) {
    this.systemConfigService = systemConfigService;
    this.ollamaProperties = ollamaProperties;
  }

  private String getEffectiveModel() {
    String configured = systemConfigService.getConfig(SystemConfigService.AI_MODEL);
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }
    return ollamaProperties.getModel();
  }

  public Map<String, Object> status() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("enabled", ollamaProperties.isEnabled());
    info.put("baseUrl", ollamaProperties.normalizedBaseUrl());
    info.put("model", getEffectiveModel());
    info.put("timeoutMs", ollamaProperties.effectiveTimeoutMs());
    return info;
  }

  public List<String> listAvailableModels() {
    try {
      long timeoutMs = ollamaProperties.effectiveTimeoutMs();
      HttpClient httpClient =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)))
              .build();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(ollamaProperties.normalizedBaseUrl() + "/api/tags"))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofMillis(Math.min(timeoutMs, 5000)))
              .GET()
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return List.of();
      }
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode modelsNode = root.path("models");
      if (!modelsNode.isArray()) {
        return List.of();
      }
      List<String> names = new ArrayList<>();
      for (JsonNode item : modelsNode) {
        String name = item.path("name").asText("").trim();
        if (!name.isBlank()) {
          names.add(name);
        }
      }
      return names.stream().distinct().collect(Collectors.toList());
    } catch (Exception ignored) {
      return List.of();
    }
  }

  public List<Map<String, Object>> getModelCatalog(String configuredModel) {
    List<String> installed = listAvailableModels();
    Set<String> installedSet = new HashSet<>(installed);

    Set<String> all = new HashSet<>();
    for (String candidate : parseModelCandidates()) {
      if (!candidate.isBlank()) {
        all.add(candidate.trim());
      }
    }
    for (String name : installed) {
      if (!name.isBlank()) {
        all.add(name.trim());
      }
    }
    if (configuredModel != null && !configuredModel.isBlank()) {
      all.add(configuredModel.trim());
    }

    List<String> sorted = new ArrayList<>(all);
    Collections.sort(sorted);

    List<Map<String, Object>> items = new ArrayList<>();
    for (String modelName : sorted) {
      boolean installedFlag = installedSet.contains(modelName);
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("name", modelName);
      item.put("installed", installedFlag);
      item.put("needDownload", !installedFlag);
      item.put("selected", configuredModel != null && configuredModel.trim().equals(modelName));
      items.add(item);
    }
    return items;
  }

  public List<Map<String, Object>> getRecommendedModels(String configuredModel) {
    List<Map<String, Object>> catalog = getModelCatalog(configuredModel);
    Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
    for (Map<String, Object> item : catalog) {
      byName.put(String.valueOf(item.get("name")), item);
    }

    List<Map<String, String>> preferred =
        List.of(
            Map.of("name", "qwen3-vl:8b", "reason", "支持图片理解，适合拍照导入题目"),
            Map.of("name", "deepseek-r1:8b", "reason", "推理能力稳定，适合综合问答"),
            Map.of("name", "gemma4:latest", "reason", "通用场景稳定，适合作为默认兜底"));

    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, String> item : preferred) {
      String name = item.get("name");
      Map<String, Object> base =
          byName.getOrDefault(name, Map.of("name", name, "installed", false));
      boolean installed = Boolean.TRUE.equals(base.get("installed"));

      Map<String, Object> row = new LinkedHashMap<>();
      row.put("name", name);
      row.put("reason", item.get("reason"));
      row.put("installed", installed);
      row.put("needDownload", !installed);
      row.put("selected", configuredModel != null && configuredModel.trim().equals(name));
      result.add(row);
    }
    return result;
  }

  public Map<String, Object> pullModel(String modelName) {
    String target = modelName == null ? "" : modelName.trim();
    if (target.isBlank()) {
      throw new RuntimeException("模型名不能为空");
    }

    try {
      long timeoutMs = ollamaProperties.effectiveTimeoutMs();
      HttpClient httpClient =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)))
              .build();

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("name", target);
      body.put("stream", false);
      String payload = objectMapper.writeValueAsString(body);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(ollamaProperties.normalizedBaseUrl() + "/api/pull"))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofMinutes(30))
              .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String detail = localizePullError(extractOllamaError(response.body()));
        throw new RuntimeException(
            "下载模型失败，状态码：" + response.statusCode() + (detail.isBlank() ? "" : "，原因：" + detail));
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("model", target);
      result.put("success", true);
      result.put("status", "downloaded");
      return result;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      String detail = localizePullError(ex.getMessage());
      throw new RuntimeException(detail.startsWith("下载模型失败") ? detail : "下载模型失败：" + detail, ex);
    }
  }

  public void pullModelStreaming(String modelName, Consumer<Map<String, Object>> onProgress) {
    String target = modelName == null ? "" : modelName.trim();
    if (target.isBlank()) {
      throw new RuntimeException("模型名不能为空");
    }

    try {
      long timeoutMs = ollamaProperties.effectiveTimeoutMs();
      HttpClient httpClient =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000)))
              .build();

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("name", target);
      body.put("stream", true);
      String payload = objectMapper.writeValueAsString(body);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(ollamaProperties.normalizedBaseUrl() + "/api/pull"))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofMinutes(30))
              .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
              .build();

      HttpResponse<java.io.InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String detail =
            localizePullError(
                extractOllamaError(
                    new String(response.body().readAllBytes(), StandardCharsets.UTF_8)));
        throw new RuntimeException(
            "下载模型失败，状态码：" + response.statusCode() + (detail.isBlank() ? "" : "，原因：" + detail));
      }

      boolean successReceived = false;
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          String trimmed = line.trim();
          if (trimmed.isBlank()) {
            continue;
          }

          try {
            JsonNode node = objectMapper.readTree(trimmed);
            String error = node.path("error").asText("").trim();
            if (!error.isBlank()) {
              throw new RuntimeException("下载模型失败：" + localizePullError(error));
            }

            long total = node.path("total").asLong(0L);
            long completed = node.path("completed").asLong(0L);
            String status = node.path("status").asText("");
            if ("success".equalsIgnoreCase(status)) {
              successReceived = true;
            }

            int percent = 0;
            if (total > 0) {
              percent = (int) Math.max(0, Math.min(100, (completed * 100) / total));
            } else if ("success".equalsIgnoreCase(status)) {
              percent = 100;
            }

            Map<String, Object> progress = new LinkedHashMap<>();
            progress.put("type", "progress");
            progress.put("model", target);
            progress.put("status", status);
            progress.put("completed", completed);
            progress.put("total", total);
            progress.put("percent", percent);
            onProgress.accept(progress);
          } catch (Exception ignoreLineError) {
            if (ignoreLineError instanceof RuntimeException) {
              throw (RuntimeException) ignoreLineError;
            }
            // ignore malformed streaming line
          }
        }
      }

      List<String> installedModels = listAvailableModels();
      boolean installed = installedModels.contains(target);
      if (!successReceived || !installed) {
        throw new RuntimeException("模型下载未完成或未成功安装，请检查模型名称与 Ollama 日志后重试");
      }
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      String detail = localizePullError(ex.getMessage());
      throw new RuntimeException(detail.startsWith("下载模型失败") ? detail : "下载模型失败：" + detail, ex);
    }
  }

  public String askTeacher(String question, String context) {
    String systemPrompt = "你是高校考试系统的教师助教。请遵循：1)优先给可落地步骤；2)必要时给示例；3)不编造系统不存在的数据；4)输出使用简体中文。";
    String userPrompt =
        "教师问题：\n"
            + safe(question)
            + "\n\n"
            + "教学上下文（若有）：\n"
            + safe(context)
            + "\n\n"
            + "请给出：结论、操作步骤、注意事项。";
    return chat(systemPrompt, userPrompt);
  }

  public String askStudent(String question, String context) {
    String systemPrompt = "你是高校考试系统的学习助教。请遵循：1)解释清晰；2)避免直接代写完整作业答案；3)给思路与示例；4)输出使用简体中文。";
    String userPrompt =
        "学生提问：\n"
            + safe(question)
            + "\n\n"
            + "学习上下文（若有）：\n"
            + safe(context)
            + "\n\n"
            + "请先给思路，再给示例。";
    return chat(systemPrompt, userPrompt);
  }

  public Map<String, Object> suggestSubjectiveScore(
      QuestionEntity question, String studentAnswer, Double maxScore, String rubric) {
    double safeMax = maxScore == null || maxScore <= 0 ? 1.0 : maxScore;
    String reference = buildReference(question);

    String systemPrompt =
        "你是考试阅卷助手。你必须严格返回 JSON，不要返回任何额外文本。JSON 格式："
            + "{\"score\":number,\"reason\":string,\"feedback\":string}。"
            + "score 取值范围 [0, 满分]，保留 1 位小数。";

    String userPrompt =
        "题目：\n"
            + safe(question == null ? null : question.getStem())
            + "\n\n"
            + "题型：\n"
            + safe(question == null ? null : question.getType())
            + "\n\n"
            + "参考答案信息：\n"
            + safe(reference)
            + "\n\n"
            + "评分标准（可为空）：\n"
            + safe(rubric)
            + "\n\n"
            + "学生答案：\n"
            + safe(studentAnswer)
            + "\n\n"
            + "满分："
            + safeMax
            + "\n"
            + "请返回 JSON。";

    String raw = chat(systemPrompt, userPrompt);
    return parseScoreJson(raw, safeMax);
  }

  private String buildReference(QuestionEntity question) {
    if (question == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    if (question.getAnswerSchema() != null && !question.getAnswerSchema().isBlank()) {
      sb.append("answerSchema:\n").append(question.getAnswerSchema().trim()).append("\n\n");
    }
    if (question.getAnalysis() != null && !question.getAnalysis().isBlank()) {
      sb.append("analysis:\n").append(question.getAnalysis().trim()).append("\n\n");
    }
    if (question.getOptionsJson() != null && !question.getOptionsJson().isBlank()) {
      sb.append("optionsJson:\n").append(question.getOptionsJson().trim());
    }
    String text = sb.toString().trim();
    if (text.length() > 1800) {
      return text.substring(0, 1800);
    }
    return text;
  }

  public String chat(String systemPrompt, String userPrompt) {
    return chat(systemPrompt, userPrompt, List.of());
  }

  public String chatWithImages(String systemPrompt, String userPrompt, List<String> base64Images) {
    return chat(systemPrompt, userPrompt, base64Images == null ? List.of() : base64Images);
  }

  private String chat(String systemPrompt, String userPrompt, List<String> base64Images) {
    if (!ollamaProperties.isEnabled()) {
      throw new RuntimeException("AI 功能未开启，请设置 AI_OLLAMA_ENABLED=true");
    }

    try {
      long timeoutMs = ollamaProperties.effectiveTimeoutMs();
      HttpClient httpClient =
          HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();

      List<Map<String, Object>> messages = new ArrayList<>();
      messages.add(Map.of("role", "system", "content", safe(systemPrompt)));

      Map<String, Object> userMessage = new LinkedHashMap<>();
      userMessage.put("role", "user");
      userMessage.put("content", safe(userPrompt));
      if (base64Images != null && !base64Images.isEmpty()) {
        userMessage.put("images", base64Images);
      }
      messages.add(userMessage);

      Map<String, Object> body = new LinkedHashMap<>();
      String effectiveModel = getEffectiveModel();
      body.put("model", effectiveModel);
      body.put("stream", false);
      body.put("messages", messages);

      String payload = objectMapper.writeValueAsString(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(ollamaProperties.normalizedBaseUrl() + "/api/chat"))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofMillis(timeoutMs))
              .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String errBody = response.body() == null ? "" : response.body().trim();
        String detail = extractOllamaError(errBody);
        throw new RuntimeException(
            "调用 Ollama 失败，状态码："
                + response.statusCode()
                + (detail.isBlank() ? "" : "，原因：" + detail)
                + "。请确认 Ollama 已启动且模型 "
                + effectiveModel
                + " 已拉取（ollama pull "
                + effectiveModel
                + "）。");
      }

      JsonNode root = objectMapper.readTree(response.body());
      String content = root.path("message").path("content").asText("").trim();
      if (content.isBlank()) {
        throw new RuntimeException(DEFAULT_FALLBACK_MESSAGE);
      }
      return content;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new RuntimeException("AI 调用失败：" + ex.getMessage(), ex);
    }
  }

  private Map<String, Object> parseScoreJson(String raw, double maxScore) {
    Map<String, Object> fallback = new LinkedHashMap<>();
    fallback.put("score", 0.0);
    fallback.put("reason", "AI 返回内容解析失败，请教师人工复核。");
    fallback.put("feedback", raw == null ? "" : raw);

    if (raw == null || raw.isBlank()) {
      return fallback;
    }

    try {
      String json = extractJson(raw);
      JsonNode node = objectMapper.readTree(json);

      double score = node.path("score").asDouble(0.0);
      score = Math.max(0.0, Math.min(maxScore, score));
      score = Math.round(score * 10.0) / 10.0;

      String reason = node.path("reason").asText("AI 未给出评分依据").trim();
      String feedback = node.path("feedback").asText("").trim();

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("score", score);
      result.put("reason", reason);
      result.put("feedback", feedback);
      return result;
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private String extractJson(String raw) {
    int first = raw.indexOf('{');
    int last = raw.lastIndexOf('}');
    if (first >= 0 && last > first) {
      return raw.substring(first, last + 1);
    }
    return raw;
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private String extractOllamaError(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    try {
      JsonNode node = objectMapper.readTree(body);
      String err = node.path("error").asText("").trim();
      if (!err.isBlank()) {
        return err;
      }
    } catch (Exception ignored) {
      // ignore parse failures and fallback to plain text body
    }
    return body.length() > 120 ? body.substring(0, 120) + "..." : body;
  }

  private String localizePullError(String raw) {
    if (raw == null || raw.isBlank()) {
      return "模型下载失败，请稍后重试";
    }

    String message = raw.trim();
    String lower = message.toLowerCase();

    if (lower.contains("wsarecv")
        || lower.contains("forcibly closed")
        || lower.contains("connection reset")) {
      return "连接模型仓库时网络中断，请检查网络后重试";
    }
    if (lower.contains("timeout") || lower.contains("timed out") || lower.contains("i/o timeout")) {
      return "连接模型仓库超时，请稍后重试";
    }
    if (lower.contains("no such host")
        || lower.contains("name or service not known")
        || lower.contains("temporary failure in name resolution")
        || lower.contains("lookup")) {
      return "无法连接模型仓库，请检查网络或 DNS 配置";
    }
    if (lower.contains("manifest") && (lower.contains("404") || lower.contains("not found"))) {
      return "未找到该模型版本，请确认模型名称是否正确";
    }
    if (lower.contains("qwen3.5:4b")) {
      return "模型 qwen3.5:4b 暂不可用，建议改用 qwen3-vl:8b 或 deepseek-r1:8b";
    }

    return message;
  }

  private List<String> parseModelCandidates() {
    return ollamaProperties.normalizedCandidates();
  }
}

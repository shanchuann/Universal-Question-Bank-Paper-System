package com.universal.qbank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.entity.QuestionEntity;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OllamaAiService {

  private static final String DEFAULT_FALLBACK_MESSAGE = "AI 服务暂不可用，请稍后重试";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${ai.ollama.enabled:true}")
  private boolean enabled;

  @Value("${ai.ollama.base-url:http://localhost:11434}")
  private String baseUrl;

  @Value("${ai.ollama.model:gemma4}")
  private String model;

  @Value("${ai.ollama.timeout-ms:60000}")
  private long timeoutMs;

  @Autowired private SystemConfigService systemConfigService;

  private String getEffectiveModel() {
    String configured = systemConfigService.getConfig(SystemConfigService.AI_MODEL);
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }
    return model;
  }

  public Map<String, Object> status() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("enabled", enabled);
    info.put("baseUrl", baseUrl);
    info.put("model", getEffectiveModel());
    info.put("timeoutMs", timeoutMs);
    return info;
  }

  public List<String> listAvailableModels() {
    try {
      HttpClient httpClient =
          HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 5000))).build();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(trimSlash(baseUrl) + "/api/tags"))
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
    if (!enabled) {
      throw new RuntimeException("AI 功能未开启，请设置 AI_OLLAMA_ENABLED=true");
    }

    try {
      HttpClient httpClient =
          HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();

      List<Map<String, String>> messages = new ArrayList<>();
      messages.add(Map.of("role", "system", "content", safe(systemPrompt)));
      messages.add(Map.of("role", "user", "content", safe(userPrompt)));

      Map<String, Object> body = new LinkedHashMap<>();
      String effectiveModel = getEffectiveModel();
      body.put("model", effectiveModel);
      body.put("stream", false);
      body.put("messages", messages);

      String payload = objectMapper.writeValueAsString(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(trimSlash(baseUrl) + "/api/chat"))
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

  private String trimSlash(String value) {
    if (value == null) {
      return "";
    }
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
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
}

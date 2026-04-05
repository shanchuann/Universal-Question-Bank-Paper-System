package com.universal.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.api.generated.model.QuestionCreateRequest;
import com.universal.qbank.api.generated.model.QuestionOption;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.QuestionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportService {

  @Autowired private QuestionRepository questionRepository;

  @Autowired private OllamaAiService ollamaAiService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final Pattern QUESTION_START_PATTERN = Pattern.compile("^(\\d+)\\s*[.、．]\\s*(.*)");
  // Matches "A. Content" or "a. Content" or "1. Content" or "(A) Content"
  // Group 1: Label from "A." or "A)" or "A、" (supports A-Z, a-z, 0-9)
  // Group 2: Label from "(A)"
  // Group 3: Content
  private static final Pattern OPTION_PATTERN =
      Pattern.compile(
          "(?:^|\\s+)(?:([A-Za-z0-9])\\s*[.、．\\)]|\\(([A-Za-z0-9])\\))\\s*(.*?)(?=\\s+(?:[A-Za-z0-9]\\s*[.、．\\)]|\\([A-Za-z0-9]\\))|$)",
          Pattern.DOTALL);
  private static final Pattern ANSWER_PATTERN = Pattern.compile("^(Answer|答案)[:：]\\s*(.*)");
  private static final Pattern ANALYSIS_PATTERN = Pattern.compile("^(Analysis|解析)[:：]\\s*(.*)");

  public List<QuestionCreateRequest> parseWordDocument(InputStream inputStream) throws IOException {
    List<QuestionCreateRequest> questions = new ArrayList<>();
    try (XWPFDocument document = new XWPFDocument(inputStream)) {
      QuestionCreateRequest currentQuestion = null;
      List<QuestionOption> currentOptions = new ArrayList<>();
      boolean optionsStarted = false;

      for (XWPFParagraph paragraph : document.getParagraphs()) {
        String text = paragraph.getText().trim();
        if (text.isEmpty()) {
          continue;
        }
        System.out.println("Processing paragraph: " + text);

        Matcher questionMatcher = QUESTION_START_PATTERN.matcher(text);
        if (questionMatcher.find()) {
          System.out.println("Found question: " + questionMatcher.group(2));
          // Save previous question
          if (currentQuestion != null) {
            finalizeQuestion(currentQuestion, currentOptions);
            questions.add(currentQuestion);
          }

          // Start new question
          currentQuestion = new QuestionCreateRequest();
          currentQuestion.setStem(questionMatcher.group(2)); // Group 2 is the content
          currentQuestion.setType(QuestionCreateRequest.TypeEnum.SINGLE_CHOICE); // Default
          currentQuestion.setDifficulty(QuestionCreateRequest.DifficultyEnum.MEDIUM); // Default
          currentQuestion.setSubjectId("general"); // Default
          currentQuestion.setScore(5.0f);
          currentOptions = new ArrayList<>();
          optionsStarted = false;
          continue;
        }

        if (currentQuestion != null) {
          // Check for Answer
          Matcher answerMatcher = ANSWER_PATTERN.matcher(text);
          if (answerMatcher.find()) {
            String answerKey = answerMatcher.group(2).trim().toUpperCase();
            System.out.println("Found answer: " + answerKey);
            // Mark correct option
            for (int i = 0; i < currentOptions.size(); i++) {
              char optionLabel = (char) ('A' + i);
              if (answerKey.indexOf(optionLabel) != -1) {
                currentOptions.get(i).setIsCorrect(true);
              }
            }
            continue;
          }

          // Check for Analysis
          Matcher analysisMatcher = ANALYSIS_PATTERN.matcher(text);
          if (analysisMatcher.find()) {
            currentQuestion.setAnalysis(analysisMatcher.group(2));
            continue;
          }

          // Check for Options (Inline or Single line)
          Matcher optionMatcher = OPTION_PATTERN.matcher(text);
          if (optionMatcher.find()) {
            System.out.println("Found options in line: " + text);
            optionsStarted = true;
            // Reset matcher to find all
            optionMatcher.reset();
            while (optionMatcher.find()) {
              String label =
                  optionMatcher.group(1) != null ? optionMatcher.group(1) : optionMatcher.group(2);
              String content = optionMatcher.group(3).trim();
              System.out.println("  Option " + label + ": " + content);

              QuestionOption option = new QuestionOption();
              option.setKey(label.toUpperCase()); // Set key (A, B, C...)
              option.setText(content);
              option.setIsCorrect(false);
              currentOptions.add(option);
            }
            continue;
          }

          // Handle auto-numbered lists as options
          if (paragraph.getNumID() != null) {
            System.out.println("Found auto-numbered option: " + text);
            optionsStarted = true;
            QuestionOption option = new QuestionOption();
            option.setText(text);
            option.setIsCorrect(false);
            currentOptions.add(option);
            continue;
          }

          // If not a new question, not an answer, not an option line:
          // Append to Stem or Last Option
          if (!optionsStarted) {
            currentQuestion.setStem(currentQuestion.getStem() + "\n" + text);
          } else if (!currentOptions.isEmpty()) {
            // Append to the last option
            QuestionOption lastOption = currentOptions.get(currentOptions.size() - 1);
            lastOption.setText(lastOption.getText() + "\n" + text);
          }
        }
      }

      // Add last question
      if (currentQuestion != null) {
        finalizeQuestion(currentQuestion, currentOptions);
        questions.add(currentQuestion);
      }
    }
    return questions;
  }

  public List<QuestionCreateRequest> parsePhotoByAi(InputStream inputStream, String parseMode)
      throws IOException {
    byte[] imageBytes = inputStream.readAllBytes();
    if (imageBytes.length == 0) {
      return List.of();
    }

    String normalizedMode = "SINGLE".equalsIgnoreCase(parseMode) ? "SINGLE" : "PAGE";
    String modeGuide =
        "SINGLE".equals(normalizedMode) ? "当前为单题导入：仅输出 1 道题。" : "当前为整页导入：请尽可能完整识别所有题目。";

    String systemPrompt =
        "你是题库导入助手。你必须只返回 JSON，不要返回任何解释文字或 Markdown。"
            + "输出格式必须是数组，每个元素包含字段："
            + "stem,type,difficulty,score,analysis,answerSchema,options。"
            + "type 仅允许：SINGLE_CHOICE,MULTI_CHOICE,TRUE_FALSE,FILL_BLANK,SHORT_ANSWER。"
            + "difficulty 仅允许：EASY,MEDIUM,HARD。"
            + "options 为数组，元素字段：key,text,isCorrect。"
            + "如果是填空/简答，options 可为空，但 answerSchema.correctAnswer 尽量给出。";

    String userPrompt =
        modeGuide
            + "\n"
            + "请识别图片中的题目并转换为 JSON 数组。"
            + "\n"
            + "每道题 score 默认给 5。subjectId 前端会补充，这里无需输出。";

    String base64 = Base64.getEncoder().encodeToString(imageBytes);
    try {
      String raw = ollamaAiService.chatWithImages(systemPrompt, userPrompt, List.of(base64));
      return parseAiQuestions(raw);
    } catch (RuntimeException ex) {
      throw new RuntimeException(
          "图片解析失败：" + ex.getMessage() + "。请在管理员 AI 设置中选择支持视觉的模型（例如 qwen3-vl:8b），并确认模型已下载。", ex);
    }
  }

  private List<QuestionCreateRequest> parseAiQuestions(String raw) {
    List<QuestionCreateRequest> results = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return results;
    }

    try {
      String cleaned = raw.trim();
      if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
        cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "");
        cleaned = cleaned.replaceFirst("\\n?```$", "");
      }

      int arrayStart = cleaned.indexOf('[');
      int arrayEnd = cleaned.lastIndexOf(']');
      String json;
      if (arrayStart >= 0 && arrayEnd > arrayStart) {
        json = cleaned.substring(arrayStart, arrayEnd + 1);
      } else {
        int objStart = cleaned.indexOf('{');
        int objEnd = cleaned.lastIndexOf('}');
        if (objStart < 0 || objEnd <= objStart) {
          return results;
        }
        String objJson = cleaned.substring(objStart, objEnd + 1);
        var node = objectMapper.readTree(objJson);
        if (node.has("questions") && node.get("questions").isArray()) {
          json = node.get("questions").toString();
        } else {
          json = "[" + objJson + "]";
        }
      }

      var root = objectMapper.readTree(json);
      if (!root.isArray()) {
        return results;
      }

      for (var item : root) {
        String stem = item.path("stem").asText("").trim();
        if (stem.isBlank()) {
          continue;
        }

        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setStem(stem);
        req.setSubjectId("general");
        req.setType(parseType(item.path("type").asText("SINGLE_CHOICE")));
        req.setDifficulty(parseDifficulty(item.path("difficulty").asText("MEDIUM")));
        req.setScore((float) item.path("score").asDouble(5.0));
        req.setAnalysis(item.path("analysis").asText(""));

        List<QuestionOption> options = new ArrayList<>();
        var optionsNode = item.path("options");
        if (optionsNode.isArray()) {
          int index = 0;
          for (var opt : optionsNode) {
            String text = opt.path("text").asText("").trim();
            if (text.isBlank()) {
              continue;
            }
            QuestionOption option = new QuestionOption();
            String key = opt.path("key").asText("").trim();
            if (key.isBlank()) {
              key = String.valueOf((char) ('A' + index));
            }
            option.setKey(key.toUpperCase());
            option.setText(text);
            option.setIsCorrect(opt.path("isCorrect").asBoolean(false));
            options.add(option);
            index++;
          }
        }
        req.setOptions(options);

        var answerSchemaNode = item.path("answerSchema");
        if (!answerSchemaNode.isMissingNode() && !answerSchemaNode.isNull()) {
          req.setAnswerSchema(objectMapper.convertValue(answerSchemaNode, Object.class));
        } else {
          String answer = item.path("answer").asText("").trim();
          if (!answer.isBlank()) {
            Map<String, Object> answerSchema = new LinkedHashMap<>();
            answerSchema.put("correctAnswer", answer);
            req.setAnswerSchema(answerSchema);
          }
        }

        results.add(req);
      }
    } catch (Exception ignored) {
      return List.of();
    }

    return results;
  }

  private QuestionCreateRequest.TypeEnum parseType(String raw) {
    if (raw == null) {
      return QuestionCreateRequest.TypeEnum.SINGLE_CHOICE;
    }
    try {
      return QuestionCreateRequest.TypeEnum.fromValue(raw.trim().toUpperCase());
    } catch (Exception ignored) {
      return QuestionCreateRequest.TypeEnum.SINGLE_CHOICE;
    }
  }

  private QuestionCreateRequest.DifficultyEnum parseDifficulty(String raw) {
    if (raw == null) {
      return QuestionCreateRequest.DifficultyEnum.MEDIUM;
    }
    try {
      return QuestionCreateRequest.DifficultyEnum.fromValue(raw.trim().toUpperCase());
    } catch (Exception ignored) {
      return QuestionCreateRequest.DifficultyEnum.MEDIUM;
    }
  }

  private void finalizeQuestion(QuestionCreateRequest question, List<QuestionOption> options) {
    question.setOptions(options);
    // Basic type inference
    long correctCount = options.stream().filter(QuestionOption::getIsCorrect).count();
    if (correctCount > 1) {
      question.setType(QuestionCreateRequest.TypeEnum.MULTI_CHOICE);
    } else if (options.isEmpty()) {
      // Maybe fill blank or short answer?
      // For now default to SINGLE_CHOICE or leave as is
    }
  }

  @Transactional
  public List<QuestionEntity> saveQuestions(List<QuestionCreateRequest> requests) {
    List<QuestionEntity> entities = new ArrayList<>();
    for (QuestionCreateRequest req : requests) {
      QuestionEntity q = new QuestionEntity();
      q.setSubjectId(req.getSubjectId());
      q.setType(req.getType().getValue());
      q.setDifficulty(req.getDifficulty().getValue());
      q.setStem(req.getStem());
      q.setAnalysis(req.getAnalysis());
      q.setStatus("APPROVED"); // 导入的题目直接设为已通过状态

      try {
        if (req.getOptions() != null) {
          q.setOptionsJson(objectMapper.writeValueAsString(req.getOptions()));
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to serialize options", e);
      }

      entities.add(q);
    }
    return questionRepository.saveAll(entities);
  }
}

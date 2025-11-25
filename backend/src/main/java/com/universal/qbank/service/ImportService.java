package com.universal.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.api.generated.model.QuestionCreateRequest;
import com.universal.qbank.api.generated.model.QuestionOption;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.QuestionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportService {

  @Autowired
  private QuestionRepository questionRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final Pattern QUESTION_START_PATTERN = Pattern.compile("^(\\d+)\\s*[.、．]\\s*(.*)");
  // Matches "A. Content" or "a. Content" or "1. Content" or "(A) Content"
  // Group 1: Label from "A." or "A)" or "A、" (supports A-Z, a-z, 0-9)
  // Group 2: Label from "(A)"
  // Group 3: Content
  private static final Pattern OPTION_PATTERN = Pattern.compile("(?:^|\\s+)(?:([A-Za-z0-9])\\s*[.、．\\)]|\\(([A-Za-z0-9])\\))\\s*(.*?)(?=\\s+(?:[A-Za-z0-9]\\s*[.、．\\)]|\\([A-Za-z0-9]\\))|$)", Pattern.DOTALL);
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
                String label = optionMatcher.group(1) != null ? optionMatcher.group(1) : optionMatcher.group(2);
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
          q.setStatus("ACTIVE");
          
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

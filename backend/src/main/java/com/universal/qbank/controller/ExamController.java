package com.universal.qbank.controller;

import com.universal.qbank.api.generated.model.ExamQuestion;
import com.universal.qbank.api.generated.model.ExamSessionPage;
import com.universal.qbank.api.generated.model.ExamSessionResponse;
import com.universal.qbank.api.generated.model.ManualGradeRequest;
import com.universal.qbank.api.generated.model.QuestionOption;
import com.universal.qbank.entity.ExamEntity;
import com.universal.qbank.entity.ExamRecordEntity;
import com.universal.qbank.entity.PaperEntity;
import com.universal.qbank.entity.PaperItemEntity;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.PaperRepository;
import com.universal.qbank.repository.QuestionRepository;
import com.universal.qbank.service.ExamService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

  @Autowired private ExamService examService;

  @Autowired private PaperRepository paperRepository;

  @Autowired private QuestionRepository questionRepository;

  @Autowired
  private com.universal.qbank.repository.UserRepository userRepository;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new com.fasterxml.jackson.databind.ObjectMapper();

  public static class StartExamRequest {
    public Long paperId;
    public String userId;
    public String type;
  }

  public static class ExamResponse {
    public Long id;
    public PaperDetail paper;
    public String userId;
    public Integer score;
    public List<RecordDetail> records; // Add records to response
  }

  public static class RecordDetail {
    public String questionId;
    public String userAnswer;
    public Boolean isCorrect;
    public Boolean isFlagged;
  }

  public static class PaperDetail {
    public Long id;
    public String title;
    public List<QuestionDetail> questions;
    public List<PaperItemDTO> items;
  }

  public static class PaperItemDTO {
    public String type;
    public String id;
    public String sectionTitle;
    public Double score;
  }

  public static class QuestionDetail {
    public String id;
    public String stem;
    public String type;
    public List<String> options;
  }

  // ManualGradeRequest and ExamSessionPage removed to use generated models

  @PostMapping("/start")
  @org.springframework.transaction.annotation.Transactional
  public ResponseEntity<ExamResponse> startExam(@RequestBody StartExamRequest req) {
    ExamEntity exam = examService.startExam(req.paperId, req.userId, req.type);
    return ResponseEntity.ok(toExamResponse(exam));
  }

  public static class SubmitExamRequest {
    public Map<String, String> answers;
    public List<String> flaggedQuestions;
  }

  @PostMapping("/{id}/submit")
  @org.springframework.transaction.annotation.Transactional
  public ResponseEntity<ExamResponse> submitExam(
      @PathVariable Long id, @RequestBody SubmitExamRequest req) {
    ExamEntity exam = examService.submitExam(id, req.answers, req.flaggedQuestions);
    return ResponseEntity.ok(toExamResponse(exam));
  }

  /** 用于列表显示的简化响应DTO */
  public static class ExamListItem {
    public String sessionId;
    public String paperVersionId;
    public String userId;
    public String nickname;
    public String username;
    public Integer score;
    public String status;
    public java.time.OffsetDateTime startAt;
    public java.time.OffsetDateTime endAt;
  }

  public static class ExamListPage {
    public int totalElements;
    public int totalPages;
    public List<ExamListItem> content;
  }

  @GetMapping
  public ResponseEntity<ExamListPage> listExams(
      @RequestParam(required = false) Long paperId,
      @RequestParam(required = false) String userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    org.springframework.data.domain.Page<ExamEntity> pageResult =
        examService.listExams(paperId, userId, page, size);

    ExamListPage resp = new ExamListPage();
    resp.totalElements = (int) pageResult.getTotalElements();
    resp.totalPages = pageResult.getTotalPages();
    resp.content = pageResult.getContent().stream()
        .map(this::toExamListItem)
        .collect(Collectors.toList());

    return ResponseEntity.ok(resp);
  }

  /** 转换为列表显示项 */
  private ExamListItem toExamListItem(ExamEntity exam) {
    ExamListItem item = new ExamListItem();
    item.sessionId = String.valueOf(exam.getId());
    item.paperVersionId = String.valueOf(exam.getPaperId());
    item.userId = exam.getUserId();
    item.score = exam.getScore();
    item.startAt = exam.getStartTime();
    item.endAt = exam.getEndTime();
    item.status = exam.getEndTime() != null ? "已阅卷" : "进行中";

    // 查找用户信息
    if (exam.getUserId() != null) {
      userRepository.findById(exam.getUserId()).ifPresent(user -> {
        item.nickname = user.getNickname();
        item.username = user.getUsername();
      });
    }

    return item;
  }

  /** 简化版本，用于列表查询，避免加载太多数据 */
  private ExamSessionResponse toExamSessionResponseSimple(ExamEntity exam) {
    ExamSessionResponse resp = new ExamSessionResponse();
    resp.setSessionId(String.valueOf(exam.getId()));
    resp.setPaperVersionId(String.valueOf(exam.getPaperId()));
    resp.setUserId(exam.getUserId());
    resp.setScore(exam.getScore());
    resp.setStartAt(exam.getStartTime());
    resp.setEndAt(exam.getEndTime());

    // 查找用户信息
    if (exam.getUserId() != null) {
      try {
        userRepository.findById(exam.getUserId()).ifPresent(user -> {
          // 用 reflection 设置额外属性
          try {
            java.lang.reflect.Method setAdditionalProperties = 
                resp.getClass().getMethod("setAdditionalProperties", String.class, Object.class);
            setAdditionalProperties.invoke(resp, "nickname", user.getNickname());
            setAdditionalProperties.invoke(resp, "username", user.getUsername());
            setAdditionalProperties.invoke(resp, "role", user.getRole());
          } catch (Exception ignore) {}
        });
      } catch (Exception e) {
        // ignore
      }
    }

    return resp;
  }

  @PostMapping("/{id}/grade")
  @org.springframework.transaction.annotation.Transactional
  public ResponseEntity<ExamResponse> gradeExam(
      @PathVariable Long id, @RequestBody ManualGradeRequest request) {
    ExamEntity exam = examService.gradeExam(id, request);
    return ResponseEntity.ok(toExamResponse(exam));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ExamSessionResponse> getExam(@PathVariable Long id) {
    ExamEntity exam = examService.getExam(id);
    return ResponseEntity.ok(toExamSessionResponse(exam));
  }

  private ExamSessionResponse toExamSessionResponse(ExamEntity exam) {
    ExamSessionResponse resp = new ExamSessionResponse();
    resp.setSessionId(String.valueOf(exam.getId()));
    resp.setPaperVersionId(String.valueOf(exam.getPaperId()));
    resp.setUserId(exam.getUserId());
    resp.setScore(exam.getScore());
    resp.setStartAt(exam.getStartTime());
    resp.setEndAt(exam.getEndTime());

    // 查找用户信息并直接设置扩展字段
    if (exam.getUserId() != null) {
      com.universal.qbank.entity.UserEntity user = null;
      try {
        user = userRepository.findById(exam.getUserId()).orElse(null);
      } catch (Exception e) {
        // ignore
      }
      if (user != null) {
        // 尝试设置 nickname 和 username (如果字段存在)
        try {
           java.lang.reflect.Method setNickname = resp.getClass().getMethod("setNickname", String.class);
           setNickname.invoke(resp, user.getNickname());
        } catch (Exception ignore) {
           // Fallback to additionalProperties if method doesn't exist
           try {
             java.lang.reflect.Method setAdditionalProperties = resp.getClass().getMethod("setAdditionalProperties", String.class, Object.class);
             setAdditionalProperties.invoke(resp, "nickname", user.getNickname());
           } catch (Exception ignore2) {}
        }
        
        try {
           java.lang.reflect.Method setUsername = resp.getClass().getMethod("setUsername", String.class);
           setUsername.invoke(resp, user.getUsername());
        } catch (Exception ignore) {
           try {
             java.lang.reflect.Method setAdditionalProperties = resp.getClass().getMethod("setAdditionalProperties", String.class, Object.class);
             setAdditionalProperties.invoke(resp, "username", user.getUsername());
           } catch (Exception ignore2) {}
        }
      }
    }

    PaperEntity paper = paperRepository.findById(exam.getPaperId()).orElse(null);
    
    List<String> qIds = new ArrayList<>();
    Map<String, Double> scoreMap = new java.util.HashMap<>();
    
    // Try to get question IDs from paper
    if (paper != null) {
        if (paper.getQuestionIds() != null && !paper.getQuestionIds().isEmpty()) {
            qIds = new ArrayList<>(paper.getQuestionIds());
        }
        // Fallback to items if questionIds is empty
        if (qIds.isEmpty() && paper.getItems() != null && !paper.getItems().isEmpty()) {
            for (PaperItemEntity item : paper.getItems()) {
                if ("QUESTION".equals(item.getItemType())) {
                    qIds.add(item.getQuestionId());
                    scoreMap.put(item.getQuestionId(), item.getScore());
                }
            }
        }
    }
    
    // Fallback: get question IDs from exam records if paper has no questions
    if (qIds.isEmpty() && exam.getRecords() != null && !exam.getRecords().isEmpty()) {
        qIds = exam.getRecords().stream()
            .map(ExamRecordEntity::getQuestionId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
    }

    if (qIds.isEmpty()) {
        return resp;
    }

    List<QuestionEntity> questions = questionRepository.findAllById(qIds);
    Map<String, QuestionEntity> questionMap = questions.stream()
        .collect(Collectors.toMap(QuestionEntity::getId, q -> q));

    Map<String, ExamRecordEntity> recordMap = new java.util.HashMap<>();
    if (exam.getRecords() != null) {
      for (ExamRecordEntity r : exam.getRecords()) {
        recordMap.put(r.getQuestionId(), r);
      }
    }

    long seed = exam.getRandomSeed() != null ? exam.getRandomSeed() : 0;
    java.util.Random rng = new java.util.Random(seed);

    List<ExamQuestion> examQuestions = new ArrayList<>();

    if (paper != null && paper.getItems() != null && !paper.getItems().isEmpty()) {
        // Use items to drive the list (preserves order and score)
        for (PaperItemEntity item : paper.getItems()) {
            if ("QUESTION".equals(item.getItemType())) {
                QuestionEntity q = questionMap.get(item.getQuestionId());
                if (q != null) {
                    ExamQuestion eq = new ExamQuestion();
                    eq.setQuestionId(q.getId());
                    eq.setStem(q.getStem());
                    eq.setType(q.getType());
                    eq.setScore(java.math.BigDecimal.valueOf(item.getScore()));

                    if (q.getOptionsJson() != null) {
                        List<QuestionOption> opts = parseOptions(q.getOptionsJson(), rng);
                        eq.setOptions(opts);
                    }

                    ExamRecordEntity record = recordMap.get(q.getId());
                    if (record != null) {
                        eq.setUserAnswer(record.getUserAnswer());
                        eq.setAwardedScore(
                            record.getScore() != null ? java.math.BigDecimal.valueOf(record.getScore()) : null);
                        eq.setGraderNotes(record.getNotes());
                        eq.setIsCorrect(record.getIsCorrect());
                    }
                    examQuestions.add(eq);
                }
            }
        }
    } else {
        // Use qIds order if possible, or just the list
        for (String qId : qIds) {
             QuestionEntity q = questionMap.get(qId);
             if (q != null) {
                 ExamQuestion eq = new ExamQuestion();
                 eq.setQuestionId(q.getId());
                 eq.setStem(q.getStem());
                 eq.setType(q.getType());
                 // Use score from scoreMap if available
                 Double maxScore = scoreMap.getOrDefault(qId, 1.0);
                 eq.setScore(java.math.BigDecimal.valueOf(maxScore));

                 if (q.getOptionsJson() != null) {
                    List<QuestionOption> opts = parseOptions(q.getOptionsJson(), rng);
                    eq.setOptions(opts);
                 }

                 ExamRecordEntity record = recordMap.get(q.getId());
                 if (record != null) {
                    eq.setUserAnswer(record.getUserAnswer());
                    eq.setAwardedScore(
                        record.getScore() != null ? java.math.BigDecimal.valueOf(record.getScore()) : null);
                    eq.setGraderNotes(record.getNotes());
                    eq.setIsCorrect(record.getIsCorrect());
                 }
                 examQuestions.add(eq);
             }
        }
    }

    resp.setQuestions(examQuestions);
    return resp;
  }

  private ExamResponse toExamResponse(ExamEntity exam) {
    PaperEntity paper = paperRepository.findById(exam.getPaperId()).orElseThrow();
    List<QuestionEntity> questions = questionRepository.findAllById(paper.getQuestionIds());

    ExamResponse resp = new ExamResponse();
    resp.id = exam.getId();
    resp.userId = exam.getUserId();
    resp.score = exam.getScore();

    PaperDetail pd = new PaperDetail();
    pd.id = paper.getId();
    pd.title = paper.getTitle();

    // Randomization
    long seed = exam.getRandomSeed() != null ? exam.getRandomSeed() : 0;
    java.util.Random rng = new java.util.Random(seed);

    pd.questions =
        questions.stream()
            .map(
                q -> {
                  QuestionDetail qd = new QuestionDetail();
                  qd.id = q.getId();
                  qd.stem = q.getStem();
                  qd.type = q.getType();
                  qd.options = new java.util.ArrayList<>();
                  if (q.getOptionsJson() != null) {
                    List<QuestionOption> opts = parseOptions(q.getOptionsJson(), rng);
                    qd.options =
                        opts.stream().map(QuestionOption::getText).collect(Collectors.toList());
                  }
                  return qd;
                })
            .collect(Collectors.toList());

    // Ensure items are initialized and mapped
    if (paper.getItems() != null && !paper.getItems().isEmpty()) {
      pd.items =
          paper.getItems().stream()
              .map(
                  item -> {
                    PaperItemDTO dto = new PaperItemDTO();
                    dto.type = item.getItemType();
                    dto.id = item.getQuestionId();
                    dto.sectionTitle = item.getSectionTitle();
                    dto.score = item.getScore();
                    return dto;
                  })
              .collect(Collectors.toList());
    } else {
      pd.items = new java.util.ArrayList<>();
      // Shuffle questions if no items structure
      java.util.Collections.shuffle(pd.questions, rng);
    }

    resp.paper = pd;

    if (exam.getRecords() != null) {
      resp.records =
          exam.getRecords().stream()
              .map(
                  r -> {
                    RecordDetail rd = new RecordDetail();
                    rd.questionId = r.getQuestionId();
                    rd.userAnswer = r.getUserAnswer();
                    rd.isCorrect = r.getIsCorrect();
                    rd.isFlagged = r.getIsFlagged();
                    return rd;
                  })
              .collect(Collectors.toList());
    }

    return resp;
  }

  private List<QuestionOption> parseOptions(String json, java.util.Random rng) {
    List<QuestionOption> result = new ArrayList<>();
    if (json == null) return result;

    // Debug logging
    System.out.println("DEBUG: Parsing optionsJson: " + json);

    try {
      // Try 1: Standard List<QuestionOption>
      result =
          objectMapper.readValue(
              json, new com.fasterxml.jackson.core.type.TypeReference<List<QuestionOption>>() {});
    } catch (Exception e) {
      try {
        // Try 2: List<String>
        List<String> strings =
            objectMapper.readValue(
                json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        for (String s : strings) {
          QuestionOption opt = new QuestionOption();
          opt.setText(s);
          result.add(opt);
        }
      } catch (Exception e2) {
        try {
          // Try 3: List<Map<String, Object>> - Generic Object
          List<Map<String, Object>> maps =
              objectMapper.readValue(
                  json,
                  new com.fasterxml.jackson.core.type.TypeReference<
                      List<Map<String, Object>>>() {});
          for (Map<String, Object> map : maps) {
            QuestionOption opt = new QuestionOption();
            // Try to find text content
            if (map.containsKey("text")) opt.setText(String.valueOf(map.get("text")));
            else if (map.containsKey("content")) opt.setText(String.valueOf(map.get("content")));
            else if (map.containsKey("value")) opt.setText(String.valueOf(map.get("value")));
            else if (map.containsKey("label")) opt.setText(String.valueOf(map.get("label")));

            // Try to find key/label
            if (map.containsKey("key")) opt.setKey(String.valueOf(map.get("key")));

            // Try to find isCorrect
            if (map.containsKey("isCorrect"))
              opt.setIsCorrect(Boolean.valueOf(String.valueOf(map.get("isCorrect"))));

            result.add(opt);
          }
        } catch (Exception e3) {
          System.err.println("Failed to parse optionsJson: " + json);
          e3.printStackTrace();
        }
      }
    }

    if (rng != null && !result.isEmpty()) {
      java.util.Collections.shuffle(result, rng);
    }
    return result;
  }
}

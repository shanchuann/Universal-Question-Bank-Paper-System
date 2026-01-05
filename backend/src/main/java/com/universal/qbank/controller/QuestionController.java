package com.universal.qbank.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.api.generated.QuestionBankApi;
import com.universal.qbank.api.generated.model.QuestionCreateRequest;
import com.universal.qbank.api.generated.model.QuestionOption;
import com.universal.qbank.api.generated.model.QuestionPage;
import com.universal.qbank.api.generated.model.QuestionResponse;
import com.universal.qbank.api.generated.model.QuestionSummary;
import com.universal.qbank.api.generated.model.QuestionUpdateRequest;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.QuestionRepository;
import com.universal.qbank.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuestionController implements QuestionBankApi {

  @Autowired private QuestionRepository questionRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private HttpServletRequest httpRequest;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public ResponseEntity<QuestionResponse> apiQuestionsPost(QuestionCreateRequest req) {
    QuestionEntity q = new QuestionEntity();
    q.setSubjectId(req.getSubjectId());
    q.setType(req.getType().getValue());
    q.setDifficulty(req.getDifficulty().getValue());
    q.setStem(req.getStem());
    
    // 从请求头获取用户ID并设置为创建者
    String authHeader = httpRequest.getHeader("Authorization");
    String userId = null;
    if (authHeader != null && authHeader.startsWith("Bearer dummy-jwt-token-")) {
      userId = authHeader.replace("Bearer dummy-jwt-token-", "");
      q.setCreatedBy(userId);
    }

    try {
      if (req.getOptions() != null) {
        q.setOptionsJson(objectMapper.writeValueAsString(req.getOptions()));
      }
      // answerSchema 可能是字符串或对象，直接转为字符串存储
      if (req.getAnswerSchema() != null) {
        Object answer = req.getAnswerSchema();
        if (answer instanceof String) {
          q.setAnswerSchema((String) answer);
        } else {
          // 如果是对象，尝试获取其字符串表示
          String answerStr = answer.toString();
          // 如果是空对象 "{}"，则不保存
          if (!answerStr.equals("{}") && !answerStr.isEmpty()) {
            q.setAnswerSchema(answerStr);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize options or answer schema", e);
    }

    // q.setTags(req.getTags());
    q.setKnowledgePointIds(req.getKnowledgePointIds());
    
    // 根据用户角色设置初始状态：学生创建的为待审核，教师/管理员创建的直接激活
    String initialStatus = "ACTIVE";
    if (userId != null) {
      var userOpt = userRepository.findById(userId);
      if (userOpt.isPresent()) {
        String role = userOpt.get().getRole();
        if ("USER".equals(role) || "STUDENT".equals(role)) {
          initialStatus = "PENDING_REVIEW";
        }
      }
    }
    q.setStatus(initialStatus);

    QuestionEntity saved = questionRepository.save(q);
    return ResponseEntity.ok(convertToResponse(saved));
  }

  private QuestionResponse convertToResponse(QuestionEntity entity) {
    QuestionResponse resp = new QuestionResponse();
    resp.setId(entity.getId());
    resp.setSubjectId(entity.getSubjectId());
    resp.setType(entity.getType());
    resp.setDifficulty(entity.getDifficulty());
    resp.setStatus(entity.getStatus());
    resp.setTags(entity.getTags());
    resp.setKnowledgePointIds(entity.getKnowledgePointIds());
    resp.setStem(entity.getStem());
    resp.setAnalysis(entity.getAnalysis());
    
    // 设置 answerSchema
    if (entity.getAnswerSchema() != null) {
      resp.setAnswerSchema(entity.getAnswerSchema());
    }

    try {
      if (entity.getOptionsJson() != null) {
        List<QuestionOption> options =
            objectMapper.readValue(
                entity.getOptionsJson(), new TypeReference<List<QuestionOption>>() {});
        resp.setOptions(options);
      }
    } catch (Exception e) {
      // Log error but don't fail the request
      System.err.println("Failed to deserialize options for question " + entity.getId());
    }

    return resp;
  }

  @Override
  public ResponseEntity<QuestionResponse> apiQuestionsQuestionIdGet(String id) {
    return questionRepository
        .findById(id)
        .map(this::convertToResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> apiQuestionsQuestionIdDelete(String id) {
    if (questionRepository.existsById(id)) {
      questionRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> apiQuestionsQuestionIdPut(String id, QuestionUpdateRequest req) {
    return questionRepository
        .findById(id)
        .map(
            existing -> {
              if (req.getStem() != null) {
                existing.setStem(req.getStem());
              }
              if (req.getDifficulty() != null) {
                existing.setDifficulty(req.getDifficulty());
              }
              if (req.getAnalysis() != null) {
                existing.setAnalysis(req.getAnalysis());
              }

              try {
                if (req.getOptions() != null) {
                  existing.setOptionsJson(objectMapper.writeValueAsString(req.getOptions()));
                }
                // answerSchema 可能是字符串或对象，直接转为字符串存储
                if (req.getAnswerSchema() != null) {
                  Object answer = req.getAnswerSchema();
                  if (answer instanceof String) {
                    existing.setAnswerSchema((String) answer);
                  } else {
                    String answerStr = answer.toString();
                    if (!answerStr.equals("{}") && !answerStr.isEmpty()) {
                      existing.setAnswerSchema(answerStr);
                    }
                  }
                }
              } catch (Exception e) {
                throw new RuntimeException("Failed to serialize options or answer schema", e);
              }

              if (req.getKnowledgePointIds() != null) {
                existing.setKnowledgePointIds(req.getKnowledgePointIds());
              }
              // 保持原有状态，不修改
              questionRepository.save(existing);
              return ResponseEntity.noContent().<Void>build();
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<QuestionPage> apiQuestionsGet(
      Integer page,
      Integer size,
      String subjectId,
      List<String> knowledgePointIds,
      String type,
      String difficulty,
      String keywords,
      String status) {
    int pageNo = (page == null) ? 0 : page;
    int pageSize = (size == null) ? 10 : size;

    Specification<QuestionEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (subjectId != null && !subjectId.isEmpty()) {
            predicates.add(cb.equal(root.get("subjectId"), subjectId));
          }
          if (type != null && !type.isEmpty()) {
            predicates.add(cb.equal(root.get("type"), type));
          }
          if (difficulty != null && !difficulty.isEmpty()) {
            predicates.add(cb.equal(root.get("difficulty"), difficulty));
          }
          // 状态过滤：如果没有指定状态，默认只显示已通过的题目
          if (status != null && !status.isEmpty()) {
            predicates.add(cb.equal(root.get("status"), status));
          } else {
            // 默认只显示 APPROVED 状态的题目（审核通过的）
            predicates.add(root.get("status").in("APPROVED", "PUBLISHED"));
          }
          if (keywords != null && !keywords.isEmpty()) {
            String likePattern = "%" + keywords + "%";
            predicates.add(cb.like(root.get("stem"), likePattern));
          }
          if (knowledgePointIds != null && !knowledgePointIds.isEmpty()) {
            // Join or check if list contains any of the IDs
            // Since knowledgePointIds is ElementCollection, we can use join
            // predicates.add(root.join("knowledgePointIds").in(knowledgePointIds));
            // But this might cause duplicates if multiple match. Distinct needed.
            // Or simpler:
            // predicates.add(root.join("knowledgePointIds").in(knowledgePointIds));
            // query.distinct(true);

            // For now, let's assume exact match or contains logic.
            // JPA Criteria for ElementCollection 'in'
            predicates.add(root.join("knowledgePointIds").in(knowledgePointIds));
            query.distinct(true);
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    Page<QuestionEntity> pagedResult =
        questionRepository.findAll(
            spec, PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

    QuestionPage response = new QuestionPage();
    response.setTotalElements((int) pagedResult.getTotalElements());
    response.setTotalPages(pagedResult.getTotalPages());
    // response.setNumber(pagedResult.getNumber());
    // response.setSize(pagedResult.getSize());
    response.setContent(
        pagedResult.getContent().stream().map(this::convertToSummary).collect(Collectors.toList()));

    return ResponseEntity.ok(response);
  }

  private QuestionSummary convertToSummary(QuestionEntity entity) {
    QuestionSummary summary = new QuestionSummary();
    summary.setId(entity.getId());
    summary.setSubjectId(entity.getSubjectId());
    summary.setStem(entity.getStem());
    summary.setType(entity.getType());
    summary.setDifficulty(entity.getDifficulty());
    summary.setStatus(entity.getStatus());
    summary.setTags(entity.getTags());
    summary.setKnowledgePointIds(entity.getKnowledgePointIds());
    summary.setOptionsJson(entity.getOptionsJson());
    summary.setAnswerSchema(entity.getAnswerSchema());
    summary.setAnalysis(entity.getAnalysis());
    summary.setReviewNotes(entity.getReviewNotes());
    summary.setCreatedBy(entity.getCreatedBy());
    summary.setCreatedAt(entity.getCreatedAt());
    return summary;
  }
}

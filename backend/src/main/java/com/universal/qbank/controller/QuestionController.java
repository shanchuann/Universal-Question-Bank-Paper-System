package com.universal.qbank.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universal.qbank.api.generated.QuestionBankApi;
import com.universal.qbank.api.generated.model.QuestionCreateRequest;
import com.universal.qbank.api.generated.model.QuestionOption;
import com.universal.qbank.api.generated.model.QuestionPage;
import com.universal.qbank.api.generated.model.QuestionResponse;
import com.universal.qbank.api.generated.model.QuestionSummary;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.QuestionRepository;
import jakarta.persistence.criteria.Predicate;
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
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public ResponseEntity<QuestionResponse> questionsPost(QuestionCreateRequest req) {
    QuestionEntity q = new QuestionEntity();
    q.setSubjectId(req.getSubjectId());
    q.setType(req.getType().getValue());
    q.setDifficulty(req.getDifficulty().getValue());
    q.setStem(req.getStem());

    try {
      if (req.getOptions() != null) {
        q.setOptionsJson(objectMapper.writeValueAsString(req.getOptions()));
      }
      if (req.getAnswerSchema() != null) {
        q.setAnswerSchema(objectMapper.writeValueAsString(req.getAnswerSchema()));
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize options or answer schema", e);
    }

    // q.setTags(req.getTags());
    q.setKnowledgePointIds(req.getKnowledgePointIds());
    q.setStatus("ACTIVE");

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

    try {
      if (entity.getOptionsJson() != null) {
        List<QuestionOption> options =
            objectMapper.readValue(
                entity.getOptionsJson(), new TypeReference<List<QuestionOption>>() {});
        resp.setOptions(options);
      }
      // We could also deserialize answerSchema if needed, but QuestionResponse might not have it
      // yet
      // or it might be part of the response.
    } catch (Exception e) {
      // Log error but don't fail the request
      System.err.println("Failed to deserialize options for question " + entity.getId());
    }

    return resp;
  }

  @Override
  public ResponseEntity<QuestionResponse> questionsQuestionIdGet(String id) {
    return questionRepository
        .findById(id)
        .map(this::convertToResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> questionsQuestionIdDelete(String id) {
    if (questionRepository.existsById(id)) {
      questionRepository.deleteById(id);
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<QuestionPage> questionsGet(
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
          if (status != null && !status.isEmpty()) {
            predicates.add(cb.equal(root.get("status"), status));
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
    summary.setCreatedAt(entity.getCreatedAt());
    return summary;
  }
}

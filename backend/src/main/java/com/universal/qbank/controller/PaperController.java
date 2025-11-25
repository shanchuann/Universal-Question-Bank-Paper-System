package com.universal.qbank.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.universal.qbank.entity.PaperEntity;
import com.universal.qbank.entity.PaperItemEntity;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.QuestionRepository;
import com.universal.qbank.service.PaperService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/papers")
public class PaperController {

  @Autowired private PaperService paperService;

  @Autowired private QuestionRepository questionRepository;

  @Autowired private com.universal.qbank.repository.PaperRepository paperRepository;

  @Autowired private com.universal.qbank.service.ExportService exportService;

  @GetMapping
  public ResponseEntity<List<PaperResponse>> listPapers() {
    List<PaperEntity> papers = paperRepository.findAll();
    List<PaperResponse> response = papers.stream().map(this::toPaperResponse).collect(Collectors.toList());
    response.forEach(p -> System.out.println("DEBUG: Returning paper with ID: " + p.id));
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePaper(@PathVariable Long id) {
    paperService.deletePaper(id);
    return ResponseEntity.noContent().build();
  }

  public static class GenerateRequest {
    public String title;
    public int total = 10;
    public Map<String, Integer> typeCounts; // optional
    public String difficulty; // optional

    public GenerateRequest() {}
  }

  public static class PaperResponse {
    @JsonProperty("id")
    public Long id;
    @JsonProperty("title")
    public String title;
    @JsonProperty("createdAt")
    public OffsetDateTime createdAt;
    @JsonProperty("questions")
    public List<QuestionPreview> questions;
    @JsonProperty("items")
    public List<PaperItemDTO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<QuestionPreview> getQuestions() { return questions; }
    public void setQuestions(List<QuestionPreview> questions) { this.questions = questions; }
    
    public List<PaperItemDTO> getItems() { return items; }
    public void setItems(List<PaperItemDTO> items) { this.items = items; }
  }

  public static class QuestionPreview {
    @JsonProperty("id")
    public String id;
    @JsonProperty("subjectId")
    public String subjectId;
    @JsonProperty("type")
    public String type;
    @JsonProperty("difficulty")
    public String difficulty;
    @JsonProperty("status")
    public String status;
    @JsonProperty("tags")
    public List<String> tags;
    @JsonProperty("createdAt")
    public OffsetDateTime createdAt;
    @JsonProperty("stem")
    public String stem;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }
  }

  public static class PaperItemDTO {
    public String type; // QUESTION, SECTION
    public String id; // questionId
    public String sectionTitle;
    public Double score;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
  }

  @GetMapping("/{id}/export/word")
  public ResponseEntity<byte[]> exportWord(
      @PathVariable Long id, @RequestParam(defaultValue = "false") boolean teacher) {
    try {
      byte[] content = exportService.exportPaperToWord(id, teacher);
      return ResponseEntity.ok()
          .header(
              org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"paper_" + id + (teacher ? "_teacher" : "_student") + ".docx\"")
          .contentType(
              org.springframework.http.MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
          .body(content);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/{id}/export/answer-sheet")
  public ResponseEntity<byte[]> exportAnswerSheet(@PathVariable Long id) {
    try {
      byte[] content = exportService.exportAnswerSheet(id);
      return ResponseEntity.ok()
          .header(
              org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"paper_" + id + "_answer_sheet.docx\"")
          .contentType(
              org.springframework.http.MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
          .body(content);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/generate")
  public ResponseEntity<PaperResponse> generate(@RequestBody GenerateRequest req) {
    PaperEntity paper =
        paperService.generatePaper(req.title, req.total, req.typeCounts, req.difficulty);
    return ResponseEntity.ok(toPaperResponse(paper));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaperResponse> getPaper(@PathVariable Long id) {
    PaperEntity paper =
        paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
    return ResponseEntity.ok(toPaperResponse(paper));
  }

  public static class UpdatePaperRequest {
    public String title;
    public List<String> questionIds;
    public List<PaperItemDTO> items;
  }

  @PutMapping("/{id}")
  public ResponseEntity<PaperResponse> updatePaper(
      @PathVariable Long id, @RequestBody UpdatePaperRequest req) {
    PaperEntity paper =
        paperRepository.findById(id).orElseThrow(() -> new RuntimeException("Paper not found"));
    if (req.title != null) {
      paper.setTitle(req.title);
    }
    if (req.items != null) {
      paper.getItems().clear();
      List<PaperItemEntity> newItems = new java.util.ArrayList<>();
      List<String> newQuestionIds = new java.util.ArrayList<>();

      for (int i = 0; i < req.items.size(); i++) {
        PaperItemDTO dto = req.items.get(i);
        PaperItemEntity item = new PaperItemEntity();
        item.setPaper(paper);
        item.setItemType(dto.type);
        item.setQuestionId(dto.id);
        item.setSectionTitle(dto.sectionTitle);
        item.setScore(dto.score);
        item.setSortOrder(i);
        newItems.add(item);

        if ("QUESTION".equals(dto.type) && dto.id != null) {
          newQuestionIds.add(dto.id);
        }
      }
      paper.getItems().addAll(newItems);
      paper.setQuestionIds(newQuestionIds);
    } else if (req.questionIds != null) {
      paper.setQuestionIds(req.questionIds);
    }
    paperRepository.save(paper);
    return ResponseEntity.ok(toPaperResponse(paper));
  }

  public static class ManualCreateRequest {
    public String title;
    public List<String> questionIds;
    public List<PaperItemDTO> items;
  }

  @PostMapping("/manual")
  public ResponseEntity<PaperResponse> createManual(@RequestBody ManualCreateRequest req) {
    PaperEntity paper;
    if (req.items != null && !req.items.isEmpty()) {
      List<PaperItemEntity> entities =
          req.items.stream()
              .map(
                  dto -> {
                    PaperItemEntity item = new PaperItemEntity();
                    item.setItemType(dto.type);
                    item.setQuestionId(dto.id);
                    item.setSectionTitle(dto.sectionTitle);
                    item.setScore(dto.score);
                    return item;
                  })
              .collect(Collectors.toList());
      paper = paperService.createComplexPaper(req.title, entities);
    } else {
      paper = paperService.createManualPaper(req.title, req.questionIds);
    }
    return ResponseEntity.ok(toPaperResponse(paper));
  }

  @DeleteMapping
  public ResponseEntity<Void> deleteAllPapers() {
    paperRepository.deleteAll();
    return ResponseEntity.noContent().build();
  }

  private PaperResponse toPaperResponse(PaperEntity paper) {
    List<QuestionEntity> questions = questionRepository.findAllById(paper.getQuestionIds());

    PaperResponse resp = new PaperResponse();
    resp.id = paper.getId();
    resp.title = paper.getTitle();
    resp.createdAt = paper.getCreatedAt();
    resp.questions =
        questions.stream()
            .map(
                q -> {
                  QuestionPreview p = new QuestionPreview();
                  p.id = q.getId();
                  p.subjectId = q.getSubjectId();
                  p.type = q.getType();
                  p.difficulty = q.getDifficulty();
                  p.status = q.getStatus();
                  p.tags = q.getTags();
                  p.createdAt = q.getCreatedAt();
                  p.stem = q.getStem();
                  return p;
                })
            .collect(Collectors.toList());

    if (paper.getItems() != null) {
      resp.items =
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
    }

    return resp;
  }
}

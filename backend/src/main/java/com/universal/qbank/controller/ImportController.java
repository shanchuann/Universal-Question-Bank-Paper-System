package com.universal.qbank.controller;

import com.universal.qbank.api.generated.model.QuestionCreateRequest;
import com.universal.qbank.service.ImportService;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ImportController {

  @Autowired private ImportService importService;

  @PostMapping("/word")
  public ResponseEntity<List<QuestionCreateRequest>> importWord(
      @RequestParam("file") MultipartFile file) {
    try {
      List<QuestionCreateRequest> questions =
          importService.parseWordDocument(file.getInputStream());
      return ResponseEntity.ok(questions);
    } catch (IOException e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/save")
  public ResponseEntity<List<com.universal.qbank.entity.QuestionEntity>> saveQuestions(
      @org.springframework.web.bind.annotation.RequestBody List<QuestionCreateRequest> questions) {
    return ResponseEntity.ok(importService.saveQuestions(questions));
  }
}

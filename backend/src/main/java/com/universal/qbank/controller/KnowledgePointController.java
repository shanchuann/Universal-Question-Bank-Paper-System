package com.universal.qbank.controller;

import com.universal.qbank.entity.KnowledgePointEntity;
import com.universal.qbank.repository.KnowledgePointRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge-points")
public class KnowledgePointController {

  @Autowired private KnowledgePointRepository repository;

  @GetMapping
  public List<KnowledgePointEntity> list(@RequestParam(required = false) String subjectId) {
    if (subjectId != null) {
      return repository.findBySubjectIdOrderBySortOrderAsc(subjectId);
    }
    return repository.findAll();
  }

  @PostMapping
  public KnowledgePointEntity create(@RequestBody KnowledgePointEntity entity) {
    return repository.save(entity);
  }

  @PutMapping("/{id}")
  public ResponseEntity<KnowledgePointEntity> update(
      @PathVariable String id, @RequestBody KnowledgePointEntity entity) {
    return repository
        .findById(id)
        .map(
            existing -> {
              existing.setName(entity.getName());
              existing.setParentId(entity.getParentId());
              existing.setSortOrder(entity.getSortOrder());
              existing.setLevel(entity.getLevel());
              return ResponseEntity.ok(repository.save(existing));
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    repository.deleteById(id);
    return ResponseEntity.ok().build();
  }
}

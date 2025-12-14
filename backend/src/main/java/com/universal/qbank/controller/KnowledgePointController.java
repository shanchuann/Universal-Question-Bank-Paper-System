package com.universal.qbank.controller;

import com.universal.qbank.entity.KnowledgePointEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.KnowledgePointRepository;
import com.universal.qbank.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge-points")
public class KnowledgePointController {

  @Autowired private KnowledgePointRepository repository;

  @Autowired private UserService userService;

  private static final Set<String> ALLOWED_LEVELS = Set.of("CHAPTER", "SECTION", "POINT");

  private String getUserIdFromToken(String token) {
    if (token == null) return null;
    if (token.startsWith("Bearer ")) {
      token = token.substring(7);
    }
    if (token.startsWith("dummy-jwt-token-")) {
      return token.substring("dummy-jwt-token-".length());
    }
    return null;
  }

  private boolean isTeacherOrAdmin(String token) {
    if (token == null) return false;
    String userId = getUserIdFromToken(token);
    if (userId == null) return false;
    if ("admin".equals(userId)) {
      return true; // fallback admin
    }
    Optional<UserEntity> user = userService.getUserById(userId);
    return user.map(u -> "TEACHER".equalsIgnoreCase(u.getRole()) || "ADMIN".equalsIgnoreCase(u.getRole()))
        .orElse(false);
  }

  private ResponseEntity<String> validate(KnowledgePointEntity entity) {
    if (entity == null) {
      return ResponseEntity.badRequest().body("Payload is required");
    }
    if (entity.getName() == null || entity.getName().isBlank()) {
      return ResponseEntity.badRequest().body("Name is required");
    }
    if (entity.getLevel() == null || !ALLOWED_LEVELS.contains(entity.getLevel())) {
      return ResponseEntity.badRequest().body("Invalid level; must be CHAPTER/SECTION/POINT");
    }
    if (entity.getSortOrder() == null) {
      entity.setSortOrder(0);
    }
    if (entity.getSubjectId() == null || entity.getSubjectId().isBlank()) {
      entity.setSubjectId("general");
    }
    return null;
  }

  @GetMapping
  public List<KnowledgePointEntity> list(@RequestParam(required = false) String subjectId) {
    if (subjectId != null) {
      return repository.findBySubjectIdOrderBySortOrderAsc(subjectId);
    }
    return repository.findAll();
  }

  @PostMapping
  public ResponseEntity<?> create(
      @RequestHeader(value = "Authorization", required = false) String token,
      @RequestBody KnowledgePointEntity entity) {
    if (!isTeacherOrAdmin(token)) {
      return ResponseEntity.status(403).body("Forbidden: teacher or admin only");
    }
    ResponseEntity<String> validation = validate(entity);
    if (validation != null) return validation;
    KnowledgePointEntity saved = repository.save(Objects.requireNonNull(entity));
    return ResponseEntity.ok(Objects.requireNonNull(saved));
  }

  @PutMapping("/{id}")
  public ResponseEntity<KnowledgePointEntity> update(
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable String id,
      @RequestBody KnowledgePointEntity entity) {
    if (!isTeacherOrAdmin(token)) {
      return ResponseEntity.status(403).build();
    }
    ResponseEntity<String> validation = validate(entity);
    if (validation != null) {
      return ResponseEntity.status(validation.getStatusCode()).build();
    }
    return repository
        .findById(Objects.requireNonNull(id))
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
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable String id) {
    if (!isTeacherOrAdmin(token)) {
      return ResponseEntity.status(403).build();
    }
    repository.deleteById(Objects.requireNonNull(id));
    return ResponseEntity.ok().build();
  }
}

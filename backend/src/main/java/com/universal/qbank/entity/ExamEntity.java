package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "exams")
public class ExamEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long paperId;

  private String userId; // For now, just a string, e.g., "admin"

  private Integer score;

  private OffsetDateTime startTime;

  private OffsetDateTime endTime;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "exam_id")
  private List<ExamRecordEntity> records;

  private Long randomSeed;

  private String type; // "EXAM" or "PRACTICE"

  @PrePersist
  public void prePersist() {
    if (this.startTime == null) {
      this.startTime = OffsetDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPaperId() {
    return paperId;
  }

  public void setPaperId(Long paperId) {
    this.paperId = paperId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(OffsetDateTime startTime) {
    this.startTime = startTime;
  }

  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(OffsetDateTime endTime) {
    this.endTime = endTime;
  }

  public List<ExamRecordEntity> getRecords() {
    return records;
  }

  public void setRecords(List<ExamRecordEntity> records) {
    this.records = records;
  }

  public Long getRandomSeed() {
    return randomSeed;
  }

  public void setRandomSeed(Long randomSeed) {
    this.randomSeed = randomSeed;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}

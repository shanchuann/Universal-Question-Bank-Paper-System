package com.universal.qbank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "student_stats")
public class StudentStatsEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String userId;

  private String nickname;

  private Long totalQuestionsAnswered = 0L;

  private Long correctAnswers = 0L;

  private Integer currentStreak = 0;

  private LocalDate lastPracticeDate;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Long getTotalQuestionsAnswered() {
    return totalQuestionsAnswered;
  }

  public void setTotalQuestionsAnswered(Long totalQuestionsAnswered) {
    this.totalQuestionsAnswered = totalQuestionsAnswered;
  }

  public Long getCorrectAnswers() {
    return correctAnswers;
  }

  public void setCorrectAnswers(Long correctAnswers) {
    this.correctAnswers = correctAnswers;
  }

  public Integer getCurrentStreak() {
    return currentStreak;
  }

  public void setCurrentStreak(Integer currentStreak) {
    this.currentStreak = currentStreak;
  }

  public LocalDate getLastPracticeDate() {
    return lastPracticeDate;
  }

  public void setLastPracticeDate(LocalDate lastPracticeDate) {
    this.lastPracticeDate = lastPracticeDate;
  }

  public Double getAccuracy() {
    if (totalQuestionsAnswered == 0) return 0.0;
    return (double) correctAnswers / totalQuestionsAnswered;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
}

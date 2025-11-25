package com.universal.qbank.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_records")
public class ExamRecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String questionId;

  private String userAnswer; // Comma separated for multiple choice

  private Boolean isCorrect;

  private Double score;

  private String notes;

  private Boolean isFlagged = false;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getQuestionId() {
    return questionId;
  }

  public void setQuestionId(String questionId) {
    this.questionId = questionId;
  }

  public String getUserAnswer() {
    return userAnswer;
  }

  public void setUserAnswer(String userAnswer) {
    this.userAnswer = userAnswer;
  }

  public Boolean getIsCorrect() {
    return isCorrect;
  }

  public void setIsCorrect(Boolean correct) {
    isCorrect = correct;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Boolean getIsFlagged() {
    return isFlagged;
  }

  public void setIsFlagged(Boolean isFlagged) {
    this.isFlagged = isFlagged;
  }
}

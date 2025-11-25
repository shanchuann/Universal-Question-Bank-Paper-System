package com.universal.qbank.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "paper_items")
public class PaperItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "paper_id")
  private PaperEntity paper;

  @Column(name = "item_type")
  private String itemType; // QUESTION, SECTION

  @Column(name = "question_id")
  private String questionId;

  @Column(name = "section_title")
  private String sectionTitle;

  @Column(name = "score")
  private Double score;

  @Column(name = "sort_order")
  private Integer sortOrder;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public PaperEntity getPaper() {
    return paper;
  }

  public void setPaper(PaperEntity paper) {
    this.paper = paper;
  }

  public String getItemType() {
    return itemType;
  }

  public void setItemType(String itemType) {
    this.itemType = itemType;
  }

  public String getQuestionId() {
    return questionId;
  }

  public void setQuestionId(String questionId) {
    this.questionId = questionId;
  }

  public String getSectionTitle() {
    return sectionTitle;
  }

  public void setSectionTitle(String sectionTitle) {
    this.sectionTitle = sectionTitle;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}

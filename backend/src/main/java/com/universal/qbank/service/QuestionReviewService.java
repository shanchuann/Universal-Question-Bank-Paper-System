package com.universal.qbank.service;

import com.universal.qbank.entity.*;
import com.universal.qbank.repository.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionReviewService {

  @Autowired private QuestionRepository questionRepository;

  @Autowired private QuestionVersionRepository questionVersionRepository;

  @Autowired private QuestionReviewRepository questionReviewRepository;

  /** 提交审核 */
  @Transactional
  public void submitForReview(String questionId, String submitterId) {
    QuestionEntity question =
        questionRepository.findById(questionId).orElseThrow(() -> new RuntimeException("Not found"));

    if (!"DRAFT".equals(question.getStatus()) && !"REJECTED".equals(question.getStatus())) {
      throw new RuntimeException("Only draft or rejected questions can be submitted for review");
    }

    // 保存版本快照
    saveVersion(question, submitterId, "Submitted for review");

    // 更新状态
    question.setStatus("PENDING_REVIEW");
    questionRepository.save(question);

    // 记录审核日志
    QuestionReviewEntity review = new QuestionReviewEntity();
    review.setQuestionId(questionId);
    review.setVersion(question.getVersion());
    review.setAction("SUBMIT");
    review.setReviewerId(submitterId);
    review.setNotes("Submitted for review");
    questionReviewRepository.save(review);
  }

  /** 审核通过 */
  @Transactional
  public void approve(String questionId, String reviewerId, String notes) {
    QuestionEntity question =
        questionRepository.findById(questionId).orElseThrow(() -> new RuntimeException("Not found"));

    // 支持从 PENDING_REVIEW 或 REJECTED 状态通过
    if (!"PENDING_REVIEW".equals(question.getStatus()) && !"REJECTED".equals(question.getStatus())) {
      throw new RuntimeException("Only pending or rejected questions can be approved");
    }

    question.setStatus("APPROVED");
    question.setReviewerId(reviewerId);
    question.setReviewedAt(OffsetDateTime.now());
    question.setReviewNotes(notes);
    questionRepository.save(question);

    // 记录审核日志
    QuestionReviewEntity review = new QuestionReviewEntity();
    review.setQuestionId(questionId);
    review.setVersion(question.getVersion());
    review.setAction("APPROVE");
    review.setReviewerId(reviewerId);
    review.setNotes(notes);
    questionReviewRepository.save(review);
  }

  /** 审核退回/撤回 */
  @Transactional
  public void reject(String questionId, String reviewerId, String notes) {
    QuestionEntity question =
        questionRepository.findById(questionId).orElseThrow(() -> new RuntimeException("Not found"));

    // 支持从 PENDING_REVIEW 或 APPROVED 状态撤回
    if (!"PENDING_REVIEW".equals(question.getStatus()) && !"APPROVED".equals(question.getStatus())) {
      throw new RuntimeException("Only pending or approved questions can be rejected/revoked");
    }

    question.setStatus("REJECTED");
    question.setReviewerId(reviewerId);
    question.setReviewedAt(OffsetDateTime.now());
    question.setReviewNotes(notes);
    questionRepository.save(question);

    // 记录审核日志
    QuestionReviewEntity review = new QuestionReviewEntity();
    review.setQuestionId(questionId);
    review.setVersion(question.getVersion());
    review.setAction("REJECT");
    review.setReviewerId(reviewerId);
    review.setNotes(notes);
    questionReviewRepository.save(review);
  }

  /** 获取待审核题目列表 */
  public Page<QuestionEntity> getPendingReviewQuestions(Pageable pageable) {
    return questionRepository.findByStatus("PENDING_REVIEW", pageable);
  }

  /** 获取题目版本历史 */
  public List<QuestionVersionEntity> getVersionHistory(String questionId) {
    return questionVersionRepository.findByQuestionIdOrderByVersionDesc(questionId);
  }

  /** 获取题目审核记录 */
  public List<QuestionReviewEntity> getReviewHistory(String questionId) {
    return questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(questionId);
  }

  /** 保存版本快照 */
  private void saveVersion(QuestionEntity question, String changedBy, String summary) {
    QuestionVersionEntity version = new QuestionVersionEntity();
    version.setQuestionId(question.getId());
    version.setVersion(question.getVersion());
    version.setStem(question.getStem());
    version.setOptionsJson(question.getOptionsJson());
    version.setAnswerSchema(question.getAnswerSchema());
    version.setAnalysis(question.getAnalysis());
    version.setDifficulty(question.getDifficulty());
    version.setChangedBy(changedBy);
    version.setChangeSummary(summary);
    questionVersionRepository.save(version);

    // 递增版本号
    question.setVersion(question.getVersion() + 1);
  }
}

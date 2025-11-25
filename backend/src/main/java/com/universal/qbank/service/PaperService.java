package com.universal.qbank.service;

import com.universal.qbank.entity.PaperEntity;
import com.universal.qbank.entity.PaperItemEntity;
import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.repository.PaperRepository;
import com.universal.qbank.repository.QuestionRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaperService {

  @Autowired private QuestionRepository questionRepository;

  @Autowired private PaperRepository paperRepository;

  /**
   * Generate a paper with given total count. If typeCounts is provided, try to pick per-type
   * counts.
   */
  public PaperEntity generatePaper(
      String title, int total, Map<String, Integer> typeCounts, String difficulty) {
    List<QuestionEntity> pool = questionRepository.findAll();

    // filter by difficulty if provided
    if (difficulty != null && !difficulty.isEmpty()) {
      pool =
          pool.stream()
              .filter(q -> difficulty.equalsIgnoreCase(q.getDifficulty()))
              .collect(Collectors.toList());
    }

    if (pool.isEmpty()) {
      throw new IllegalStateException("No questions available to generate paper");
    }

    List<String> selectedIds = new ArrayList<>();
    Random rnd = new Random();

    if (typeCounts != null && !typeCounts.isEmpty()) {
      // group by type
      Map<String, List<QuestionEntity>> byType =
          pool.stream().collect(Collectors.groupingBy(q -> q.getType() == null ? "" : q.getType()));
      for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
        String type = e.getKey();
        int need = e.getValue();
        List<QuestionEntity> list = byType.getOrDefault(type, Collections.emptyList());
        Collections.shuffle(list, rnd);
        for (int i = 0; i < Math.min(need, list.size()); i++) {
          selectedIds.add(list.get(i).getId());
        }
      }
    }

    // fill remaining
    Collections.shuffle(pool, rnd);
    for (QuestionEntity q : pool) {
      if (selectedIds.size() >= total) break;
      if (!selectedIds.contains(q.getId())) selectedIds.add(q.getId());
    }

    PaperEntity paper = new PaperEntity();
    paper.setTitle(title == null ? "Generated Paper" : title);
    paper.setQuestionIds(selectedIds.stream().limit(total).collect(Collectors.toList()));

    return paperRepository.save(paper);
  }

  public PaperEntity createManualPaper(String title, List<String> questionIds) {
    PaperEntity paper = new PaperEntity();
    paper.setTitle(title == null ? "Manual Paper" : title);
    paper.setQuestionIds(questionIds == null ? Collections.emptyList() : questionIds);
    return paperRepository.save(paper);
  }

  public PaperEntity createComplexPaper(String title, List<PaperItemEntity> items) {
    PaperEntity paper = new PaperEntity();
    paper.setTitle(title == null ? "Manual Paper" : title);

    // Populate legacy questionIds for backward compatibility
    List<String> qIds =
        items.stream()
            .filter(i -> "QUESTION".equals(i.getItemType()))
            .map(PaperItemEntity::getQuestionId)
            .collect(Collectors.toList());
    paper.setQuestionIds(qIds);

    // Link items to paper
    if (items != null) {
      for (int i = 0; i < items.size(); i++) {
        PaperItemEntity item = items.get(i);
        item.setPaper(paper);
        item.setSortOrder(i);
      }
      paper.setItems(items);
    }

    return paperRepository.save(paper);
  }

  public void deletePaper(Long id) {
    paperRepository.deleteById(id);
  }
}

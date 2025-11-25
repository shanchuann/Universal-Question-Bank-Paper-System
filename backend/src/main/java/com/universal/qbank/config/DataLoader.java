package com.universal.qbank.config;

import com.universal.qbank.entity.QuestionEntity;
import com.universal.qbank.entity.UserEntity;
import com.universal.qbank.repository.QuestionRepository;
import com.universal.qbank.repository.UserRepository;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {

  @Bean
  public CommandLineRunner loadData(
      QuestionRepository questionRepository, UserRepository userRepository) {
    return args -> {
      // Seed Admin User
      if (userRepository.findByUsername("admin").isEmpty()) {
        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setPassword("password"); // In production, use BCrypt
        admin.setRole("ADMIN");
        admin.setEmail("admin@example.com");
        userRepository.save(admin);
        System.out.println("Admin user seeded.");
      }

      if (questionRepository.count() == 0) {
        QuestionEntity q1 = new QuestionEntity();
        q1.setSubjectId("MATH");
        q1.setType("SINGLE_CHOICE");
        q1.setDifficulty("EASY");
        q1.setStatus("PUBLISHED");
        q1.setStem("What is 1 + 1?");
        q1.setOptionsJson(
            "[{\"text\":\"1\",\"isCorrect\":false},{\"text\":\"2\",\"isCorrect\":true},{\"text\":\"3\",\"isCorrect\":false},{\"text\":\"4\",\"isCorrect\":false}]");
        q1.setTags(Arrays.asList("arithmetic", "basic"));

        QuestionEntity q2 = new QuestionEntity();
        q2.setSubjectId("HISTORY");
        q2.setType("MULTIPLE_CHOICE");
        q2.setDifficulty("MEDIUM");
        q2.setStatus("PUBLISHED");
        q2.setStem("Which of the following are continents?");
        q2.setOptionsJson(
            "[{\"text\":\"Asia\",\"isCorrect\":true},{\"text\":\"Africa\",\"isCorrect\":true},{\"text\":\"London\",\"isCorrect\":false},{\"text\":\"Mars\",\"isCorrect\":false}]");
        q2.setTags(Arrays.asList("geography", "world"));

        QuestionEntity q3 = new QuestionEntity();
        q3.setSubjectId("SCIENCE");
        q3.setType("TRUE_FALSE");
        q3.setDifficulty("HARD");
        q3.setStatus("DRAFT");
        q3.setStem("The earth is flat.");
        q3.setOptionsJson(
            "[{\"text\":\"True\",\"isCorrect\":false},{\"text\":\"False\",\"isCorrect\":true}]");
        q3.setTags(Arrays.asList("physics", "astronomy"));

        questionRepository.saveAll(Arrays.asList(q1, q2, q3));
        System.out.println("Sample questions loaded.");
      }
    };
  }
}

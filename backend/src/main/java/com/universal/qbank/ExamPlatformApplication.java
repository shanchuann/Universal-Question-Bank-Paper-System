package com.universal.qbank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ExamPlatformApplication {

  private static final Logger log = LoggerFactory.getLogger(ExamPlatformApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(ExamPlatformApplication.class, args);
  }

  @Bean
  public CommandLineRunner cleanupLegacyTables(
      JdbcTemplate jdbcTemplate,
      @org.springframework.beans.factory.annotation.Value(
              "${app.cleanup.drop-legacy-question-options:true}")
          boolean dropLegacyQuestionOptions) {
    return args -> {
      if (!dropLegacyQuestionOptions) {
        log.info(
            "Skip dropping legacy QUESTION_OPTIONS table (app.cleanup.drop-legacy-question-options=false)");
        return;
      }
      try {
        jdbcTemplate.execute("DROP TABLE IF EXISTS QUESTION_OPTIONS");
        log.info("Dropped legacy table QUESTION_OPTIONS");
      } catch (Exception e) {
        log.warn("Failed to drop table QUESTION_OPTIONS: {}", e.getMessage());
      }
    };
  }
}

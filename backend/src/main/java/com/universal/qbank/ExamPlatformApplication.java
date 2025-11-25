package com.universal.qbank;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class ExamPlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExamPlatformApplication.class, args);
  }

  @Bean
  public CommandLineRunner cleanupLegacyTables(JdbcTemplate jdbcTemplate) {
    return args -> {
      try {
        jdbcTemplate.execute("DROP TABLE IF EXISTS QUESTION_OPTIONS");
        System.out.println("Dropped legacy table QUESTION_OPTIONS");
      } catch (Exception e) {
        System.out.println("Failed to drop table QUESTION_OPTIONS: " + e.getMessage());
      }
    };
  }
}

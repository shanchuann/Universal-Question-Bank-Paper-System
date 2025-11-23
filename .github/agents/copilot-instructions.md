# Universal-Question-Bank-Paper-System Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-11-18

## Active Technologies
- Java 17 (Spring Boot 3.2), TypeScript 5.4 (Vue 3.4) + Spring Boot starters (Web, Data JPA, Security, Validation), Spring Cloud OpenAPI, MapStruct, Apache POI XWPF, Apache Commons RNG MT19937, Redis Java client (Lettuce), MySQL Connector/J, Flyway, Prometheus Java client, Vue 3, Vite, Pinia, Axios, Element Plus, Apache ECharts 5 (001-specify-exam-platform)
- MySQL 8 (InnoDB, partitioned tables), Redis 7 (autosave buffer + caching), OSS media bucket (images/audio/video) (001-specify-exam-platform)
- Backend Java 21 (Zulu) with Spring Boot 3.2.5 -- acceptance notes require Java 17 toolchain (NEEDS CLARIFICATION); Frontend TypeScript 5.x with Vue 3.4 + Spring Boot starters (Web, Data JPA, Security, Validation, Actuator), OpenAPI Generator 7.x, Spotless, Gradle Kotlin DSL; Vite 5, Vue Router, Pinia, Vitest, ESLint, Prettier (confirm versions) (001-specify-exam-platform)
- MySQL 8 (constitution standard) with Redis 7 cache and MinIO object storage (align local stack with compose) (001-specify-exam-platform)

- Backend Java 17 (Spring Boot 3.x), Frontend TypeScript + Vue 3, Node 18 toolchain + Spring Security 6, Spring Data JPA, MapStruct, Apache POI (XWPF), Apache Commons RNG (MersenneTwister), Redis (Lettuce client), MySQL Connector/J, Vue Router, Pinia, Axios, Element Plus UI, Apache ECharts (001-specify-exam-platform)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

npm test; npm run lint

## Code Style

Backend Java 17 (Spring Boot 3.x), Frontend TypeScript + Vue 3, Node 18 toolchain: Follow standard conventions

## Recent Changes
- 001-specify-exam-platform: Added Backend Java 21 (Zulu) with Spring Boot 3.2.5 -- acceptance notes require Java 17 toolchain (NEEDS CLARIFICATION); Frontend TypeScript 5.x with Vue 3.4 + Spring Boot starters (Web, Data JPA, Security, Validation, Actuator), OpenAPI Generator 7.x, Spotless, Gradle Kotlin DSL; Vite 5, Vue Router, Pinia, Vitest, ESLint, Prettier (confirm versions)
- 001-specify-exam-platform: Added Java 17 (Spring Boot 3.2), TypeScript 5.4 (Vue 3.4) + Spring Boot starters (Web, Data JPA, Security, Validation), Spring Cloud OpenAPI, MapStruct, Apache POI XWPF, Apache Commons RNG MT19937, Redis Java client (Lettuce), MySQL Connector/J, Flyway, Prometheus Java client, Vue 3, Vite, Pinia, Axios, Element Plus, Apache ECharts 5

- 001-specify-exam-platform: Added Backend Java 17 (Spring Boot 3.x), Frontend TypeScript + Vue 3, Node 18 toolchain + Spring Security 6, Spring Data JPA, MapStruct, Apache POI (XWPF), Apache Commons RNG (MersenneTwister), Redis (Lettuce client), MySQL Connector/J, Vue Router, Pinia, Axios, Element Plus UI, Apache ECharts

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

# Phase 0 Research Notes

## Decision: Adopt Apache POI XWPF for Word Question Ingestion

- **Rationale**: Apache POI is Apache-licensed, aligns with Spring ecosystem, supports DOCX parsing with granular control over paragraphs, tables, images, and embedded objects, and integrates with existing Java stack without commercial licensing. Extensive community usage in education content pipelines and compatibility with on-prem deployments make it a low-risk choice.
- **Alternatives considered**:
  - **Aspose.Words for Java**: Feature-rich and faster for complex layouts but introduces licensing costs and potential legal review overhead.
  - **docx4j**: Open-source but less matured for mixed media extraction and has slower performance benchmarks under heavy batch imports.

## Decision: Standardize on Apache Commons RNG MersenneTwister

- **Rationale**: Apache Commons RNG provides a well-tested `MersenneTwister` implementation with reproducible seeding, thread-local generators, and better performance compared to custom ports. It integrates cleanly with Java 17 modules and offers deterministic sequences needed for audit trails.
- **Alternatives considered**:
  - **Uncommons Maths**: Mature but unmaintained in recent years and not modularized for JPMS.
  - **Custom MT19937 implementation**: Higher maintenance risk and requires additional verification for statistical correctness.

## Decision: Use Apache ECharts for Analytics Visualizations

- **Rationale**: ECharts delivers rich visualization types needed for score distributions, error rates, and knowledge mastery, supports responsive design, and integrates with Vue 3 via official wrappers. It handles large datasets efficiently and provides accessible theming out-of-the-box.
- **Alternatives considered**:
  - **Chart.js**: Simpler API but limited for drill-down analytics and large data volumes without custom plugins.
  - **D3.js**: Very flexible but requires significant custom rendering code, slowing delivery of dashboards.

## Decision: Exam Autosave Persistence via Redis-backed Write-Ahead Buffer

- **Rationale**: Leveraging Redis with TTL-backed hashes allows sub-500 ms writes, resilience during transient outages, and eventual persistence to MySQL via asynchronous workers. Supports optimistic locking and session sharding for ≥500 concurrent users.
- **Alternatives considered**:
  - **Direct MySQL writes per autosave**: Risks exceeding latency budget under concurrent load due to disk I/O contention.
  - **Client-side local storage only**: Fails compliance requirements for auditability and recovery after device loss.

## Decision: Pin Backend Toolchain to Java 17

- **Rationale**: Spring Boot 3.2.5 mandates Java 17 and remains compatible up to Java 22; the constitution and acceptance criteria explicitly call for a Java 17 toolchain, and Gradle toolchains let contributors use newer JDKs locally without breaking CI.
- **Alternatives considered**:
  - **Adopt Java 21 immediately**: Rejected because it violates acceptance criteria, increases plugin compatibility risk, and complicates CI images.

## Decision: Define Minimum P0 Test Gates (`./gradlew check`, `npm run test:unit`)

- **Rationale**: Running `./gradlew check` plus JaCoCo (>=50% line coverage) and `npm run test:unit -- --run --coverage` (>=60% statement coverage) enforces constitution-driven TDD while keeping the scaffold lightweight.
- **Alternatives considered**:
  - **Introduce Testcontainers/Cypress suites now**: Deferred to later phases to keep the bootstrap focused and avoid unnecessary blockers.

## Decision: Scaffold Audit Logging and Secrets Handling Up Front

- **Rationale**: Shipping actuator/security/data starters, JSON Logback configuration, JPA auditing, and an `AuditTrailService` stub—with secrets pulled from environment variables or optional `.env`—meets constitution gates without future rewrites.
- **Alternatives considered**:
  - **Defer audit/secrets until business logic exists**: Rejected to avoid non-compliance and duplicated effort.

## Decision: Standardize on MySQL 8.4.0 (or 8.0.39) Images

- **Rationale**: Aligns with constitution requirements and the Spring-managed MySQL connector, avoiding immature MySQL 9.x compatibility paths.
- **Alternatives considered**:
  - **Retain MySQL 9.2**: Rejected because support is experimental and conflicts with mandated stack.

## Decision: Use Gradle Convention Plugins for Multi-Module + OpenAPI Generation

- **Rationale**: Convention plugins in `buildSrc` centralize Spotless, Spring Boot, and OpenAPI tasks, and keeping generated sources in `build/generated` (git-ignored) guarantees reproducible builds tied to the OpenAPI spec.
- **Alternatives considered**:
  - **Maintain config per module / commit generated code**: Rejected for duplication and risk of stale artifacts.

## Decision: Scaffold Frontend with Vite 5, Vue 3.4, and Strict TypeScript

- **Rationale**: Vite + Vue + Pinia + Vitest with strict TypeScript, ESLint 9, and Prettier satisfies frontend readiness, isolates generated clients under `src/api/client`, and keeps lint/test scripts straightforward.
- **Alternatives considered**:
  - **Adopt Vue CLI or Nuxt**: Rejected as heavier than needed and inconsistent with the Vite requirement.

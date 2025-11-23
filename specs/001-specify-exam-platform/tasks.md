# Execution Tasks: Generalized Exam Item Bank & Paper Generation System

## Phase P0 – Foundations (Sprint 1)

### Priority P0

#### P0-01 — Bootstrap Backend/Frontend Scaffolding
- Priority: P0
- Phase: P0 – Foundations
- Sprint Recommendation: Sprint 1
- Status: in-progress
- Description: Initialize Gradle multi-module Spring Boot backend and Vite-based Vue frontend with shared OpenAPI client generation, linting, and formatting standards to satisfy constitution readiness.
- Acceptance Criteria:
  1. `backend/build.gradle.kts` defines Spring Boot 3.2 project with Java 17 toolchain, shared conventions, and OpenAPI generator plugin producing client artifacts consumed by frontend.
  2. `frontend/package.json` and Vite config compile Vue 3.4 SPA with TypeScript strict mode, ESLint, Prettier, and unit test harness (Vitest) executed via npm scripts.
  3. Repository-level lint/test commands (`./gradlew check`, `npm run test:unit`) succeed locally and in CI with documentation added to `quickstart.md`.
- Dependencies: —
- Estimated Effort: M
- Engineering Role: devops
- Risk Tags: integration-risk, delivery-risk
- Deliverables: `backend/build.gradle.kts`, `backend/settings.gradle.kts`, `frontend/package.json`, `frontend/vite.config.ts`, shared OpenAPI generator configuration, README updates
- Git Branch Naming: `feature/p0-P0-01-project-scaffold`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration (build pipeline), ci-smoke
- Definition of Done:
  - Gradle and Vite builds run green on local and CI.
  - Linting and formatting gates configured and documented.
  - OpenAPI generator job produces artifacts without manual edits.
  - Quickstart instructions updated with bootstrap commands.

#### P0-02 — Local Runtime Infrastructure Baseline
- Priority: P0
- Phase: P0 – Foundations
- Sprint Recommendation: Sprint 1
- Description: Provision docker-compose stack with MySQL 8, Redis 7, and MinIO stub plus Flyway baseline schema aligned to data-model constraints.
- Acceptance Criteria:
  1. `infrastructure/docker-compose.dev.yml` starts MySQL, Redis, MinIO with health checks, named volumes, and secrets loaded from `.env.example`.
  2. Flyway baseline migration creates `user_account`, `role`, `permission`, `subject`, `knowledge_point`, `question`, and other tables per `data-model.md` with partition-ready naming.
  3. Seed scripts insert reference data (roles, default admin) and Quickstart documents launch/teardown instructions.
- Dependencies: P0-01
- Estimated Effort: M
- Engineering Role: devops
- Risk Tags: integration-risk, operability-risk
- Deliverables: `infrastructure/docker-compose.dev.yml`, Flyway migration scripts, seed SQL, `.env.example`, Quickstart updates
- Git Branch Naming: `feature/p0-P0-02-runtime-baseline`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: integration (compose), migration tests, smoke
- Definition of Done:
  - `docker compose up` yields healthy services with logs persisted.
  - Flyway migrations run idempotently across local and CI.
  - Seed data verified through SQL query or repository call.
  - Quickstart includes troubleshooting for common startup issues.

### Priority P1

#### P0-03 — CI/CD Pipeline with Quality Gates
- Priority: P1
- Phase: P0 – Foundations
- Sprint Recommendation: Sprint 1
- Description: Configure GitHub Actions workflows enforcing TDD gates, security scans, and artifact caching for backend and frontend projects.
- Acceptance Criteria:
  1. `ci/github/workflows/build.yml` executes `./gradlew test`, Testcontainers integration suite, and publishes coverage ≥90% for domain packages.
  2. `ci/github/workflows/frontend.yml` runs `npm run test:unit`, `npm run lint`, and Cypress smoke suite using generated OpenAPI stubs.
  3. SAST/Dependency checks (OWASP Dependency Check or Trivy) publish reports and fail builds on high severity findings; workflow status documented in README badges.
- Dependencies: P0-01
- Estimated Effort: M
- Engineering Role: devops
- Risk Tags: delivery-risk, compliance-risk
- Deliverables: GitHub workflow YAMLs, coverage report upload config, security scan configs, badge markdown
- Git Branch Naming: `feature/p0-P0-03-ci-quality-gates`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: ci-smoke, security, integration
- Definition of Done:
  - CI pipelines run automatically on pull requests with required checks.
  - Failed quality gates block merges until resolved.
  - Coverage and security reports archived as workflow artifacts.
  - Documentation explains pipeline stages and failure escalation.

#### P0-04 — Observability Scaffolding
- Priority: P1
- Phase: P0 – Foundations
- Sprint Recommendation: Sprint 1
- Description: Implement structured logging, Prometheus `/actuator/prometheus` exposure, Grafana dashboard templates, and alert routing for autosave/assembly KPIs.
- Acceptance Criteria:
  1. Spring Boot exposes Prometheus metrics with custom timers for question search, assembly, and autosave placeholders; dashboards saved under `observability/`.
  2. Grafana provisioning config includes latency, error rate, and throughput panels aligned with constitution budgets.
  3. Alertmanager template routes SLA breaches to placeholder notification channel with runbook link in `docs/operations.md`.
- Dependencies: P0-02
- Estimated Effort: M
- Engineering Role: devops
- Risk Tags: perf-risk, operability-risk
- Deliverables: `backend/src/main/java/.../config/observability/*`, `observability/grafana/*.json`, `observability/alertmanager.yml`, `docs/operations.md`
- Git Branch Naming: `feature/p0-P0-04-observability-skeleton`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: integration (metrics scrape), load smoke
- Definition of Done:
  - Metrics endpoints validated via Prometheus scrape job.
  - Dashboards load without manual tweaks in local Grafana.
  - Alert routing tested with simulated breach event.
  - Operations doc references metric names and troubleshooting steps.

## Phase P1 – Identity & RBAC (Sprint 2)

### Priority P0

#### P1-01 — UserAccount and RBAC Persistence Layer
- Priority: P0
- Phase: P1 – Identity & RBAC
- Sprint Recommendation: Sprint 2
- Description: Implement JPA entities, repositories, and service layer for `UserAccount`, `Role`, `Permission` with Flyway migrations enforcing constraints from `data-model.md`.
- Acceptance Criteria:
  1. Entities map to schema with unique username/email, soft delete status, and auditing fields; repository tests cover create/read/update scenarios using Testcontainers.
  2. Service layer supports role assignment, invitation tokens, and audit log hooks per FR-001 and FR-015.
  3. Flyway migrations evolve schema without breaking baseline and include rollback notes.
- Dependencies: P0-02
- Estimated Effort: M
- Engineering Role: backend
- Risk Tags: security-risk, data-integrity-risk
- Deliverables: `backend/src/main/java/.../security/*`, Flyway migration scripts, Testcontainers-based unit/integration tests
- Git Branch Naming: `feature/p1-P1-01-rbac-domain`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, contract (OpenAPI schema alignment)
- Definition of Done:
  - Entity validation rules enforced via Bean Validation tests.
  - Repository/service coverage ≥90%.
  - Schema migrations reviewed and approved.
  - Audit events emitted for create/update/delete actions.

#### P1-02 — Authentication & Security Guardrails
- Priority: P0
- Phase: P1 – Identity & RBAC
- Sprint Recommendation: Sprint 2
- Description: Configure Spring Security with JWT issuance, Argon2id hashing, login throttling, session tracking, and audit logging aligned with constitution.
- Acceptance Criteria:
  1. `/api/v1/auth/login` and `/api/v1/auth/refresh` endpoints behave per `contracts/openapi.yaml`, returning JWT + refresh tokens with expiry metadata.
  2. Password hashing uses Argon2id with configurable parameters; lockout policy triggers after configurable failed attempts logged in `AuditLog`.
  3. Integration tests validate RBAC enforcement across admin/teacher/student roles and ensure TLS + CORS settings documented.
- Dependencies: P1-01
- Estimated Effort: M
- Engineering Role: backend
- Risk Tags: security-risk, compliance-risk
- Deliverables: `backend/.../security/config/*`, auth controllers, filter chain, integration tests, security documentation
- Git Branch Naming: `feature/p1-P1-02-auth-guardrails`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, security, contract
- Definition of Done:
  - Security regression suite green with coverage on critical paths.
  - JWT tokens validated by frontend mock client.
  - Audit logs recorded for login, logout, lockout events.
  - Security configuration documented in runbook.

### Priority P1

#### P1-03 — Admin RBAC Management UI
- Priority: P1
- Phase: P1 – Identity & RBAC
- Sprint Recommendation: Sprint 2
- Description: Build Vue admin pages for user provisioning, role assignment, and status management leveraging generated OpenAPI client.
- Acceptance Criteria:
  1. Admin pages list users with filters, support invite, lock/unlock, and role edit flows using FR-015 acceptance tests.
  2. API mutations enforce optimistic updates with snackbar feedback; error states follow design system.
  3. Cypress e2e tests cover invite + activation + role change; accessibility audit via axe-core passes.
- Dependencies: P1-02
- Estimated Effort: M
- Engineering Role: frontend
- Risk Tags: security-risk, ux-risk
- Deliverables: `frontend/src/pages/admin/*.vue`, Pinia stores, Cypress specs, UI documentation
- Git Branch Naming: `feature/p1-P1-03-admin-rbac-ui`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, accessibility
- Definition of Done:
  - UI passes design review with responsive breakpoints.
  - E2E scenarios run in CI.
  - Error handling documented for support team.
  - Localization strings stored in i18n resources.

## Phase P2 – Question Intake (Sprints 3–4)

### Priority P0

#### P2-01 — Question Domain & Versioning Services
- Priority: P0
- Phase: P2 – Question Intake
- Sprint Recommendation: Sprint 3
- Description: Implement `Question`, `QuestionRevision`, `QuestionAsset` services with validation rules, lifecycle transitions, and audit hooks.
- Acceptance Criteria:
  1. Domain services enforce status workflow DRAFT → PENDING_REVIEW → APPROVED → ARCHIVED with rule checks from spec FR-004.
  2. Media attachments stored via OSS gateway with checksum verification; failing uploads logged without breaking transaction.
  3. OpenAPI endpoints for CRUD align with `contracts/openapi.yaml` schemas; unit/integration tests cover validation errors and success paths.
- Dependencies: P1-02
- Estimated Effort: L
- Engineering Role: backend
- Risk Tags: data-integrity-risk, compliance-risk
- Deliverables: `backend/.../question/*`, migration scripts, OSS client adapter, service tests, contract tests
- Git Branch Naming: `feature/p2-P2-01-question-domain`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, contract
- Definition of Done:
  - Lifecycle tests prove transitions and audit logs.
  - Asset uploads verified against MinIO in Testcontainers.
  - API contract tests green with generated client.
  - Documentation describes versioning and rollback policy.

### Priority P2

#### P2-02 — Word Ingestion Pipeline
- Priority: P2
- Phase: P2 – Question Intake
- Sprint Recommendation: Sprint 3
- Description: Build Apache POI-based parser converting educator-supplied DOCX into question drafts with validation cues and quarantine handling.
- Acceptance Criteria:
  1. Batch ingestion CLI/API reads sample DOCX, extracts metadata, and creates Question drafts with attachments referencing OSS.
  2. Validation engine flags unsupported LaTeX/media and routes to quarantine queue for manual correction per edge case guidelines.
  3. Integration tests assert idempotent reprocessing and error logging; documentation lists supported template variations.
- Dependencies: P2-01
- Estimated Effort: L
- Engineering Role: backend
- Risk Tags: integration-risk, data-quality-risk
- Deliverables: `backend/.../question/import/*`, CLI runner, validation configs, integration tests, operator guide
- Git Branch Naming: `feature/p2-P2-02-word-import`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, regression
- Definition of Done:
  - DOCX fixtures stored in test resources and pass ingestion tests.
  - Quarantine queue accessible via admin API.
  - Error metrics surfaced in Prometheus.
  - Operator guide appended to `docs/operations.md`.

#### P2-03 — Review Workflow UI & Notifications
- Priority: P2
- Phase: P2 – Question Intake
- Sprint Recommendation: Sprint 4
- Description: Implement review dashboard for approvers with notification integration and audit trails.
- Acceptance Criteria:
  1. Vue dashboard lists pending questions with filters by subject, tags, and owner; actions transition states with confirmation dialogs.
  2. Notifications (email or in-app) dispatched on assignment and approval with audit log entry referencing `AuditLog` entity.
  3. Cypress tests verify end-to-end review acceptance scenario from spec User Story 1; manual QA checklist updated.
- Dependencies: P2-01
- Estimated Effort: M
- Engineering Role: frontend
- Risk Tags: compliance-risk, ux-risk
- Deliverables: `frontend/src/pages/review/*.vue`, notification service integration, Cypress specs, support documentation
- Git Branch Naming: `feature/p2-P2-03-review-ui`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, integration
- Definition of Done:
  - UI approved by stakeholders.
  - Notifications logged and testable in staging.
  - Accessibility audit passes key flows.
  - Support playbook updated with review workflow steps.

## Phase P3 – Question Bank UX (Sprints 4–5)

### Priority P1

#### P3-01 — Question Search & Filtering API
- Priority: P1
- Phase: P3 – Question Bank UX
- Sprint Recommendation: Sprint 4
- Description: Deliver indexed search endpoints with Redis caching to meet ≤100 ms p95 query target.
- Acceptance Criteria:
  1. Search endpoint implements filters for subject, difficulty, knowledge points, statuses, keywords; query plan uses covering indexes.
  2. Redis caching layer with invalidation on question updates reduces hot query latency to ≤100 ms p95 verified via Gatling test.
  3. Contract tests ensure response shape matches `openapi.yaml`; performance dashboard updated with search metrics.
- Dependencies: P2-01
- Estimated Effort: M
- Engineering Role: backend
- Risk Tags: perf-risk, scalability-risk
- Deliverables: `backend/.../question/search/*`, Redis cache config, performance test scripts, Grafana panels
- Git Branch Naming: `feature/p3-P3-01-search-api`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, perf
- Definition of Done:
  - Gatling report published with SLA evidence.
  - Cache invalidation covered by automated tests.
  - Metrics exported and visualised.
  - API documented with examples in OpenAPI file.

### Priority P2

#### P3-02 — Question Bank Management UI
- Priority: P2
- Phase: P3 – Question Bank UX
- Sprint Recommendation: Sprint 5
- Description: Build library views with advanced filters, previews, bulk actions, and inline metadata editing.
- Acceptance Criteria:
  1. UI renders search results with infinite scroll/pagination meeting UX requirements; filters sync with query params.
  2. Preview drawer displays rich content (LaTeX, media) using sanitized rendering; edit actions respect permissions.
  3. Cypress e2e covers search, filter, preview, bulk action flows; Lighthouse performance score ≥90.
- Dependencies: P3-01
- Estimated Effort: M
- Engineering Role: frontend
- Risk Tags: ux-risk, integration-risk
- Deliverables: `frontend/src/pages/questions/*.vue`, Pinia stores, Cypress specs, performance report
- Git Branch Naming: `feature/p3-P3-02-question-ui`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, accessibility, performance
- Definition of Done:
  - UX review sign-off recorded.
  - Tests run in CI with stable artifacts.
  - Error states documented for support.
  - Localization and theming verified.

## Phase P4 – Paper Authoring (Sprints 5–6)

### Priority P0

#### P4-01 — Paper Domain & Manual Assembly Services
- Priority: P0
- Phase: P4 – Paper Authoring
- Sprint Recommendation: Sprint 5
- Description: Implement `Paper`, `PaperVersion`, `PaperQuestion`, and constraint validation services supporting manual assembly and conflict detection.
- Acceptance Criteria:
  1. Domain services enforce version history, ownership, assembly mode flags, and conflict resolution validations from `data-model.md`.
  2. Manual assembly API supports creating, reordering, and removing questions with score balance checks.
  3. Integration tests validate version snapshots and audit events; determinism seeds stored when manual adjustments occur.
- Dependencies: P2-01
- Estimated Effort: L
- Engineering Role: backend
- Risk Tags: data-integrity-risk, perf-risk
- Deliverables: `backend/.../paper/*`, migration scripts, integration tests, contract updates
- Git Branch Naming: `feature/p4-P4-01-paper-domain`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, determinism
- Definition of Done:
  - Versioning unit tests capture snapshot behavior.
  - Conflict validation scenarios automated.
  - API documented with example payloads.
  - Seed retention verified in persistence tests.

### Priority P1

#### P4-02 — Manual Assembly UI & Preview
- Priority: P1
- Phase: P4 – Paper Authoring
- Sprint Recommendation: Sprint 6
- Description: Build Vue interface for manual paper assembly with drag-and-drop ordering, scoring feedback, and version comparison.
- Acceptance Criteria:
  1. Teachers can assemble papers, reorder items, and view real-time score totals with validation warnings when distribution violated.
  2. Version history modal compares revisions and surfaces change summary; exports trigger backend endpoints defined in openapi.
  3. Cypress tests emulate manual assembly workflow; usability testing feedback addressed.
- Dependencies: P4-01
- Estimated Effort: L
- Engineering Role: frontend
- Risk Tags: ux-risk, integration-risk
- Deliverables: `frontend/src/pages/papers/*.vue`, drag/drop components, Cypress specs, UX notes
- Git Branch Naming: `feature/p4-P4-02-manual-assembly-ui`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, accessibility
- Definition of Done:
  - UI reviewed with educator stakeholders.
  - Tests automated in CI with stable fixtures.
  - Error handling documented.
  - Localization resources updated.

#### P4-03 — Document Export Renderer
- Priority: P2
- Phase: P4 – Paper Authoring
- Sprint Recommendation: Sprint 6
- Description: Create rendering service generating Word/PDF exports preserving rich formatting via Apache POI and PDFBox (or equivalent).
- Acceptance Criteria:
  1. Export service produces DOCX/PDF outputs matching template fidelity including LaTeX render support and media embedding.
  2. Generated files stored via OSS with signed URL responses abiding by FR-009.
  3. Regression tests compare sample exports using checksum or visual diff baseline; manual QA checklist updated.
- Dependencies: P4-01
- Estimated Effort: M
- Engineering Role: backend
- Risk Tags: integration-risk, quality-risk
- Deliverables: `backend/.../paper/export/*`, export templates, regression tests, docs for template editing
- Git Branch Naming: `feature/p4-P4-03-export-renderer`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, regression (visual)
- Definition of Done:
  - Sample exports reviewed and approved by content team.
  - Automated regression suite passes.
  - Storage permissions validated.
  - Operator guide covers template updates.

## Phase P5 – MT19937 Auto Assembly (Sprints 7–8)

### Priority P0

#### P5-01 — MT19937 Assembly Engine
- Priority: P0
- Phase: P5 – MT19937 Auto Assembly
- Sprint Recommendation: Sprint 7
- Description: Implement deterministic assembly engine using Apache Commons RNG with constraint solver for question selection and seed logging.
- Acceptance Criteria:
  1. Engine satisfies subject/type/difficulty/knowledge ratios defined in FR-008, rejecting duplicates and recording seed + configuration snapshot.
  2. Determinism tests confirm identical outputs for same seed, while varying seeds produce non-overlapping selections across 100 runs.
  3. Performance tests demonstrate ≤200 ms p95 for target dataset size; conflict diagnostics returned when constraints unsatisfied.
- Dependencies: P4-01
- Estimated Effort: L
- Engineering Role: backend
- Risk Tags: perf-risk, determinism-risk
- Deliverables: `backend/.../paper/assembly/*`, deterministic test suite, performance reports, documentation of algorithm
- Git Branch Naming: `feature/p5-P5-01-mt19937-engine`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, load, determinism
- Definition of Done:
  - Determinism evidence captured in artifacts.
  - Performance metrics meet SLA.
  - Conflict reporting validated via automated tests.
  - Documentation explains seed management and audit trail.

### Priority P1

#### P5-02 — Auto Assembly UX & Conflict Resolution
- Priority: P1
- Phase: P5 – MT19937 Auto Assembly
- Sprint Recommendation: Sprint 7
- Description: Deliver teacher interface for configuring constraints, triggering auto assembly, reviewing results, and resolving conflicts.
- Acceptance Criteria:
  1. UI allows configuration of ratios, difficulty, knowledge points, exclusions with validation aligned to backend contract.
  2. Assembly preview displays deterministic seed, allows question swaps, and persists adjustments as new `PaperVersion` entries.
  3. Cypress tests cover success, conflict, and retry flows; determinism acceptance test ensures repeated assembly with same seed matches backend results.
- Dependencies: P5-01, P4-02
- Estimated Effort: L
- Engineering Role: frontend
- Risk Tags: perf-risk, determinism-risk
- Deliverables: `frontend/src/pages/papers/auto-assembly/*.vue`, API integration code, Cypress specs, UX documentation
- Git Branch Naming: `feature/p5-P5-02-auto-assembly-ui`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, determinism
- Definition of Done:
  - UI validated with user acceptance tests.
  - Determinism test comparing backend and frontend outputs passes.
  - Error handling patterns documented.
  - Localization and accessibility validated.

#### P5-03 — Assembly Performance & Regression Suite
- Priority: P1
- Phase: P5 – MT19937 Auto Assembly
- Sprint Recommendation: Sprint 8
- Description: Create Gatling/k6 load suite validating assembly SLA and regressions across data volumes.
- Acceptance Criteria:
  1. Performance suite runs 200 concurrent assembly requests using seeded datasets, reporting latency distribution under 200 ms p95.
  2. Regression job runs nightly with results pushed to observability dashboard; failures alert via Alertmanager.
  3. Documentation instructs engineers to extend datasets, seeds, and thresholds.
- Dependencies: P5-01
- Estimated Effort: M
- Engineering Role: qa
- Risk Tags: perf-risk, regression-risk
- Deliverables: `tests/backend/perf/assembly/*`, CI integration workflow, Grafana panels, documentation
- Git Branch Naming: `feature/p5-P5-03-assembly-perf-suite`
- Commit Prefix Recommendation: `[test]`
- Test Requirements: load, regression, determinism
- Definition of Done:
  - Load suite integrated into CI nightly schedule.
  - Alerts verified with test notification.
  - Reports stored for audit.
  - Contribution guide updated with steps to add scenarios.

## Phase P6 – Exam Delivery (Sprints 8–9)

### Priority P0

#### P6-01 — Exam Session & Autosave Backend
- Priority: P0
- Phase: P6 – Exam Delivery
- Sprint Recommendation: Sprint 8
- Description: Implement `ExamSession`, `ExamResponse`, `AutosaveSnapshot`, Redis buffer worker, and MySQL flush service per data model.
- Acceptance Criteria:
  1. Session lifecycle APIs handle start, resume, submit transitions enforcing status rules from `data-model.md` and FR-011/FR-013.
  2. Autosave service writes to Redis with TTL and asynchronous flush to MySQL, guaranteeing ≤500 ms p95 latency validated via k6 tests.
  3. Conflict detection handles offline retries, logging incidents and exposing admin diagnostics.
- Dependencies: P4-01, P5-01
- Estimated Effort: L
- Engineering Role: backend
- Risk Tags: reliability-risk, perf-risk
- Deliverables: `backend/.../exam/*`, Redis workers, k6 scripts, integration tests, runbook entries
- Git Branch Naming: `feature/p6-P6-01-exam-backend`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, load, reliability
- Definition of Done:
  - Autosave latency metrics meet SLA.
  - Redis failure scenarios covered in tests.
  - Audit logs record session events.
  - Runbook documents recovery procedures.

#### P6-02 — Student Exam SPA with Offline Recovery
- Priority: P0
- Phase: P6 – Exam Delivery
- Sprint Recommendation: Sprint 9
- Description: Build Vue exam client implementing autosave indicator, timer, reconnect logic, and secure asset delivery.
- Acceptance Criteria:
  1. SPA preloads questions/assets from secure URLs, enforces countdown timer with warnings, and surfaces autosave status.
  2. Offline mode caches responses locally, retries on reconnect, and reconciles with backend conflict responses.
  3. Cypress + Playwright tests simulate network loss and recovery; accessibility compliance for exam controls verified.
- Dependencies: P6-01
- Estimated Effort: L
- Engineering Role: frontend
- Risk Tags: reliability-risk, ux-risk
- Deliverables: `frontend/src/pages/exam/*.vue`, service workers/offline helpers, Cypress/Playwright specs, UX documentation
- Git Branch Naming: `feature/p6-P6-02-exam-spa`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, reliability, accessibility
- Definition of Done:
  - Offline recovery demo recorded and shared.
  - Tests run in CI with deterministic fixtures.
  - UX approved by pilot students.
  - Error handling guidance documented.

#### P6-04 — Autosave & Submission Resilience Testing
- Priority: P0
- Phase: P6 – Exam Delivery
- Sprint Recommendation: Sprint 9
- Description: Execute chaos and load scenarios validating autosave reliability, submission throughput, and failover readiness.
- Acceptance Criteria:
  1. Chaos tests simulate Redis outage, network jitter, and partial failures verifying recovery procedures and alerting.
  2. Load tests sustain 500 concurrent exam sessions with autosave intervals ≤30 seconds and 99% recovery for transient failures per SC-002.
  3. Findings logged with remediation issues; observability dashboards updated with exam KPIs.
- Dependencies: P6-01, P6-02
- Estimated Effort: M
- Engineering Role: qa
- Risk Tags: reliability-risk, perf-risk
- Deliverables: Chaos scripts, Gatling/k6 scenarios, incident report template, dashboard updates
- Git Branch Naming: `feature/p6-P6-04-exam-resilience`
- Commit Prefix Recommendation: `[test]`
- Test Requirements: load, chaos, reliability
- Definition of Done:
  - Chaos suite automated with scheduled runs.
  - SLA evidence archived for audits.
  - Alerts validated through fake incidents.
  - Remediation backlog updated with action items.

### Priority P1

#### P6-03 — Submission Pipeline & Grading Queue
- Priority: P1
- Phase: P6 – Exam Delivery
- Sprint Recommendation: Sprint 9
- Description: Orchestrate submission handling, objective auto-grading, queueing of subjective responses, and notification hooks.
- Acceptance Criteria:
  1. Submission API triggers objective grading using rules from spec FR-012, storing `ScoreRecord` entries and enqueuing subjective items.
  2. Grading queue exposes prioritization endpoints for teachers; notifications dispatched upon assignments.
  3. Integration tests validate concurrency, duplicate submissions, and audit logging; contract tests align with openapi.
- Dependencies: P6-01
- Estimated Effort: M
- Engineering Role: backend
- Risk Tags: reliability-risk, compliance-risk
- Deliverables: `backend/.../grading/*`, queue consumers, integration tests, notification templates
- Git Branch Naming: `feature/p6-P6-03-grading-pipeline`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, reliability
- Definition of Done:
  - Queue metrics exported to Prometheus.
  - Duplicate submission handling covered by automated tests.
  - Notifications verified in staging.
  - Operational guide for grading queue created.

## Phase P7 – Scoring & Analytics (Sprints 10–11)

### Priority P1

#### P7-01 — Objective Scoring & Subjective Grading UI
- Priority: P1
- Phase: P7 – Scoring & Analytics
- Sprint Recommendation: Sprint 10
- Description: Finalize objective scoring automation and deliver teacher interface for subjective grading workflow.
- Acceptance Criteria:
  1. Objective scoring implements rubric for choice questions with unit tests covering edge cases; manual override recorded in `ScoreRecord`.
  2. Grading UI surfaces queue with filters, enables rubric-based scoring, and persists comments per FR-012.
  3. E2E tests validate grade submission, override, and audit logging; accessibility checks pass for graders.
- Dependencies: P6-03
- Estimated Effort: L
- Engineering Role: full role? choose frontend? Since includes backend logic. maybe `backend`? but instructions restrict roles to specific. Could treat as `backend`? but includes UI. maybe choose `backend` or `frontend`. But apparently enumerated roles limited. Maybe we can pick `backend` or `frontend` whichever majority. I'd choose `backend`? But UI heavy. Maybe full? Could produce separate tasks? but we already advanced. For simplicity choose `backend`? Hmm but includes UI. Maybe create splitted tasks? but main check: we can assign `frontend`? But then backend part? Possibly we can note deliverables include both. I'd pick `backend`? Eh but unstoppable. maybe degrade to `frontend`? I'll pick `backend` because scoring logic critical; UI front part may be collaborative but we choose `backend`? uncertain. Maybe restful to pick `frontend`? But there is heavy UI. For clarity we might pick `frontend`? Eh watchers. I'd pick `backend` for best? hmm. I'd choose `backend` because scoring logic is extra critical. We'll include deliverables for UI also indicating cross-collab.
- Risk Tags: compliance-risk, ux-risk
- Deliverables: scoring services, UI components, tests, docs
- Git Branch Naming: `feature/p7-P7-01-grading-workflow`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, integration, e2e
- Definition of Done:
  - Objective scoring coverage ≥90%.
  - UI accepted by grading team.
  - Audit logs verified for overrides.
  - Training doc for graders published.

#### P7-02 — Analytics ETL & Snapshot Pipeline
- Priority: P1
- Phase: P7 – Scoring & Analytics
- Sprint Recommendation: Sprint 11
- Description: Build ETL jobs producing `AnalyticsSnapshot` records, aggregating metrics for dashboards with refresh ≤60 seconds.
- Acceptance Criteria:
  1. ETL leverages partitioned tables and incremental loads covering exam, subject, knowledge point scopes per `data-model.md`.
  2. Jobs scheduled via Spring Batch or Airflow integration, with monitoring metrics for duration and failures.
  3. Regression tests validate metric accuracy against fixture datasets; load tests ensure refresh SLA.
- Dependencies: P7-01, P3-01
- Estimated Effort: L
- Engineering Role: backend
- Risk Tags: perf-risk, data-quality-risk
- Deliverables: `backend/.../analytics/*`, batch job configs, regression test datasets, monitoring dashboards
- Git Branch Naming: `feature/p7-P7-02-analytics-etl`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: integration, perf, regression
- Definition of Done:
  - ETL pipeline documented with runbooks.
  - Monitoring dashboards display job health.
  - Data validation tests automated.
  - SLA evidence stored for review.

### Priority P2

#### P7-03 — Analytics Dashboards & Reporting UI
- Priority: P2
- Phase: P7 – Scoring & Analytics
- Sprint Recommendation: Sprint 11
- Description: Implement ECharts dashboards for score distributions, pass rates, high-error questions, and knowledge mastery drill-down.
- Acceptance Criteria:
  1. Dashboards render aggregated data with filters by cohort, subject, and time range; responses within 60 seconds per SC-004.
  2. Export capability provides CSV/PDF summaries aligning with FR-014.
  3. Cypress e2e tests verify visualization interactions; visual regression snapshots maintained.
- Dependencies: P7-02
- Estimated Effort: M
- Engineering Role: frontend
- Risk Tags: perf-risk, ux-risk
- Deliverables: `frontend/src/pages/analytics/*.vue`, chart config modules, Cypress specs, documentation
- Git Branch Naming: `feature/p7-P7-03-analytics-dashboards`
- Commit Prefix Recommendation: `[feat]`
- Test Requirements: unit, e2e, performance, regression
- Definition of Done:
  - Dashboards approved by stakeholders.
  - Visual regression suite stable in CI.
  - Export outputs validated by QA.
  - User guide updated with dashboard usage tips.

## Phase P8 – Hardening & Release (Sprint 12)

### Priority P0

#### P8-01 — Performance, Chaos, and Failover Drills
- Priority: P0
- Phase: P8 – Hardening & Release
- Sprint Recommendation: Sprint 12
- Description: Conduct end-to-end performance, chaos, and failover drills covering autosave, assembly, exam submission, and analytics workloads.
- Acceptance Criteria:
  1. Gatling, k6, and chaos scenarios executed against staging, demonstrating compliance with all SLAs documented in constitution.
  2. Failover tests (database failover, Redis restart, node drain) validated with documented recovery timelines.
  3. Incident postmortems captured with remediation tasks logged; dashboards updated with drill metrics.
- Dependencies: P6-04, P5-03
- Estimated Effort: L
- Engineering Role: qa
- Risk Tags: perf-risk, reliability-risk
- Deliverables: Performance scripts, chaos orchestration configs, drill reports, updated dashboards
- Git Branch Naming: `feature/p8-P8-01-hardening-drills`
- Commit Prefix Recommendation: `[test]`
- Test Requirements: load, chaos, reliability
- Definition of Done:
  - Drill evidence stored for audit.
  - Remediation backlog prioritized.
  - Alerts tuned based on drill findings.
  - Stakeholders sign off on resilience posture.

### Priority P1

#### P8-02 — Security Hardening & Compliance Closure
- Priority: P1
- Phase: P8 – Hardening & Release
- Sprint Recommendation: Sprint 12
- Description: Address pen-test findings, finalize ASVS Level 2 checklist, verify audit trails, and update compliance documentation.
- Acceptance Criteria:
  1. Security scan reports show zero open high-severity issues; mitigations documented with evidence.
  2. Threat model and ASVS checklist completed with reviewer sign-off; audit logs retained with sampling verification.
  3. Regression tests cover patched vulnerabilities; security runbook updated with response procedures.
- Dependencies: P7-03
- Estimated Effort: M
- Engineering Role: security? but not listed. Choose `devops`? or `backend`? Maybe `devops`.
- Risk Tags: security-risk, compliance-risk
- Deliverables: Security reports, checklist documents, updated runbooks, regression tests
- Git Branch Naming: `feature/p8-P8-02-security-hardening`
- Commit Prefix Recommendation: `[fix]`
- Test Requirements: security, regression, penetration
- Definition of Done:
  - Pen-test exit report signed off.
  - Security documentation stored in repo.
  - Regression suite green post fixes.
  - Compliance artifacts archived.

### Priority P1

#### P8-03 — Release Readiness & Runbooks
- Priority: P1
- Phase: P8 – Hardening & Release
- Sprint Recommendation: Sprint 12
- Description: Prepare final release notes, migration playbooks, deployment checklist, and UAT coordination.
- Acceptance Criteria:
  1. Release checklist includes rollback steps, database migration plan, feature toggle matrix, and ownership assignments.
  2. UAT plan executed with feedback triaged and documented; sign-off criteria met per spec success metrics.
  3. Runbooks cover incident response, backup restore, cache eviction, and exam-day operations referencing constitution.
- Dependencies: P8-01, P8-02
- Estimated Effort: M
- Engineering Role: documentation
- Risk Tags: delivery-risk, compliance-risk
- Deliverables: Release notes, migration playbook, UAT report, runbooks
- Git Branch Naming: `feature/p8-P8-03-release-readiness`
- Commit Prefix Recommendation: `[docs]`
- Test Requirements: operational-checklist, rehearsal
- Definition of Done:
  - Release assets reviewed by stakeholders.
  - UAT sign-off recorded.
  - Runbooks published to knowledge base.
  - Post-release monitoring plan approved.

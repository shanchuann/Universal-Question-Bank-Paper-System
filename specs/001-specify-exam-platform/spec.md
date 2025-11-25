# Feature Specification: Generalized Exam Item Bank & Paper Generation System

**Feature Branch**: `001-specify-exam-platform`  
**Created**: 2025-11-18  
**Status**: Draft  
**Input**: User description: "通用试题库组卷系统（Generalized Exam Item Bank & Paper Generation System）——构建多学科题库、智能组卷与在线考试的 B/S 架构平台"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Curate High-Quality Questions (Priority: P1)

An administrator or lead teacher records new questions with rich content, routes them through the review workflow, and publishes them to the shared bank for subsequent use.

**Why this priority**: Without a vetted, searchable question inventory the rest of the system cannot deliver value; this story unlocks all downstream capabilities.

**Independent Test**: Create and approve a sample question set with mixed media, verify audit logging, and confirm the entries appear in the searchable bank without consuming other features.

**Acceptance Scenarios**:

1. **Given** a logged-in administrator with creation rights, **When** they upload a Word file containing multi-format questions, **Then** the system parses question metadata, flags ambiguities, and queues items in "Pending Review" state.
2. **Given** a pending question in review, **When** an approver accepts it, **Then** the item transitions to "Approved", is timestamped with reviewer identity, and becomes searchable with its subject, knowledge point, and difficulty tags.

---

### User Story 2 - Assemble Exam Papers Efficiently (Priority: P2)

A teacher defines exam parameters, instructs the system to build a paper automatically with MT19937 randomness, optionally swaps individual questions, and exports the approved version.

**Why this priority**: Accelerates exam preparation and embodies the system promise of intelligent paper generation while leveraging the curated bank.

**Independent Test**: Supply assembly constraints, observe MT19937-based selection honoring ratios and difficulty, adjust one question manually, and export the paper without involving exam delivery.

**Acceptance Scenarios**:

1. **Given** a teacher selecting subject, type ratio, difficulty mix, target score, duration, and knowledge points, **When** they trigger automatic assembly, **Then** the generator returns a non-duplicated question set within 200 ms p95 and records the random seed for audit.
2. **Given** an assembled draft paper, **When** the teacher replaces a question and saves a new version, **Then** the change is versioned, the retired question is archived for traceability, and the preview reflects the update.

---

### User Story 3 - Deliver Robust Online Exams (Priority: P3)

A student authenticates with an issued paper code, completes the exam with real-time autosave, submits voluntarily or at timeout, and receives results once scoring completes.

**Why this priority**: Directly impacts learner experience and ensures the system can conduct assessments end-to-end.

**Independent Test**: Launch a proctored exam session with countdown and autosave, disconnect and reconnect to verify resilience, submit responses, and review stored answers and scoring outcomes.

**Acceptance Scenarios**:

1. **Given** a valid active paper code, **When** a student starts the exam, **Then** the system validates availability, starts the countdown, and preloads all question assets from secure sources.
2. **Given** a running exam session, **When** the autosave cycle hits 30 seconds or the student submits manually, **Then** responses are persisted with timestamps, objective questions are graded instantly, and subjective responses enter the grading queue for teachers.

---

### User Story 4 - Analyze Outcomes for Continuous Improvement (Priority: P4)

A department head reviews aggregated exam metrics to understand performance trends, high-error questions, and knowledge point mastery across cohorts.

**Why this priority**: Converts captured assessment data into actionable insights that justify system adoption.

**Independent Test**: Execute analytics queries for a completed exam cycle, inspect dashboards for score distributions and question error rates, and export summary reports without interacting with exam delivery.

**Acceptance Scenarios**:

1. **Given** completed exam sessions with stored answers, **When** analytics is generated, **Then** the dashboard visualizes score averages, pass rate, and distribution buckets with filters by class and subject.
2. **Given** the same dataset, **When** an administrator drills into a knowledge point, **Then** the system displays question-level error frequency and recommends review actions aligned with the curriculum.

### Edge Cases

- Automatic assembly is requested but the filtered question pool cannot satisfy the requested ratios or difficulty bands; system must return a descriptive conflict report and prevent publishing.
- A Word import includes malformed LaTeX or unsupported media; ingestion should quarantine the affected questions, notify the uploader, and preserve the rest.
- A student loses connectivity during an exam; autosave must queue retries, surface status to the student, and reconcile attempts when the link resumes without data loss.
- Simultaneous high-traffic exam sessions (≥500 concurrent users) occur during a maintenance window; platform must maintain SLA or gracefully decline new sessions with guidance.
- An administrator attempts to delete a question appearing in an upcoming scheduled exam; system should block deletion, require explicit override steps, and log the decision trail.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The platform MUST authenticate users and enforce RBAC roles for administrators, teachers, and students across all entry points.
- **FR-002**: The system MUST allow authorized staff to create, edit, and review questions with text, LaTeX, images, and multimedia attachments, capturing subject, knowledge point, difficulty tier, and scoring weight.
- **FR-003**: Bulk import MUST accept structured Word documents, parse question attributes automatically, surface validation errors, and support staged reprocessing.
- **FR-004**: Every question MUST follow the lifecycle states (Draft → Pending Review → Approved → Archived) with audit logs for reviewer decisions and timestamps.
- **FR-005**: Administrators MUST be able to soft-delete or restore questions with double confirmation and mandatory rationale logging while preserving historical usage references.
- **FR-006**: Users MUST be able to search and filter the question bank by subject, type, difficulty, knowledge point, keyword, creation date, and status, returning results within 100 ms p95.
- **FR-007**: Manual paper assembly MUST support question multi-select, ordering, difficulty balancing feedback, and per-question replacement prior to publishing.
- **FR-008**: Automatic paper assembly MUST utilize MT19937 to generate deterministic, auditable random sequences that satisfy subject, type ratio, difficulty, knowledge coverage, target score, and exam duration, rejecting duplicates.
- **FR-009**: The system MUST store generated papers with version history, allow draft previews, and enable exports to both Word and PDF preserving formatting.
- **FR-010**: Each paper MUST receive a unique identifier and activation window; only active identifiers may initiate exam sessions.
- **FR-011**: Online exams MUST provide countdown timers, ≤30-second autosave intervals, and visual status indicators for connectivity or save errors.
- **FR-012**: Objective questions MUST be graded automatically upon submission, while subjective responses MUST route to teacher grading queues with workload prioritization.
- **FR-013**: The platform MUST persist all responses, scoring events, and session logs for audit and retake purposes, supporting retention policies and secure retrieval.
- **FR-014**: Analytics MUST compute score distributions, pass rates, high-error questions, and knowledge mastery visualizations with drill-down capabilities.
- **FR-015**: System management MUST include user provisioning, role assignment, log inspection, configuration of backups, autosave intervals, and review thresholds.
- **FR-016**: The platform MUST expose documented APIs or data synchronization jobs for integration with campus information systems while honoring access controls.

### Non-Functional Requirements

- **NFR-001**: Core interactions (question search, form submissions) MUST respond within the prescribed budgets: ≤100 ms for queries, ≤200 ms for automatic assembly, ≤500 ms for autosave acknowledgments.
- **NFR-002**: The system MUST sustain at least 500 concurrent active exam participants with monthly availability ≥99.5%, supported by horizontal scaling and graceful degradation plans.
- **NFR-003**: All communication MUST adopt HTTPS with TLS 1.2+, enforce secure password hashing, implement input validation against SQL injection and XSS, and require two-step confirmation for destructive operations.
- **NFR-004**: Operational logs, audit trails, and security events MUST be retained for a minimum of one year with tamper-evident storage and searchable access controls.
- **NFR-005**: Automated and manual backups MUST execute according to configurable schedules and support point-in-time recovery tests at least quarterly.
- **NFR-006**: The architecture MUST accommodate future extension for new subjects, question types, and AI-driven personalization without breaking existing contracts or data schemas.

### Assumptions

- Existing institutional identity systems are out of scope; user accounts are managed within the platform with optional future connectors.
- Object storage endpoints for multimedia are provisioned and governed outside this project, with secure URLs supplied to the application.
- Regulatory requirements for data residency align with standard higher-education practices and do not demand specialized handling beyond stated security controls.

### Key Entities *(include if feature involves data)*

- **Question**: Represents an assessment item with content assets, difficulty tier, knowledge point tags, subject, approval status, and version lineage.
- **Paper**: Encapsulates an ordered set of questions, assembly parameters, MT19937 seed, total score, duration, version history, and activation windows.
- **ExamSession**: Tracks a student's interaction with a paper, including start/end times, autosave snapshots, submission status, device metadata, and connectivity events.
- **UserAccount**: Stores identity, role assignments, permissions, audit trail of actions, and security credentials per RBAC policies.
- **PerformanceMetric**: Aggregates analytics outputs such as score distributions, knowledge mastery indicators, question error rates, and reporting snapshots.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 95% of automated paper assemblies meeting valid constraints complete within 200 ms p95 and produce zero duplicate question selections across 100 consecutive runs.
- **SC-002**: 100% of active exam sessions maintain autosave intervals ≤30 seconds with a recovery success rate of 99% for transient connectivity losses during monitored drills.
- **SC-003**: Teachers reduce manual paper preparation time by 60% compared to baseline (as measured through user surveys and time tracking over the first full term).
- **SC-004**: Administrators can generate score distribution and knowledge mastery reports for any completed exam in under 60 seconds, with ≥90% satisfaction reported in post-launch stakeholder reviews.
- **SC-005**: Security audits confirm full compliance with TLS 1.2+, encrypted credential storage, and log retention policies, with zero high-severity findings per quarterly assessment.

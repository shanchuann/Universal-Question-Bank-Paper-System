# Data Model: Generalized Exam Item Bank & Paper Generation System

## Core Entities

### UserAccount

- **Purpose**: Represents any authenticated actor (administrator, teacher, student).
- **Key Fields**: `id (UUID)`, `username`, `email`, `password_hash`, `status (ACTIVE|LOCKED|INVITED)`, `display_name`, `created_at`, `updated_at`, `last_login_at`.
- **Relationships**: Many-to-many with `Role`; one-to-many with `AuditLog` (actor), `ExamSession` (student), `Paper` (owner), `Question` (creator/reviewer).
- **Constraints**: Unique username/email; passwords stored with Argon2id or bcrypt; soft delete via status.

### Role

- **Purpose**: RBAC definition for administrators, teachers, students.
- **Key Fields**: `id`, `code (ADMIN|TEACHER|STUDENT)`, `name`, `description`.
- **Relationships**: Many-to-many with `UserAccount`; many-to-many with `Permission` (optional extension).

### Permission *(optional extension for fine-grained RBAC)*

- **Purpose**: Enumerates discrete capabilities for policy enforcement.
- **Key Fields**: `id`, `code`, `description`.
- **Relationships**: Many-to-many with `Role`.

### Subject

- **Purpose**: Catalog of academic subjects.
- **Key Fields**: `id`, `code`, `name`, `description`, `is_active`.
- **Relationships**: One-to-many with `KnowledgePoint`, `Question`, `Paper`.

### KnowledgePoint

- **Purpose**: Hierarchical taxonomy for curriculum alignment.
- **Key Fields**: `id`, `subject_id`, `parent_id (nullable)`, `code`, `name`, `description`.
- **Relationships**: Many-to-many with `Question` via `QuestionKnowledgePoint`; used by analytics.

### Question

- **Purpose**: Canonical representation of a banked question.
- **Key Fields**: `id`, `subject_id`, `author_id`, `reviewer_id`, `type (SINGLE_CHOICE|MULTI_CHOICE|TRUE_FALSE|FILL_BLANK|SHORT_ANSWER)`, `stem_richtext`, `options_json`, `answer_schema`, `analysis_richtext`, `difficulty (EASY|MEDIUM|HARD)`, `score`, `status (DRAFT|PENDING_REVIEW|APPROVED|ARCHIVED)`, `version_number`, `approved_at`, `tags`, `media_bundle_id`.
- **Relationships**: Many-to-many with `KnowledgePoint`; one-to-many with `QuestionRevision`; referenced by `PaperQuestion` and `ExamResponse`.
- **Constraints**: Status workflow enforced; unique `(id, version_number)` pair; optional reviewer_id when approved.

### QuestionRevision

- **Purpose**: Maintains immutable snapshots each time a question changes.
- **Key Fields**: `id`, `question_id`, `version_number`, `payload_json`, `changed_by`, `changed_at`, `change_notes`.
- **Relationships**: Belongs to `Question`.

### QuestionAsset

- **Purpose**: Metadata for multimedia stored in OSS.
- **Key Fields**: `id`, `question_id`, `asset_type (IMAGE|AUDIO|VIDEO|LATEX)`, `oss_url`, `checksum`, `file_size`, `created_at`.
- **Relationships**: Belongs to `Question`.

### Paper

- **Purpose**: Logical container for an assessment owned by a teacher.
- **Key Fields**: `id`, `subject_id`, `owner_id`, `title`, `total_score`, `duration_minutes`, `assembly_mode (MANUAL|AUTO)`, `mt_seed`, `status (DRAFT|PUBLISHED|RETIRED)`, `current_version_id`, `created_at`, `updated_at`.
- **Relationships**: One-to-many with `PaperVersion`; one-to-many with `ExamSession`; references `Subject` and `UserAccount`.

### PaperVersion

- **Purpose**: Versioned snapshot of paper configuration.
- **Key Fields**: `id`, `paper_id`, `version_number`, `parameters_json (ratios, difficulty mix, knowledge coverage)`, `rendered_docx_uri`, `rendered_pdf_uri`, `created_at`, `created_by`.
- **Relationships**: One-to-many with `PaperQuestion`; belongs to `Paper`.

### PaperQuestion

- **Purpose**: Ordered association of questions within a paper version.
- **Key Fields**: `id`, `paper_version_id`, `question_id`, `question_version_number`, `position`, `score_override (nullable)`.
- **Constraints**: Unique `(paper_version_id, position)`; references frozen question version.

### AssemblyConstraint

- **Purpose**: Stores auto-assembly preferences for reproducibility.
- **Key Fields**: `id`, `paper_version_id`, `type_ratio_json`, `difficulty_distribution_json`, `knowledge_point_ids`, `excluded_question_ids`, `created_at`.
- **Relationships**: Belongs to `PaperVersion`.

### ExamSession

- **Purpose**: Represents a student sitting for a paper.
- **Key Fields**: `id`, `paper_version_id`, `student_id`, `access_code`, `start_at`, `end_at`, `status (IN_PROGRESS|SUBMITTED|FORFEITED|TIMEOUT)`, `time_remaining_seconds`, `autosave_interval_seconds`, `client_metadata_json`.
- **Relationships**: One-to-many with `ExamResponse`, `AutosaveSnapshot`, `ScoreRecord`; belongs to `PaperVersion` and `UserAccount`.

### ExamResponse

- **Purpose**: Stores submitted answers.
- **Key Fields**: `id`, `exam_session_id`, `question_id`, `question_version_number`, `response_payload`, `is_objective`, `objective_score`, `needs_manual_grade`, `manual_score`, `manual_graded_by`, `manual_graded_at`.
- **Constraints**: Unique `(exam_session_id, question_id)`; objective questions auto-scored.

### AutosaveSnapshot

- **Purpose**: Persisted copy of interim responses supporting recovery.
- **Key Fields**: `id`, `exam_session_id`, `snapshot_payload`, `captured_at`, `origin (CLIENT|SERVER)`, `sequence_number`.
- **Relationships**: Belongs to `ExamSession`; keyed by Redis for hot data, replayed into MySQL.

### ScoreRecord

- **Purpose**: Aggregates scoring outcomes per session.
- **Key Fields**: `id`, `exam_session_id`, `objective_score`, `subjective_score`, `total_score`, `graded_at`, `graded_by`, `status (PENDING|COMPLETED)`.
- **Relationships**: Belongs to `ExamSession`.

### AnalyticsSnapshot

- **Purpose**: Materialized views for faster dashboards.
- **Key Fields**: `id`, `scope (EXAM|SUBJECT|KNOWLEDGE_POINT)`, `reference_id`, `aggregation_window`, `metrics_json`, `computed_at`.
- **Relationships**: References `PaperVersion`, `Subject`, or `KnowledgePoint` based on scope.

### AuditLog

- **Purpose**: Immutable ledger of security-sensitive actions.
- **Key Fields**: `id`, `actor_id`, `action_type`, `resource_type`, `resource_id`, `payload_json`, `created_at`, `ip_address`.
- **Relationships**: Belongs to `UserAccount`.

### SystemConfig

- **Purpose**: Stores tunable parameters (autosave interval, backup schedule, review thresholds).
- **Key Fields**: `id`, `code`, `value`, `data_type`, `description`, `updated_at`, `updated_by`.

## Relationship Highlights

- A `Question` may appear in multiple `PaperVersion` entities via `PaperQuestion`, ensuring reuse while preserving historical versions.
- `ExamSession` references a specific `PaperVersion` to guarantee consistent question order and scoring criteria.
- `AutosaveSnapshot` provides high-frequency state stored in Redis first, with asynchronous flushing to durable storage for auditability.
- `AnalyticsSnapshot` leverages denormalized metrics to keep dashboard queries within the ≤100 ms budget.

## State Machines

- **Question Status**: `DRAFT → PENDING_REVIEW → APPROVED → ARCHIVED` (with optional rollback from APPROVED to DRAFT requiring justification).
- **Paper Lifecycle**: `DRAFT → PUBLISHED → RETIRED`; manual edits create new `PaperVersion` without mutating already published versions.
- **ExamSession Status**: `IN_PROGRESS → SUBMITTED` or `IN_PROGRESS → TIMEOUT/SUBMITTED (auto)`; manual proctor actions can mark `FORFEITED`.
- **ScoreRecord Status**: `PENDING → COMPLETED` once subjective grading is finished.

## Partitioning & Indexing

- Partition `Question` and `Paper` tables by `subject_id` and optionally creation date to manage large volumes.
- Index search-relevant fields: `(subject_id, type, difficulty)`, full-text index on `stem_richtext`, GIN/JSON index on `tags`.
- `ExamSession` partitioned by exam date to speed archival queries; indexes on `(paper_version_id, status)` and `(student_id, created_at)`.
- Redis keys follow `exam:session:{sessionId}:snapshot:{sequence}` convention to satisfy constitution naming guidance.

## Validation Rules

- `Question.score` must be >0 and align with paper total when assembled.
- Difficulty distribution per paper must comply with organization policy (e.g., 30/50/20 for E/M/H) enforced during assembly.
- `ExamSession.time_remaining_seconds` cannot exceed paper duration; server reconciles client claims with authoritative timestamps.
- Autosave payload size capped to protect Redis memory; server rejects oversize requests with actionable errors.
- All destructive actions recorded in `AuditLog` with IP and user agent for compliance.

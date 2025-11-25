# Universal Question Bank Paper System Constitution

## Core Principles

### I. Architecture Cohesion

- Anchor every service to the approved Spring Boot, Vue 3, MySQL, Redis, Nginx stack.
- Define module boundaries explicitly: question bank, paper assembly, examination, analytics, and shared infrastructure must communicate only through reviewed APIs.
- Enforce twelve-factor practices for configuration, scaling, and deployment to keep the B/S architecture predictable and portable.

### II. Security and Data Protection

- Require TLS 1.2 or higher for all transport, RBAC enforcement at every entry point, and least-privilege data access.
- Store passwords using strong adaptive hashing, log all administrative operations, and retain auditable logs for at least one year.
- Guard input and output paths against SQL injection and XSS, and require two-step confirmation for destructive actions.

### III. Test-First Delivery (Non-Negotiable)

- Follow TDD across features: capture requirements as executable tests, verify red-green-refactor completion, and reject merges without failing-first evidence.
- Maintain automated unit, integration, and contract suites with observable coverage for critical flows.
- Block releases when tests are flaky, missing, or bypassed without written risk acceptance.

### IV. Observability and Operability

- Instrument services with structured logging, metrics, and tracing that expose latency, error, and throughput baselines.
- Provide actionable dashboards and alerts for exam delivery, MT19937 auto-assembly, and persistence layers.
- Document operational runbooks for incident response, backup restore, and cache eviction strategies.

### V. Simplicity and Futureproofing

- Apply YAGNI and KISS: implement only validated requirements, keep domain models concise, and prefer configuration over custom code.
- Design extension points for new subjects, question types, and AI-assisted assembly without breaking existing contracts.
- Version APIs and schemas semantically, communicating changes with migration guidance before rollout.

## Technical Standards and Constraints

- **Technology Stack**: Backend Java 17+ with Spring Boot, frontend Vue 3 with TypeScript, persistence on MySQL 8 (InnoDB, utf8mb4), caching via Redis, static and exam traffic served by Nginx, object storage for media assets.
- **Performance Budgets**: Question searches must complete in <= 100 ms p95, automated paper assembly in <= 200 ms p95, exam autosave commits within <= 500 ms p95, and the system must sustain at least 500 concurrent users with 99.5 percent monthly availability.
- **Security Baseline**: Enforce HTTPS everywhere, adopt OWASP ASVS Level 2 controls, sanitize and validate all inputs, rotate credentials regularly, and store encryption keys in managed vaults. Every deletion requires confirmation and logging of actor, timestamp, and justification.
- **Data Management**: Model core tables (t_question, t_paper, t_exam_session, t_user, t_subject, t_knowledge_point) with normalized schemas, partition or shard high-volume tables, enforce referential integrity, and sync with campus systems via scheduled ETL connectors.
- **Content Handling**: Support rich text, LaTeX, images, and multimedia through validated upload pipelines with virus scanning. Store large assets in OSS with metadata references in MySQL.
- **API and Module Contracts**: Document RESTful endpoints using OpenAPI, require versioned namespaces, return consistent error envelopes, and forbid cross-module database access. Provide webhooks or polling endpoints for external integrations.
- **Automated Assembly Algorithm**: Implement MT19937-based selection to honor subject, type ratio, difficulty distribution, knowledge point coverage, total score, and duration constraints while preventing duplicates. Expose reproducible seeds for audits and version every change to the assembly heuristics.

## Delivery Workflow and Quality Gates

- **Work Item Intake**: Track requirements and defects in a shared backlog with definition of ready, including acceptance tests, risk notes, and MT19937 parameter specs when applicable.
- **Branching and Commits**: Use trunk-based or short-lived feature branches, follow conventional commits, and link commits to backlog items. No direct pushes to main without reviewed pull requests.
- **Code Review**: Require dual reviewer approval for core modules, verify alignment with this constitution, and mandate security and performance checklist completion before merge.
- **Testing Expectations**: Maintain minimum 90 percent unit coverage for domain services, 100 percent coverage of critical exam flows via integration tests, and stage environment smoke tests before release. Include load tests for assembly and exam submission paths each release cycle.
- **Continuous Delivery**: Enforce automated build, test, and security scanning pipelines. Deploy to staging via infrastructure-as-code, run regression suites, and require release owner sign-off before promoting to production.
- **Versioning and Migration**: Adopt semantic versioning for services and APIs, publish migration playbooks for schema or contract changes, and schedule backward-compatible grace periods before deprecations take effect.

## Governance

- This constitution supersedes team preferences, ad hoc practices, and legacy conventions; compliance is mandatory for all contributors and vendors.
- Any proposed amendment requires architectural review, stakeholder approval, documented impact analysis, and an implementation migration plan before adoption.
- Pull requests violating core principles, performance budgets, or security baselines must be rejected or remediated before merge.
- Each release owner confirms checklist adherence, including threat modeling updates, test evidence, and rollback readiness, prior to deployment.
- Maintain a living compliance checklist referenced in every pull request template and review meeting minutes quarterly to ensure sustained alignment.

**Version**: 1.0.0 | **Ratified**: 2025-11-18 | **Last Amended**: 2025-11-18

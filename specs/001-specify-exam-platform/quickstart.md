# Quickstart Guide: Generalized Exam Item Bank & Paper Generation System

## Prerequisites

- Java 17 LTS (Zulu or Temurin) with `JAVA_HOME` pointing at the 17 toolchain
- Node.js 22 LTS and pnpm 10+
- Docker Desktop with Docker Compose v2
- Git, Make (optional), and OpenSSL (for local TLS assets)
- MySQL 8.4.0 (provided via `docker-compose`) and Redis 7

## Repository Setup

1. Clone the repo and checkout `001-specify-exam-platform`.
2. Copy `infrastructure/.env.example` to `.env` at the repo root; fill placeholders for MySQL, Redis, MinIO, JWT secret, and mail credentials.
3. (Optional) Generate local TLS certificates: `make dev-cert` writes PEM files under `infrastructure/certs/` for Nginx and Vite HTTPS tests.
4. Pull base images: `docker compose -f infrastructure/docker-compose.dev.yml pull`.

## Start Shared Services

```bash
docker compose -f infrastructure/docker-compose.dev.yml up -d mysql redis minio nginx
```

- MySQL runs as `mysql:8.4.0` with a persistent volume `uqb-mysql-data`.
- Redis uses `redis:7.2` configured for persistence and key eviction alerts.
- MinIO and Nginx proxy certificates load secrets from the `.env` file.

Run `docker compose ps` to confirm all services report `healthy`.

## Backend Workflow (`backend/`)

1. Install dependencies: `./gradlew --version` downloads the Gradle wrapper if needed.
2. Verify formatting and tests: `./gradlew spotlessApply check` (includes JaCoCo with >=50% line coverage gate).
3. Generate OpenAPI interfaces: `./gradlew openApiGenerate` (outputs to `build/generated/openapi`).
4. Sync generated API stubs into the compiled source set: `./gradlew compileJava` (depends on `openApiGenerate`).
5. Launch the dev server: `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`.
6. Check actuator endpoints:
   - `http://localhost:8080/actuator/health`
   - `http://localhost:8080/actuator/prometheus`

Secrets such as datasource passwords and JWT keys are read from environment variables or `.env` via Spring configuration import.

## Frontend Workflow (`frontend/`)

1. Install dependencies: `pnpm install`.
2. Lint and test gates:
   - `pnpm lint`
   - `pnpm test:unit -- --run --coverage` (requires >=60% statement coverage)
3. Start the dev server: `pnpm dev -- --host --https` (serves on `https://localhost:5173`).
4. Regenerate API clients after backend spec changes: `pnpm api:sync` (calls the Gradle OpenAPI task and copies clients into `src/api/client`).

## Testing Summary

| Scope    | Command                                      | Notes |
|----------|----------------------------------------------|-------|
| Backend  | `./gradlew check`                            | Runs Spotless, unit tests, JaCoCo (>=50% line coverage) |
| Frontend | `pnpm test:unit -- --run --coverage`         | Vitest unit suite with >=60% statement coverage |
| Lint     | `./gradlew spotlessCheck` / `pnpm lint`      | Both commands run in CI and must pass before merge |

Future phases add Testcontainers integration tests, Cypress e2e, and contract suites; placeholders exist but are disabled by default.

## Observability & Audit Hooks

- Structured JSON logs are emitted via Logback (logstash encoder) and include request id, actor id, and session metadata.
- JPA auditing is enabled; implementing services should populate the shared `AuditTrailService` to record CRUD activity.
- Actuator endpoints expose `health`, `info`, `metrics`, and `auditevents`; secure them behind RBAC once security wiring lands.

## Shutdown & Reset

- Stop services: `docker compose -f infrastructure/docker-compose.dev.yml down`
- Reset volumes (destructive): `docker compose -f infrastructure/docker-compose.dev.yml down -v`

Refer back to `specs/001-specify-exam-platform/plan.md` for phase sequencing and additional tasks.

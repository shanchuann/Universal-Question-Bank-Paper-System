<p align="center">
	<img src="https://files.seeusercontent.com/2026/04/08/q8Ov/Visualization-for-Hexo.png" alt="Visualization for Hexo" style="max-width:720px; width:100%; height:auto;">
</p>

<p align="center">
	<strong>Tags:</strong>
	<span style="display:inline-block; margin-top:6px;">Java · Spring Boot · Vue 3 · TypeScript · OpenAPI · Docker · Ollama</span>
</p>

A full-featured platform for managing question banks, generating exam papers, running online practice/exams, automatic scoring, and AI-assisted teaching workflows.

Quick links: [backend](backend) · [frontend](frontend) · [openapi.yaml](openapi.yaml)

## Key Features
- Multi-type question bank management (CRUD)
- Paper generation by knowledge points, difficulty, and question types
- Online practice and exam flows with submission and scoring
- Role-based access and admin console
- Optional AI features (local Ollama): teacher/student assistant, subjective pre-grading

## Tech Stack
- Backend: Java 17+, Spring Boot, Gradle, JPA (PostgreSQL recommended)
- Frontend: Vue 3, TypeScript, Vite
- API: OpenAPI 3.0 (spec at repository root)

## Prerequisites
- Java 17+
- Node.js 18+ and npm 9+
- Docker & Docker Compose (for production)
- Ollama (optional, for AI features)

## Development

Backend (run locally):
```bash
cd backend
# Windows
./gradlew.bat bootRun
# macOS / Linux
./gradlew bootRun
```

Frontend (run locally):
```bash
cd frontend
npm ci
npm run generate-api
npm run dev
```

Run tests:
```bash
# Backend
cd backend && ./gradlew test
# Frontend
cd frontend && npm run test
```

## Build & Release (Docker)

This repo includes Dockerfiles for both services:
- Backend: `backend/Dockerfile` (builds a fat jar)
- Frontend: `frontend/Dockerfile` (builds static assets into an Nginx image)

Example local image build and tag (replace `<YOUR_OWNER>`):
```bash
docker build -f backend/Dockerfile -t ghcr.io/<YOUR_OWNER>/uqbank-backend:prod .
docker build -f frontend/Dockerfile -t ghcr.io/<YOUR_OWNER>/uqbank-frontend:prod .
```

Push to GitHub Container Registry (example):
```bash
echo $CR_PAT | docker login ghcr.io -u <YOUR_OWNER> --password-stdin
docker push ghcr.io/<YOUR_OWNER>/uqbank-backend:prod
docker push ghcr.io/<YOUR_OWNER>/uqbank-frontend:prod
```

## Production deployment (Docker Compose)

Use the provided production example: `deploy/docker-compose.prod.yml`. Typical workflow:
1. Prepare `deploy/.env.prod` with secrets and tags.
2. Build and publish backend/frontend images, or use CI to publish.
3. Start on the server:
```bash
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d
```

Example `deploy/.env.prod` (adjust values and secure secrets):
```env
GHCR_OWNER=your-ghcr-username-or-org
BACKEND_TAG=prod
FRONTEND_TAG=prod

# Database (recommended: external PostgreSQL)
DB_URL=jdbc:postgresql://db:5432/uqbank
DB_USERNAME=uqbank
DB_PASSWORD=CHANGE_ME

FLYWAY_ENABLED=true
AI_OLLAMA_ENABLED=false
AI_OLLAMA_BASE_URL=http://localhost:11434
AI_OLLAMA_MODEL=gemma4
TZ=Asia/Shanghai
```

Notes:
- `deploy/docker-compose.prod.yml` mounts `./uploads` to the backend. Ensure the host directory exists and is writable.
- The Compose file includes an optional `ollama` service and `watchtower` for automatic updates. Use `watchtower` with caution.
- Frontend Nginx configuration is at `deploy/nginx.prod.conf` and is mounted into the frontend container by the Compose file.

## Configuration (important environment variables)
- `DB_URL` — JDBC URL for PostgreSQL (e.g. `jdbc:postgresql://host:5432/uqbank`)
- `DB_USERNAME` / `DB_PASSWORD` — database credentials
- `FLYWAY_ENABLED` — enable Flyway migrations (true/false)
- `JPA_DDL_AUTO` — Hibernate DDL mode (validate/update)
- `SQL_INIT_MODE` — whether to run `data.sql` initial scripts
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS` — SMTP for emails
- `AI_OLLAMA_ENABLED`, `AI_OLLAMA_BASE_URL`, `AI_OLLAMA_MODEL` — Ollama settings

See `backend/src/main/resources/application.properties` for defaults and comments.

## Health check & monitoring
- The frontend Nginx config exposes a lightweight endpoint `/healthz` returning `ok`.
- Consider enabling Spring Actuator (secure access) for metrics and readiness checks.

## Troubleshooting
- Check logs: `docker compose -f deploy/docker-compose.prod.yml logs -f`
- Backend health: `curl http://localhost:8080/healthz`
- DB issues: verify `DB_URL` connectivity and credentials

## Contributing
- Run tests and linters before submitting PRs.
- Follow standard GitHub PR workflow.

For more service-specific information, see `backend/README.md` and `deploy/README.md`.

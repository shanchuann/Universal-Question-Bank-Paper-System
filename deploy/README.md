# Deployment (Docker Compose)

This folder contains example and helper files for deploying the system to a production-like environment using Docker Compose. The main Compose file is `docker-compose.prod.yml` in this folder.

Overview
- The stack consists of three main containers: `uqbank-backend`, `uqbank-frontend`, and optional `ollama` (AI). A `watchtower` service is included in the example for automatic updates — evaluate carefully before enabling in production.

Prerequisites
- Docker & Docker Compose on the target host
- A container registry (e.g. GitHub Container Registry) or CI that builds and pushes images
- A PostgreSQL instance (external or in Compose)

Prepare environment file
Create `deploy/.env.prod` and fill in secret values. Example:
```env
GHCR_OWNER=your-ghcr-username-or-org
BACKEND_TAG=prod
FRONTEND_TAG=prod

# Database
DB_URL=jdbc:postgresql://db:5432/uqbank
DB_USERNAME=uqbank
DB_PASSWORD=CHANGE_ME

FLYWAY_ENABLED=true
AI_OLLAMA_ENABLED=false
AI_OLLAMA_BASE_URL=http://localhost:11434
AI_OLLAMA_MODEL=gemma4
TZ=Asia/Shanghai
```

Build and publish images (example)
```bash
# Build backend image
docker build -f backend/Dockerfile -t ghcr.io/${GHCR_OWNER}/uqbank-backend:${BACKEND_TAG} .
# Build frontend image
docker build -f frontend/Dockerfile -t ghcr.io/${GHCR_OWNER}/uqbank-frontend:${FRONTEND_TAG} .

# Login and push (example using GHCR)
echo $CR_PAT | docker login ghcr.io -u ${GHCR_OWNER} --password-stdin
docker push ghcr.io/${GHCR_OWNER}/uqbank-backend:${BACKEND_TAG}
docker push ghcr.io/${GHCR_OWNER}/uqbank-frontend:${FRONTEND_TAG}
```

Start the stack
```bash
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d
```

Optional: include PostgreSQL in Compose
If you prefer a single Compose stack for small deployments or testing, add a `db` service and volumes. Example snippet to merge into `docker-compose.prod.yml`:
```yaml
	db:
		image: postgres:15
		environment:
			POSTGRES_DB: uqbank
			POSTGRES_USER: uqbank
			POSTGRES_PASSWORD: CHANGE_ME
		volumes:
			- db_data:/var/lib/postgresql/data

volumes:
	db_data:
	uploads:
```

Nginx and TLS
- The frontend container uses Nginx with configuration from `deploy/nginx.prod.conf`. For TLS, terminate TLS on a reverse proxy (e.g., Nginx/Traefik) in front of this Compose stack, or update `deploy/nginx.prod.conf` to add TLS settings and certificates.

Ollama (AI)
- The Compose file includes an `ollama` service for local AI features. If you enable `AI_OLLAMA_ENABLED=true`, ensure the Ollama model is pulled and accessible to the backend (see `AI_OLLAMA_BASE_URL`).

Watchtower and auto-updates
- The example includes `watchtower` configured to poll and update images. Use with care: automatic restarts may be disruptive for stateful services or database migrations.

Troubleshooting
- View logs: `docker compose -f deploy/docker-compose.prod.yml logs -f`
- Check running containers: `docker ps` and `docker compose -f deploy/docker-compose.prod.yml ps`
- Backend health: `curl http://localhost:8080/healthz` (from host or via proxy)
- Database connectivity: verify `DB_URL`, network access and credentials

Security recommendations
- Restrict access to administrative endpoints (Actuator) — only expose internally or protect with authentication.
- Use secrets manager or environment management in CI to avoid committing secrets to disk.

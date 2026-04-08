# Backend (Spring Boot)

This directory contains the Spring Boot backend service for the Universal Question Bank Paper System.

Quick summary
- Java 17+, Spring Boot, Gradle
- Default port: 8080
- Database: PostgreSQL (recommended)

Prerequisites
- Java 17+
- (Optional) Docker for container runs
- A running PostgreSQL instance for development or production

Run locally (development)
```bash
cd backend
# Windows
./gradlew.bat bootRun
# macOS / Linux
./gradlew bootRun
```

Build an executable JAR
```bash
cd backend
./gradlew clean bootJar -x test
# The built jar will be under build/libs/
```

Run the JAR
```bash
java -jar build/libs/your-app-name.jar
```

Run tests
```bash
cd backend
./gradlew test
```

Docker (build image)
```bash
# Run from repository root
docker build -f backend/Dockerfile -t ghcr.io/<YOUR_OWNER>/uqbank-backend:prod .
```

Configuration and environment variables
The backend reads most settings from `application.properties` and supports overriding via environment variables. Important variables:
- `DB_URL` — JDBC URL for the database (e.g. `jdbc:postgresql://db:5432/uqbank`)
- `DB_USERNAME` / `DB_PASSWORD` — DB credentials
- `FLYWAY_ENABLED` — enable Flyway migrations (true/false)
- `JPA_DDL_AUTO` — Hibernate DDL mode (`validate`/`update`)
- `SQL_INIT_MODE` — whether `data.sql` should be executed
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS` — SMTP for email
- `AI_OLLAMA_ENABLED`, `AI_OLLAMA_BASE_URL`, `AI_OLLAMA_MODEL` — Ollama AI settings

Database migrations
- Flyway migrations are in `classpath:db/migration/postgresql` and are controlled by `FLYWAY_ENABLED`.
- For production, set `FLYWAY_ENABLED=true` and ensure the DB user has proper privileges.

Initialization data
- `data.sql` may be used for initial data depending on `SQL_INIT_MODE` and `JPA_DEFER_DATASOURCE_INITIALIZATION` settings.

File uploads
- The backend expects an uploads directory at `/app/uploads` inside the container. In Compose it is mounted from `./uploads` on the host.

Logging
- Console logging is configured in `application.properties` (colorized output enabled by default).

Troubleshooting
- Check application logs for stack traces: `docker compose logs -f uqbank-backend` or `./gradlew bootRun` output
- Health endpoint: `GET /healthz` (via frontend proxy or backend port)

Where to find source code
- Java sources: `backend/src/main/java`
- Application properties: `backend/src/main/resources/application.properties`

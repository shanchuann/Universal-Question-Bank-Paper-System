# Universal Question Bank Paper System

A full-stack exam platform for question management, paper generation, online practice/exam, grading, and AI-assisted teaching.

## Tech Stack

- Backend: Java 17+, Spring Boot, Gradle, JPA
- Frontend: Vue 3, TypeScript, Vite
- API Contract: OpenAPI 3.0 (frontend client generated from spec)
- Testing: JUnit (backend), Vitest (frontend)

## Project Structure

- backend/: Spring Boot service
- frontend/: Vue application
- openapi.yaml: API contract
- uploads/: uploaded assets

## Core Features

- Authentication and role-based access
- Question bank CRUD with multiple question types
- Paper generation by knowledge point, difficulty, and type
- Online practice/exam with submission and scoring
- Analytics and admin management views

## AI Features (Ollama)

- Teacher assistant and student assistant chat
- AI subjective pre-grading for teacher workflows
- Floating AI assistant entry (draggable icon + draggable panel)
- Session history with 3-day retention and expiration notice
- Enter to send, Shift+Enter for newline
- Double-click conversation title to rename
- Drag question cards from practice page into AI context (practice routes only)
- Admin AI switches and model selection from local Ollama model list

## Prerequisites

- Java 17+
- Node.js 18+
- npm 9+
- Ollama (optional, for AI features)

## Quick Start

### Backend

```bash
cd backend
./gradlew.bat bootRun
```

Backend runs at http://localhost:8080.

Run tests:

```bash
./gradlew.bat test
```

### Frontend

```bash
cd frontend
npm install
npm run generate-api
npm run dev
```

Frontend runs at http://localhost:5173.

Build:

```bash
npm run build
```

## AI Setup (Optional)

1. Install and start Ollama.
2. Pull a model, for example:

```bash
ollama pull gemma2
```

3. Configure project root env values:

```env
AI_OLLAMA_ENABLED=true
AI_OLLAMA_BASE_URL=http://localhost:11434
AI_OLLAMA_MODEL=gemma4
AI_OLLAMA_TIMEOUT_MS=60000
```

4. Restart backend.

## API and Client

- OpenAPI spec: openapi.yaml
- Generated frontend client: frontend/src/api/generated/

## Development Notes

- Frontend lint/format: ESLint + Prettier
- Backend format: Spotless
- Run tests and build before submitting changes

## Contribution

Issues and pull requests are welcome.

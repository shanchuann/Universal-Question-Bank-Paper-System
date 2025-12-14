<p align="center">
   <img src="https://s2.loli.net/2025/11/25/vAmlECjbOaMWdif.png" alt="System Screenshot" width="600" />
</p>
<div align="center">
   <img src="https://img.shields.io/badge/project-exam--system-blue" />
   <img src="https://img.shields.io/badge/backend-spring--boot-green" />
   <img src="https://img.shields.io/badge/frontend-vue3-brightgreen" />
</div>
A comprehensive system for managing exam questions, generating papers, and conducting online exams.

## Project Structure

- `backend/` &mdash; Spring Boot application (Java 17+)
- `frontend/` &mdash; Vue 3 + TypeScript + Vite application
 
## Tech Stack

- **Backend**: Java 17+, Spring Boot, Gradle
- **Frontend**: Vue 3, TypeScript, Vite
- **API**: OpenAPI 3.0 (auto-generates frontend API client)
- **Testing**: JUnit (backend), Vitest (frontend)
- **Code Style**: ESLint (frontend), Spotless (backend)

## Main Features

- **User Authentication**: JWT login and permission management
- **Question Bank Management**: CRUD for questions, supports multiple types
- **Paper Generation**: Auto-generate papers by knowledge point, difficulty, and type
- **Online Exam**: Answering, auto-save, submission, and grading
- **Statistics & Analytics**: Score statistics, knowledge mastery analysis
- **Admin Panel**: Manage users, questions, papers, exams, etc.

## Getting Started

### Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- npm 9 or higher

### Backend

```bash
cd backend
./gradlew bootRun
# The backend server will start at http://localhost:8080
./gradlew test
```

### Frontend

```bash
cd frontend
npm install
npm run generate-api # If OpenAPI spec changed
# OpenAPI spec: ./openapi.yaml
# Generated code: frontend/src/api/generated/
npm run dev
# The frontend will be available at http://localhost:5173
npm run test:unit
npm run build
```

## API Documentation

- **OpenAPI specification:** [`openapi.yaml`](openapi.yaml)
- **Frontend API client** is auto-generated to ensure consistency between backend and frontend interfaces.

## Code Quality & Conventions

- Frontend uses **ESLint** and **Prettier** for code style.
- Backend uses **Spotless** for Java code formatting.
- Please run tests and formatting before submitting code.

## File Structure

- `backend/src/main/java/` &mdash; Backend core business logic
- `backend/src/main/resources/` &mdash; Backend configuration and templates
- `frontend/src/` &mdash; Frontend pages, components, state management, API client
 
---

## Contribution

Contributions are welcome via **Issues** or **Pull Requests**. Please follow the code style and include necessary explanations and tests.

---

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- npm 9 or higher

## Getting Started

### Backend

1. Navigate to the `backend` directory:

   ```bash
   cd backend
   ```

2. Run the application:

   ```bash
   ./gradlew bootRun
   ```

   The backend server will start at `http://localhost:8080`.

3. Run tests:

   ```bash
   ./gradlew test
   ```

### Frontend

1. Navigate to the `frontend` directory:

   ```bash
   cd frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Generate API client (if specs changed):

   ```bash
   npm run generate-api
   ```

4. Run the development server:

   ```bash
   npm run dev
   ```

   The frontend will be available at `http://localhost:5173`.

5. Run unit tests:

   ```bash
   npm run test:unit
   ```

6. Build for production:

   ```bash
   npm run build
   ```

## Features

- **User Authentication**: Login with JWT support.
- **Question Bank**: Browse and manage questions.
- **Paper Generation**: Automatically generate exam papers based on criteria.
- **Online Exam**: Take exams with auto-save and submission.

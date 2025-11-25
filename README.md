# Universal Question Bank & Paper System

A comprehensive system for managing exam questions, generating papers, and conducting online exams.

## Project Structure

- `backend/`: Spring Boot application (Java 17+)
- `frontend/`: Vue 3 + TypeScript + Vite application
- `specs/`: OpenAPI specifications

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

## API Documentation

The OpenAPI specification is located in `specs/001-specify-exam-platform/contracts/openapi.yaml`.

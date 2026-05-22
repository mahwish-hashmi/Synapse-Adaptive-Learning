# AI-Powered Adaptive Learning Platform

A production-grade EdTech backend built with Spring Boot — featuring JWT authentication,
role-based access, adaptive quiz engine, weak topic detection, and personalized learning paths.

---

## Tech Stack

| Layer       | Technology                                   |
|-------------|----------------------------------------------|
| Backend     | Spring Boot 3.4, Spring Security, Spring JPA |
| Auth        | JWT (JJWT 0.12.6)                            |
| Database    | PostgreSQL                                   |
| Docs        | Swagger / OpenAPI 3                          |
| Build       | Maven, Java 21                               |

---

## Features (by phase)

- **Phase 1 (current)** — JWT auth, role-based access (STUDENT / ADMIN), question CRUD, Swagger docs
- **Phase 2** — Quiz engine, attempt tracking, scoring
- **Phase 3** — Weak topic detection AI, mastery scoring
- **Phase 4** — Personalized learning paths, adaptive quiz, spaced repetition
- **Phase 5** — LLM integration, analytics dashboard, gamification
- **Phase 6** — Redis, Docker, CI/CD

---

## Getting started

### 1. Prerequisites
- Java 21
- PostgreSQL running locally
- Maven 3.9+

### 2. Set up environment variables

```bash
cp .env.example .env
# Edit .env with your actual database credentials and a strong JWT secret
```

### 3. Create the database

```sql
CREATE DATABASE questiondb;
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

### 5. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## API Overview

### Auth (public)
| Method | Endpoint               | Description        |
|--------|------------------------|--------------------|
| POST   | /api/v1/auth/register  | Register account   |
| POST   | /api/v1/auth/login     | Login, get JWT     |

### Questions (requires JWT)
| Method | Endpoint                                          | Role    |
|--------|---------------------------------------------------|---------|
| GET    | /api/v1/questions                                 | Any     |
| GET    | /api/v1/questions/{id}                            | Any     |
| GET    | /api/v1/questions/category/{category}             | Any     |
| POST   | /api/v1/questions                                 | ADMIN   |
| PUT    | /api/v1/questions/{id}                            | ADMIN   |
| DELETE | /api/v1/questions/{id}                            | ADMIN   |

---

## Project structure

```
src/main/java/com/telusko/quizapp/
├── config/          SecurityConfig, SwaggerConfig
├── controller/      AuthController, QuestionController
├── dto/
│   ├── request/     RegisterRequest, LoginRequest
│   └── response/    ApiResponse<T>, AuthResponse
├── entity/          User, Question
├── exception/       GlobalExceptionHandler, custom exceptions
├── repository/      UserRepository, QuestionRepository
├── security/        JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── service/         AuthService, QuestionService
```

---

## Environment variables

| Variable            | Description                        | Default (dev only)   |
|---------------------|------------------------------------|----------------------|
| DB_URL              | PostgreSQL JDBC URL                | localhost:5432/questiondb |
| DB_USERNAME         | Database username                  | postgres             |
| DB_PASSWORD         | Database password                  | —                    |
| JWT_SECRET          | HS256 signing key (min 32 chars)   | —                    |
| JWT_EXPIRATION_MS   | Token lifetime in milliseconds     | 86400000 (24h)       |

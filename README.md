<div align="center">

# Quill

**Spring Boot blog API with JWT auth, full-text search, and Redis-backed rate limiting**

[Quick Start](#quick-start) ·
[Architecture](#architecture) ·
[API](#api-endpoints) ·
[Contributing](#contributing)

</div>

<div align="center">

![Language](https://img.shields.io/badge/Java-25-blue)
![Framework](https://img.shields.io/badge/Spring_Boot-4.0.7-green)
![Database](https://img.shields.io/badge/PostgreSQL-18-orange)
![License](https://img.shields.io/badge/license-MIT-brightgreen)
![Docker](https://img.shields.io/badge/docker-ready-2496ED?logo=docker&logoColor=white)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)

</div>

---

## Overview

Quill is a blog platform API built with Spring Boot 4.0, Java 25, and PostgreSQL. It provides a comprehensive REST API for managing blog posts, categories, tags, comments, and user authentication with JWT.

### Goals

- Provide a production-ready blog API with modern Spring Boot practices
- Implement robust authentication and authorization with JWT
- Deliver high performance through caching, virtual threads, and database indexing

### Key Characteristics

- Stateless JWT authentication with refresh token rotation
- Full-text search via PostgreSQL tsvector with GIN index
- Redis-backed rate limiting with Bucket4j for API protection

---

# Table of Contents

- [Quick Start](#quick-start)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Development](#development)
- [Deployment](#deployment)
- [Observability](#observability)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [Roadmap](#roadmap)
- [License](#license)

---

# Quick Start

## Prerequisites

- JDK 25
- Docker Desktop (for PostgreSQL, Redis, Mailpit via Compose)
- Maven wrapper (`./mvnw`)

## Run Locally

```bash
# Clone
git clone https://github.com/nathsagar96/quill.git

# Enter project
cd quill

# Start dependencies (PostgreSQL 18, Redis 8, Mailpit)
docker compose up -d

# Set required JWT secret
export JWT_SECRET=$(openssl rand -base64 64)

# Run application
./mvnw spring-boot:run
```

## Verify

```bash
curl http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

# Features

## Core Features

- Blog post CRUD with DRAFT / SCHEDULED / PUBLISHED lifecycle
- Full-text search across post titles and bodies with AND/OR/phrase/exclusion syntax
- Category and tag management for content organization
- Comments on posts with ownership-based editing

## Security

- Stateless JWT authentication with access + refresh token rotation
- BCrypt password hashing and email verification flow
- Role-based access control (USER / ADMIN) with endpoint-level authorization

## Performance

- Virtual threads enabled for async request processing
- PostgreSQL full-text search via tsvector with GIN index
- ETag support for categories, tags, and posts

## Operations

- Scheduled post publishing via ShedLock (distributed lock on PostgreSQL)
- Redis-backed rate limiting with Bucket4j to prevent API abuse
- Structured JSON logging with correlation IDs and request tracing

---

# Technology Stack

| Layer | Technology | Version |
|---------|---------|---------|
| Runtime | Java | 25 |
| Framework | Spring Boot | 4.0.7 |
| Database | PostgreSQL | 18 |
| Cache | Redis | 8 |
| Authentication | JWT (JJWT) | 0.13 |
| API Docs | Springdoc OpenAPI | 3.0.3 |
| Monitoring | Micrometer + Prometheus | - |
| Testing | JUnit 5 + AssertJ + Mockito + Testcontainers | - |

---

# Architecture

```text
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│  API Gateway (CORS)  │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│   Security Filter    │
│   (JWT Validation)   │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│   Rate Limit Filter  │
│   (Bucket4j + Redis) │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│    Controllers       │
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐
│     Services         │
├────────┬─────────────┤
│  JPA   │   Events    │
└───┬────┴──────┬──────┘
    │           │
    ▼           ▼
┌────────┐ ┌──────────┐
│Postgres│ │  Redis   │
└────────┘ └──────────┘
    │
    ▼
┌──────────────────────┐
│   Flyway Migrations  │
└──────────────────────┘
```

## Request Flow

1. Request enters via CORS gateway (configured for `localhost:5173`)
2. `JwtAuthenticationFilter` validates JWT from `Authorization` header (skipped for public endpoints)
3. Rate limiting filter checks Bucket4j in Redis — returns 429 if exceeded
4. Controller dispatches to service layer inside a transactional boundary (`@Transactional(readOnly = true)`)
5. Services mutate loaded entities (dirty checking) and publish domain events
6. `@Async @TransactionalEventListener` handlers fire after commit for email notifications

---

# Project Structure

```text
.
├── src/
│   ├── main/java/com/quill/
│   │   ├── config/          # Jpa, Cache, Scheduling, Security, AppConfig
│   │   ├── controller/      # Auth, Post, Category, Tag, Comment, User
│   │   ├── dto/             # request/ and response/ Java records
│   │   ├── event/           # UserRegisteredEvent, PasswordResetRequestedEvent
│   │   ├── exception/       # Sealed ApplicationException hierarchy
│   │   ├── filter/          # JwtAuthenticationFilter, CacheControlFilter
│   │   ├── mapper/          # Handwritten @Component mappers
│   │   ├── model/           # Post, User, Category, Tag, Comment, enums
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── scheduler/       # PostScheduler (SCHEDULED → PUBLISHED)
│   │   ├── security/        # JwtService, SecurityConfig, CorsProperties
│   │   └── service/         # Business logic + EmailNotificationListener
│   ├── main/resources/
│   │   ├── db/migration/    # Flyway migrations (V1–V16)
│   │   └── application.yaml
│   └── test/                # Unit + integration tests
├── compose.yaml             # PostgreSQL, Redis, Mailpit
├── AGENTS.md                # AI agent instructions
└── pom.xml
```

## Important Directories

| Path | Purpose |
|--------|---------|
| `src/main/java/com/quill` | Application code |
| `src/main/resources/db/migration` | Flyway schema migrations |
| `src/test` | JUnit 5 + Testcontainers integration tests |
| `compose.yaml` | Docker Compose for local dev services |
| `AGENTS.md` | AI agent coding instructions |

---

# API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/auth/register` | Register a new user | Public |
| POST | `/api/auth/verify-email` | Confirm email address | Public |
| POST | `/api/auth/login` | Authenticate, get tokens | Public |
| POST | `/api/auth/refresh` | Refresh access token | Public |
| POST | `/api/auth/forgot-password` | Request password reset | Public |
| POST | `/api/auth/reset-password` | Reset password with token | Public |
| POST | `/api/auth/logout` | Invalidate refresh token | Authenticated |
| GET | `/api/posts` | List published posts | Public |
| GET | `/api/posts/search?q=` | Full-text search | Public |
| GET | `/api/posts/me` | List my posts | Authenticated |
| GET | `/api/posts/{id}` | Get post by ID | Public |
| GET | `/api/posts/slug/{slug}` | Get post by slug | Public |
| POST | `/api/posts` | Create a post | Authenticated |
| PUT | `/api/posts/{id}` | Update post (author) | Authenticated |
| DELETE | `/api/posts/{id}` | Delete post | Admin |
| GET | `/api/categories` | List categories | Public |
| GET | `/api/categories/{id}` | Get category | Public |
| POST | `/api/categories` | Create category | Authenticated |
| PUT | `/api/categories/{id}` | Update category | Authenticated |
| DELETE | `/api/categories/{id}` | Delete category | Admin |
| GET | `/api/tags` | List tags | Public |
| GET | `/api/tags/{id}` | Get tag | Public |
| POST | `/api/tags` | Create tag | Authenticated |
| PUT | `/api/tags/{id}` | Update tag | Authenticated |
| DELETE | `/api/tags/{id}` | Delete tag | Admin |
| GET | `/api/posts/{postId}/comments` | List comments | Authenticated |
| POST | `/api/posts/{postId}/comments` | Add comment | Authenticated |
| PUT | `/api/posts/{postId}/comments/{id}` | Edit comment (author) | Authenticated |
| DELETE | `/api/posts/{postId}/comments/{id}` | Delete comment | Admin |
| GET | `/api/users/me` | Get my profile | Authenticated |
| PUT | `/api/users/me` | Update my profile | Authenticated |

## Example Request

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "secret123"
}
```

## Example Response

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "dGhpcyBpcyBh...",
  "expiresIn": 86400
}
```

---

# Configuration

## Environment Variables

| Variable | Default | Required | Description |
|------------|------------|----------|-------------|
| `JWT_SECRET` | - | Yes | Base64-encoded HMAC-SHA signing key |
| `SPRING_PROFILES_ACTIVE` | - | No | Spring profile |
| `LOGGING_LEVEL_COM_QUILL` | INFO | No | Application log level |

## Application Properties

Key configuration values in `application.yaml` are auto-wired via `spring-boot-docker-compose` from `compose.yaml`. Static fallbacks point to `localhost:5432` (PostgreSQL) and `localhost:6379` (Redis).

---

# Development

## Build

```bash
./mvnw package
```

Output: `target/quill.jar`

## Run Tests

```bash
./mvnw test

# Single test class
./mvnw test -Dtest=PostServiceTest

# Single test method
./mvnw test -Dtest=PostServiceTest#createsAndReturns
```

## Lint

```bash
./mvnw spotless:apply
```

Formats code with Palantir Java Format (not bound to lifecycle — run manually).

## JaCoCo Coverage

```bash
./mvnw verify
```

Enforces 80% instruction / 70% branch coverage minimum.

---

# Deployment

## Docker

```bash
docker build -t quill .
docker run -e JWT_SECRET=... quill
```

## Production Checklist

- [ ] `JWT_SECRET` managed securely via secrets manager
- [ ] PostgreSQL backups configured
- [ ] Prometheus scraping enabled
- [ ] Rate limiting thresholds tuned
- [ ] TLS terminated at reverse proxy
- [ ] Load testing completed

---

# Observability

## Metrics

- JVM memory, threads, GC via Micrometer + Prometheus
- HTTP request duration and throughput
- Custom application metrics

## Logging

- Structured JSON logs
- Correlation IDs across requests
- Request tracing via MDC

## Health Checks

| Endpoint | Purpose |
|-----------|----------|
| `/actuator/health` | Liveness / readiness |
| `/actuator/info` | Build info |
| `/actuator/prometheus` | Prometheus metrics scrape |

---

# Troubleshooting

## Common Issues

### Application won't start — `JWT_SECRET` not set

```bash
export JWT_SECRET=$(openssl rand -base64 64)
./mvnw spring-boot:run
```

### Database connection issues

```bash
docker compose ps
docker compose logs postgres
# Verify: psql -h localhost -U quill -d quill
```

### Cache / Redis issues

```bash
docker compose logs redis
redis-cli -h localhost ping
# Should return: PONG
```

---

# Contributing

## Workflow

1. Fork repository.
2. Create feature branch.
3. Make changes.
4. Run tests (`./mvnw verify`).
5. Open pull request.

## Commit Style

```text
feat: add URL validation
fix: resolve cache invalidation bug
docs: update deployment guide
test: add integration coverage
```

---

# Roadmap

- [ ] Image upload support for posts
- [ ] RSS/Atom feed endpoints
- [ ] Admin dashboard API endpoints

---

# License

Licensed under the **MIT** license.

See [LICENSE](LICENSE) for details.

---

# Acknowledgments

- Spring Boot team for the framework
- PostgreSQL for the database
- Redis for caching and rate limiting

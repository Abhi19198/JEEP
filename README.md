# JEEP — Java Enterprise Engineering Platform

JEEP is a web application for managing organizations, teams, projects, and tasks in one place.

## Features

- **Identity** — Register and log in (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`). Passwords are hashed with BCrypt. Authentication uses JWT: access tokens expire after 15 minutes and refresh tokens after 7 days.
- **Organization** — Create, read, update, and delete organizations. Add members with roles (OWNER, ADMIN, MANAGER, MEMBER). Every service checks organization membership before allowing access to that organization's data.
- **Team** — Create teams inside an organization and list an organization's teams. Teams have members and roles.
- **Project** — Create, read, update, and delete projects within an organization. Project status: ACTIVE, ON_HOLD, ARCHIVED, COMPLETED.
- **Task** — Create, read, update, and delete tasks within a project, with an assignee. Priority: LOW, MEDIUM, HIGH, CRITICAL. Status: TODO, IN_PROGRESS, REVIEW, DONE (plus BLOCKED and CANCELLED).
- **Repository** — Link a GitHub, GitLab, Bitbucket, or Azure DevOps repository to a project. For a linked GitHub repository, JEEP shows recent commits, open pull requests, and contributors from the public GitHub API (`GET /api/v1/repositories/{repositoryId}/github/commits`, `.../pull-requests`, `.../contributors`). The calls are unauthenticated — no token is stored — so the public limit of 60 requests/hour applies and a clear error is returned when it is hit. Repositories linked as another provider return a "not supported for this provider" response instead of GitHub data; nothing is faked.
- **Notification** — Create and list notifications for the signed-in user. Types: TASK_UPDATE, COMMENT, MEMBER_ADDED, PROJECT_UPDATE, SYSTEM. Channels: EMAIL, IN_APP, SLACK, SMS. Each notification carries a read flag.
- **Analytics** — Organization summary at `GET /api/v1/analytics/organizations/{organizationId}`. The summary is cached in Redis for 10 minutes (see Caching below); membership is still checked against the database on every request.
- **AI** — Project suggestion at `POST /api/v1/ai/suggest`. This is a rule-based suggestion for now: it picks from a small set of canned recommendations based on the request. It is not a connection to a real AI model.
- **Live updates** — Task changes and notifications are pushed over STOMP/WebSocket (`/ws`): task events on `/topic/project/{projectId}`, notifications on `/topic/user/{userId}/notifications`. The socket authenticates with the JWT from the STOMP CONNECT frame and the server authorizes each subscription (project topics require organization membership; a user can only subscribe to their own notifications). The REST endpoints keep working unchanged and are the fallback when the socket is down.

## Caching (Redis)

- Only two things are cached, both cache-aside (read: cache first, miss → database; write: evict the related key):
  - Organization analytics summary — key `analytics:org:{organizationId}`, TTL 10 minutes. Evicted when a task in that organization is created, updated, or deleted.
  - Project task-list pages — key `tasks:project:{projectId}:s:{search}:p:{page}:z:{size}:sort:{sort}`, TTL 5 minutes. All pages of a project are evicted on any task write in that project.
- Nothing else is cached. Membership checks always run against the database first, so a cached value can never leak to a non-member.
- The cache fails open: if Redis is down or unreachable, reads behave as cache misses and writes/evictions are skipped, so the app keeps serving from the database. Redis is also excluded from the actuator readiness group for the same reason (it still appears in the main `/actuator/health`). Redis connection/command timeouts are 2 seconds so a dead Redis cannot stall requests.
- Redis runs as a service in `docker-compose.yml` and `docker-compose.prod.yml` (`redis:7.4-alpine`, AOF persistence, health-checked; the backend waits for it). Local overrides: `SPRING_DATA_REDIS_HOST` (default `localhost`, `redis` in compose) and `SPRING_DATA_REDIS_PORT` (default `6379`).
- No response-time numbers are claimed here: they were not measured against a live Redis/PostgreSQL setup.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 25 |
| Backend | Spring Boot 3.5.16 |
| Database | PostgreSQL 16 |
| Cache | Redis 7.4 (organization analytics + project task lists, fail-open) |
| Live updates | STOMP over WebSocket (`/ws`) |
| Migrations | Flyway |
| Auth | JWT (jjwt 0.12.6), BCrypt |
| API docs | SpringDoc OpenAPI |
| Monitoring | Spring Boot Actuator (health, metrics, Prometheus) |
| Frontend | React 19 + Vite |
| Containers | Docker, Docker Compose |

## Architecture

- Nine feature modules under `backend/src/main/java/com/jeep/`: `identity`, `organization`, `team`, `project`, `task`, `repository`, `notification`, `analytics`, and `ai`, plus `config` and `shared` support packages.
- Layered design in each module: controller → service → repository, with DTOs at the API boundary and JPA entities for persistence.
- Membership checks run in every service, so a user can only access data belonging to organizations they are a member of.
- A global exception handler (`shared/api/GlobalExceptionHandler.java`) turns errors into consistent API responses.
- The database schema is managed by Flyway migrations that run on backend startup; Hibernate runs in `validate` mode and never auto-updates the schema.
- The frontend is a React + Vite single-page app served by Nginx in its container. Nginx proxies `/api/` requests to the backend, and the app calls the API under `/api/v1`. Nginx also proxies `/ws/` to the backend with the `Upgrade` headers WebSocket needs.
- Live updates are delivered by `LiveUpdateListener` (in `websocket/`), which forwards `TaskChangedEvent` / `NotificationCreatedEvent` published by the task and notification services to the STOMP broker. Nothing else publishes to those topics.

## How to run

### Prerequisites

- Docker and Docker Compose

### Steps

1. Copy the example environment file and fill in the required values:

   ```bash
   cp .env.example .env
   ```

   Required in `.env`: `POSTGRES_PASSWORD`, `JEEP_SECURITY_JWT_SECRET` (at least 32 random characters), and `JEEP_CORS_ALLOWED_ORIGINS` (the exact frontend origin, without a trailing slash). `POSTGRES_USER` defaults to `jeep` and `VITE_API_BASE_URL` defaults to `/api/v1`. Never commit the real `.env` file.

2. Start everything:

   ```bash
   docker compose up -d --build
   ```

   This starts PostgreSQL (port 5432), Redis (port 6379), the backend (port 8080), and the frontend (port 5173). Flyway runs the database migrations when the backend starts.

3. Open the app at http://localhost:5173. Check backend readiness at http://localhost:8080/actuator/health/readiness. API documentation is available locally at http://localhost:8080/swagger-ui.html (the SpringDoc default path).

4. Stop everything with:

   ```bash
   docker compose down
   ```

### Production deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for the production setup: immutable SHA-tagged images, `docker-compose.prod.yml`, Caddy TLS, backups, and rollback.

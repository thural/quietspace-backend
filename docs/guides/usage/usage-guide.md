# QuietSpace Backend — Usage Guide

This guide explains how to start and stop the QuietSpace backend in **development mode**, using two approaches:

- **A. Minimal Docker** — run only the required infrastructure containers (MySQL + MailDev) and run the application locally with Gradle.
- **B. Full Docker Compose** — run the entire stack (database, backend, frontend, maildev) via Docker Compose.

It also documents every environment variable the application and infrastructure need.

> **Spring Boot does NOT auto-load the `.env` file** in this project. The line `spring.config.import: optional:file:.env` in `src/main/resources/application.yml` does not reliably populate these properties for a locally run JVM. The Docker Compose stack works because it passes `.env` via `env_file` (real OS environment variables). When running the app **outside** Docker (Option A), you must export the variables from `.env` into your shell before launching the app.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 25 | Provisioned automatically by the Gradle toolchain (`../../../build.gradle.kts`); no manual install needed. |
| Gradle | 9.6.1 | Use the included wrapper (`./gradlew`). |
| Docker | latest | Required for both options (containers). |
| Docker Compose | v2 (`docker compose`) | Required for Option B only. |

Verify the database is reachable and the `.env` file exists at the project root:

```bash
ls -l .env
docker ps   # confirms Docker daemon is running
```

---

## Environment variables (`.env`)

All variables live in the root `.env`. The currently committed `.env` already contains every value below.

### Variables consumed by the Spring Boot application

| Variable | Used in | Purpose | Required? |
|----------|---------|---------|-----------|
| `SPRING_PROFILES_ACTIVE` | `application.yml` | Active Spring profile. Use `dev`. Defaults to `dev` if unset. | Recommended |
| `SERVER_PORT_NUMBER` | host port mapping (compose) | Host port the backend is published on (container always listens on `8080`). | Optional |
| `ADMIN_PASSWORD` | `application.yml` (`spring.custom.admin-password`) | Password for the seeded admin account. | **Required** |
| `JWT_SECRET_KEY` | `application.yml` (`security.jwt.secret-key`) | HMAC secret for signing JWTs. | **Required** |
| `ACTIVATION_URL` | `application.yml` (`mailing.frontend.activation-url`) | URL embedded in account-activation emails. | Required for email activation |
| `MAILDEV_HOST` | `application.yml` (`spring.mail.host`) | SMTP host. No default → **startup fails if missing**. | **Required** |
| `MAILDEV_PORT` | `application.yml` (`spring.mail.port`) | SMTP port. | **Required** |
| `DB_HOST_NAME` | `application-dev.yml` (JDBC URL) | Database host. Defaults to `localhost`. | Optional |
| `DB_NAME` | `application-dev.yml` (JDBC URL) | Database/schema name. Defaults to `quietspace`. | Optional (compose needs it) |
| `DB_USER_USERNAME` | `application-dev.yml` (datasource) | MySQL application user. No default. | **Required** |
| `DB_USER_PASSWORD` | `application-dev.yml` (datasource) | MySQL application user password. No default. | **Required** |
| `FRONTEND_HOST` | `application-dev.yml` (`application.urls.frontend`) | Frontend host for CORS/link building. Defaults to `localhost`. | Optional |
| `FRONTEND_PORT` | `application-dev.yml` (`application.urls.frontend`) | Frontend port. Defaults to `3000`. | Optional |

### Variables consumed by the Docker / MySQL infrastructure

| Variable | Used in | Purpose |
|----------|---------|---------|
| `DB_ROOT_PASSWORD` | `docker-compose.yaml` → `MYSQL_ROOT_PASSWORD` | MySQL root password (container init). |
| `DB_NAME` | `docker-compose.yaml` → `MYSQL_DATABASE` | Database created on first start. |
| `DB_USER_USERNAME` | `docker-compose.yaml` → `MYSQL_USER` | MySQL non-root user created on first start. |
| `DB_USER_PASSWORD` | `docker-compose.yaml` → `MYSQL_PASSWORD` | Password for that user. |
| `DB_PORT_NUMBER` | `docker-compose.yaml` | Host port mapped to the container's `3306`. |
| `SERVER_PORT_NUMBER` | `docker-compose.yaml` | Host port mapped to the backend container's `8080`. |
| `MAILDEV_PORT` | `docker-compose.yaml` | Host port mapped to MailDev SMTP (`1025`). |

> The frontend and k8s/prod profiles reference additional variables (`DB_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`, `FRONTEND_URL`, `BACKEND_PORT`) that are **not** needed for dev mode. They are only used by `application-prod.yml` / `application-k8s.yml`.

---

## Option A — Minimal Docker (MySQL + MailDev) + local Gradle run

Use this when you want to develop/debug the application on your host machine and only need containers for the stateful services.

### 1. Start the MySQL container

```bash
docker run -d --name quietspace-dev-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=quietspace \
  -e MYSQL_USER=quietspace_user \
  -e MYSQL_PASSWORD=userpassword \
  -p 3306:3306 \
  mysql:8.0
```

Wait until it is ready (logs show `ready for connections`):

```bash
docker logs -f quietspace-dev-mysql
```

### 2. Start the MailDev container

```bash
docker run -d --name quietspace-dev-maildev \
  -p 1080:1080 \
  -p 1025:1025 \
  maildev/maildev
```

MailDev web UI: <http://localhost:1080> (captured emails). SMTP listens on `1025`, matching `MAILDEV_PORT`.

### 3. Export `.env` and run the application

```bash
cd /path/to/quietspace-backend
set -a && . ./.env && set +a   # export all variables into the shell
./gradlew bootRun
```

Expected output ends with:

```
Tomcat started on port 8080 (http) with context path '/'
Started QuietspaceApplication in ... seconds
```

### 4. Stop (Option A)

Stop the application with `Ctrl+C`, then stop/remove the containers:

```bash
docker stop quietspace-dev-mysql quietspace-dev-maildev
# optional cleanup:
docker rm quietspace-dev-mysql quietspace-dev-maildev
```

---

## Option B — Full Docker Compose stack

Runs database, backend, frontend, and MailDev together. The backend image is built from `infrastructure/docker/Dockerfile` (multi-stage, JDK 25 build → JRE 25 runtime).

### 1. Prepare the environment file

The Compose file references `./.env` **relative to the Compose file directory** (`infrastructure/docker/.env`). Copy the root `.env` there so Compose can read it:

```bash
cp .env infrastructure/docker/.env
```

### 2. Create the external Docker network

The Compose file attaches services to an **external** network `monolith-network` that must pre-exist:

```bash
docker network create monolith-network
```

### 3. Start the stack

Run from the project root so the build context (`../..`) resolves correctly:

```bash
docker compose -f infrastructure/docker/docker-compose.yaml up -d --build
```

Add the dev override (enables JDWP debug port `5005`) if desired:

```bash
docker compose -f infrastructure/docker/docker-compose.yaml \
               -f infrastructure/docker/docker-compose.override.yaml up -d --build
```

### 4. Verify

```bash
docker compose -f infrastructure/docker/docker-compose.yaml ps
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs
```

### 5. Stop (Option B)

```bash
docker compose -f infrastructure/docker/docker-compose.yaml down
# keep the database volume data:
#   docker compose -f infrastructure/docker/docker-compose.yaml down
# remove everything including volumes (DESTROYS data):
#   docker compose -f infrastructure/docker/docker-compose.yaml down -v
```

---

## API documentation (live while running)

| Type | UI view | Raw JSON |
|------|---------|----------|
| REST (springdoc/OpenAPI) | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| WebSocket (Springwolf/AsyncAPI) | `http://localhost:8080/springwolf/asyncapi-ui.html` | `http://localhost:8080/springwolf/docs` |

---

## Troubleshooting

- **`Could not resolve placeholder 'MAILDEV_HOST'`** — you ran the app via Gradle without exporting `.env`. Use `set -a && . ./.env && set +a` first (see Option A, step 3).
- **Port `3306` already allocated** — a MySQL container is already running (e.g. `quietspace-monolith-db`). Stop the conflicting container or reuse it; do not start a second one on the same host port.
- **`network monolith-network not found`** — create it with `docker network create monolith-network` before `docker compose up`.
- **Compose cannot find `.env`** — ensure `infrastructure/docker/.env` exists (copied from the project root).
- **Database connection refused** — confirm the MySQL container is healthy and that `DB_USER_USERNAME` / `DB_USER_PASSWORD` match what was used to start it.

# Configuration Guide

## Overview

QuietSpace uses a layered configuration system with `.env` as the single source of truth for environment-specific values. Configuration files are split by profile (dev/prod) to handle the differences between local development and Docker/production environments.

## Configuration Files

| File | Purpose | Environment |
|---|---|---|
| `.env` | Environment variables (single source of truth) | Both |
| `application.yml` | Base configuration (shared) | Both |
| `application-dev.yml` | Local development overrides | Dev only |
| `application-prod.yml` | Docker/production overrides | Prod only |

## Configuration Flow

```
                    ┌─────────────────┐
                    │     .env        │
                    │ (env variables) │
                    └────────┬────────┘
                             │
               ┌──────────────┴──────────────┐
               ▼                             ▼
     ┌─────────────────┐           ┌─────────────────┐
     │ application.yml │           │ application.yml │
     │   (base config) │           │   (base config) │
     └────────┬────────┘           └────────┬────────┘
              │                             │
              ▼                             ▼
     ┌─────────────────┐           ┌─────────────────┐
     │ application-    │           │ application-    │
     │   dev.yml       │           │   prod.yml      │
     └─────────────────┘           └─────────────────┘
              │                             │
              ▼                             ▼
     ┌─────────────────┐           ┌─────────────────┐
     │  Local Dev      │           │  Docker/Prod    │
     │  (localhost)    │           │  (service names)│
     └─────────────────┘           └─────────────────┘
```

## Environment Variables (.env)

### Setup

```bash
cp .env.example .env   # create from template
# edit .env with your values
```

**Mode switching:** Edit `SPRING_PROFILES_ACTIVE` in `.env` to toggle between `dev` and `prod`. The dev profile hardcodes most hostnames/port values in `application-dev.yml`, so only a few `.env` values are read in dev mode.

| `.env` variable | Read in dev mode? | Read in prod mode? |
|---|---|---|
| `ADMIN_PASSWORD` | Yes | Yes |
| `JWT_SECRET_KEY` | Yes | Yes |
| `DB_USER_USERNAME` | Yes | Yes |
| `DB_USER_PASSWORD` | Yes | Yes |
| `SPRING_PROFILES_ACTIVE` | Yes | Yes |
| All other variables | No (overridden by dev profile) | Yes |

### Database Configuration

| Variable | Description | Dev Value | Prod Value | Sync Required |
|---|---|---|---|---|
| `DB_ROOT_PASSWORD` | MySQL root password | `rootpassword` | `rootpassword` | Must match `MYSQL_ROOT_PASSWORD` in docker-compose.yaml |
| `DB_NAME` | Database name | `quietspace` | `quietspace` | Must match `MYSQL_DATABASE` in docker-compose.yaml |
| `DB_USER_USERNAME` | Database username | `quietspace_user` | `quietspace_user` | Must match `MYSQL_USER` in docker-compose.yaml |
| `DB_USER_PASSWORD` | Database password | `userpassword` | `userpassword` | Must match `MYSQL_PASSWORD` in docker-compose.yaml |
| `DB_PORT_NUMBER` | MySQL port | `3306` | `3306` | Must match docker-compose.yaml port mapping |
| `DB_HOST_NAME` | MySQL hostname | `localhost` | `quietspace-monolith-db` | Must match docker service name in prod |

**Important:** `DB_HOST_NAME` is hardcoded to `localhost` in `application-dev.yml` and read from `.env` in `application-prod.yml`. Do NOT change `DB_HOST_NAME` in `.env` without updating docker-compose.yaml.

### Spring Application Configuration

| Variable | Description | Dev Value | Prod Value | Sync Required |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | `prod` | Must match the profile you want to use |
| `ADMIN_PASSWORD` | Admin user password | `admin123` | `admin123` | Must be set in `.env` |
| `JWT_SECRET_KEY` | JWT signing key | (set in .env) | (set in .env) | Must be a valid base64-encoded key |
| `ACTIVATION_URL` | Account activation URL | `http://localhost:3000/activate` | `http://localhost:3000/activate` | Must match frontend URL |
| `SERVER_PORT_NUMBER` | Application port | `8080` | `8080` | Must match docker-compose.yaml port mapping |

### Frontend Configuration

| Variable | Description | Dev Value | Prod Value | Sync Required |
|---|---|---|---|---|
| `FRONTEND_HOST` | Frontend hostname | `localhost` | `localhost` | Used for generating URLs |
| `FRONTEND_PORT` | Frontend port | `3000` | `3000` | Used for generating URLs |

### MailDev Configuration

| Variable | Description | Dev Value | Prod Value | Sync Required |
|---|---|---|---|---|
| `MAILDEV_HOST` | Mail server hostname | `localhost` | `mailpit` | Must match docker service name in prod |
| `MAILDEV_PORT` | Mail server port | `1025` | `1025` | Must match docker-compose.yaml port mapping |

## Profile-Specific Configuration

### Development Profile (application-dev.yml)

| Setting | Value | Purpose |
|---|---|---|
| `datasource.url` | `jdbc:mysql://localhost:3306/quietspace` | Hardcoded for local MySQL |
| `mail.host` | `localhost` | Hardcoded for local MailDev |
| `mail.port` | `1025` | Hardcoded for local MailDev |
| `jpa.hibernate.ddl-auto` | `update` | Auto-update schema |
| `flyway.enabled` | `false` | Disable migrations (use ddl-auto) |
| `server.port` | `8080` | Fixed port |
| `devtools.livereload` | `true` | Enable live reload |
| `logging.level.root` | `INFO` | Logging level |

### Production Profile (application-prod.yml)

| Setting | Value | Purpose |
|---|---|---|
| `datasource.url` | `jdbc:mysql://${DB_HOST_NAME}:${DB_PORT_NUMBER}/${DB_NAME}` | Read from .env |
| `jpa.hibernate.ddl-auto` | `validate` | Validate schema only |
| `flyway.enabled` | `true` | Enable migrations |
| `flyway.baseline-on-migrate` | `true` | Baseline on first migration |
| `hikari.maximum-pool-size` | `5` | Connection pool size |
| `server.port` | `${SERVER_PORT_NUMBER}` | Read from .env |

## Sync Requirements

### Critical Sync Points

These values MUST be kept in sync across files to avoid connection failures:

#### 1. Database Credentials

| Location | Variable | Must Match |
|---|---|---|
| `.env` | `DB_USER_USERNAME` | `DB_USER_USERNAME` in `.env` |
| `.env` | `DB_USER_PASSWORD` | `DB_USER_PASSWORD` in `.env` |
| `docker-compose.yaml` | `MYSQL_USER` | `DB_USER_USERNAME` in `.env` |
| `docker-compose.yaml` | `MYSQL_PASSWORD` | `DB_USER_PASSWORD` in `.env` |
| `application-dev.yml` | `datasource.username` | `DB_USER_USERNAME` in `.env` |
| `application-dev.yml` | `datasource.password` | `DB_USER_PASSWORD` in `.env` |
| `application-prod.yml` | `datasource.username` | `DB_USER_USERNAME` in `.env` |
| `application-prod.yml` | `datasource.password` | `DB_USER_PASSWORD` in `.env` |

#### 2. Database Hostname

| Location | Variable | Must Match |
|---|---|---|
| `.env` | `DB_HOST_NAME` | Docker service name `quietspace-monolith-db` |
| `application-prod.yml` | `datasource.url` | Uses `${DB_HOST_NAME}` from `.env` |
| `application-dev.yml` | `datasource.url` | Hardcoded `localhost` (not from `.env`) |

#### 3. Mail Server

| Location | Variable | Must Match |
|---|---|---|
| `.env` | `MAILDEV_HOST` | Docker service name `mailpit` |
| `application.yml` | `spring.mail.host` | Uses `${MAILDEV_HOST}` from `.env` |
| `application-dev.yml` | `spring.mail.host` | Hardcoded `localhost` (overrides base) |

#### 4. Ports

| Location | Variable | Must Match |
|---|---|---|
| `.env` | `DB_PORT_NUMBER` | `3306` in docker-compose.yaml |
| `.env` | `SERVER_PORT_NUMBER` | `8080` in docker-compose.yaml |
| `.env` | `MAILDEV_PORT` | `1025` in docker-compose.yaml |
| `.env` | `FRONTEND_PORT` | `3000` (used in application.yml) |

### Common Configuration Errors

#### Error: Communications link failure

**Cause:** `DB_HOST_NAME` in `.env` doesn't match the Docker service name.

**Fix:**
```bash
# In .env, ensure:
DB_HOST_NAME=quietspace-monolith-db

# In application-dev.yml, ensure:
url: jdbc:mysql://localhost:3306/quietspace
```

#### Error: Authentication failed for user

**Cause:** Database credentials in `.env` don't match what MySQL was initialized with.

**Fix:**
```bash
# Ensure these match in .env:
DB_USER_USERNAME=quietspace_user
DB_USER_PASSWORD=userpassword

# If MySQL was already initialized, reset:
docker volume rm quietspace_monolith_data
docker compose -f docker-compose.yaml up -d
```

#### Error: Mail server connection refused

**Cause:** `MAILDEV_HOST` in `.env` doesn't match the Docker service name.

**Fix:**
```bash
# In .env, ensure:
MAILDEV_HOST=mailpit

# For local dev (not in Docker), use:
MAILDEV_HOST=localhost
```

#### Error: Port already in use

**Cause:** Port in `.env` conflicts with another service.

**Fix:**
```bash
# Check what's using the port:
lsof -i :3306

# Change port in .env:
DB_PORT_NUMBER=3307
```

## Docker Compose Configuration

The project uses `docker-compose-dev.yml` at the project root for local development (MySQL, RabbitMQ, Mailpit). A full production stack is defined in `docker-compose.yaml` (to be created for production deployments).

### Service Dependencies

```
quietspace-monolith-db (MySQL)
        │
        ▼ (service_healthy)
quietspace-monolith (Spring Boot)
        │
        ├──▶ quietspace-frontend
        └──▶ mail-dev
```

### Health Checks

| Service | Check | Interval | Timeout | Retries |
|---|---|---|---|---|
| `quietspace-monolith-db` | `mysqladmin ping` | 5s | 5s | 10 |
| `quietspace-monolith` | `wget /actuator/health` | 10s | 5s | 5 |

### Environment File Paths

| Service | `env_file` Path | Resolves To |
|---|---|---|
| `quietspace-monolith-db` | `../../.env` | Project root `.env` |
| `quietspace-monolith` | `../../.env` | Project root `.env` |
| `quietspace-frontend` | `../../.env` | Project root `.env` |

## Running with Different Profiles

### Local Development (Dev Profile)

```bash
# Ensure .env has:
SPRING_PROFILES_ACTIVE=dev

# Run from IDE or:
./gradlew bootRun
```

### Docker (Prod Profile)

```bash
# Ensure .env has:
SPRING_PROFILES_ACTIVE=prod
DB_HOST_NAME=quietspace-monolith-db
MAILDEV_HOST=mailpit
FRONTEND_HOST=quietspace-frontend

# Run from project root:
docker compose -f docker-compose.yaml up -d
```

### Override Environment Variables

```bash
# Override specific variables:
DB_HOST_NAME=my-custom-db docker compose -f docker-compose.yaml up -d

# Or create a .env.local file (not committed to git)
```

## Security Notes

- Never commit `.env` file to git (it's in `.gitignore`)
- Use strong passwords for production
- Rotate `JWT_SECRET_KEY` periodically
- Use a secrets manager for production deployments
- K8s secrets should NOT be committed to the repository
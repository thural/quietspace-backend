# CD Pipeline

## Overview

The Continuous Deployment (CD) pipeline runs only on the `prod` branch. It builds the Docker image, pushes it to GHCR, and deploys to the production VPS. The CI pipeline (test) runs first on `prod` before the CD stages execute.

## Pipeline Stages

```
[ CI: Test ] ──> [ CD: Build ] ──> [ CD: Deploy ]
                      │                   │
                      ▼                   ▼
                [ Push to GHCR ]   [ SSH + Docker Compose ]
```

**Note:** The CI stage (test) runs on both `main` and `prod` branches. The CD stages (build, deploy) only run on `prod`.

## Branch Behavior

| Branch | CI Stages | CD Stages |
|---|---|---|
| `main` | test | — |
| `prod` | test | build, deploy |

## Stage Details

### 1. Build Docker Image

**Purpose:** Build and push the Docker image to GitHub Container Registry (GHCR).

**Runner:** `ubuntu-22.04`

**Condition:** Only runs on `prod` branch (`if: github.ref == 'refs/heads/prod'`).

**Dependencies:** Requires `test` stage to succeed.

**Steps:**
1. Checkout code with full git history
2. Setup JDK 25 (Amazon Corretto)
3. Setup Gradle with caching
4. Extract project version from `../../build.gradle.kts`
5. Login to GHCR using `GITHUB_TOKEN`
6. Build and push Docker image

**Docker Build:**
- **Base image:** `eclipse-temurin:25-jre-alpine`
- **Build context:** Project root (`.`)
- **Dockerfile:** `Dockerfile`
- **Platform:** `linux/amd64`
- **Tags:**
  - `ghcr.io/<repo>/quietspace:monolith-<version>`
  - `ghcr.io/<repo>/quietspace:monolith-latest`

### 2. Deploy to VPS

**Purpose:** Deploy the application to the production VPS using Docker Compose.

**Runner:** `ubuntu-22.04`

**Condition:** Only runs on `prod` branch (`if: github.ref == 'refs/heads/prod'`).

**Dependencies:** Requires `build` stage to succeed.

**Steps:**
1. Checkout code
2. Create deployment folder on VPS via SSH
3. Copy Docker Compose file to VPS via SCP
4. Create `.env` file on VPS with secrets (via `env:` block + SCP)
5. Deploy via SSH

**Deployment Commands:**
```bash
# Create deployment directory
ssh $VPS_USERNAME@$VPS_IP "mkdir -p deployment"

# Copy Docker Compose file
scp docker-compose.yaml $VPS_USERNAME@$VPS_IP:deployment/docker-compose.yaml

# Create .env file on VPS
scp /tmp/deploy-env $VPS_USERNAME@$VPS_IP:deployment/.env

# Deploy
ssh $VPS_USERNAME@$VPS_IP << 'DEPLOY_EOF'
cd deployment
docker compose -f docker-compose.yaml pull -q
docker compose -f docker-compose.yaml up -d
DEPLOY_EOF
```

**Trigger:** Runs after build succeeds.

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      VPS (Ubuntu)                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────┐  ┌─────────────────┐              │
│  │  quietspace-    │  │  quietspace-    │              │
│  │  monolith       │  │  monolith-db    │              │
│  │  (Spring Boot)  │  │  (MySQL 8.0)    │              │
│  │  :8080          │  │  :3306          │              │
│  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                        │
│           └────────────────────┘                        │
│                    │                                    │
│           monolith-network                              │
│                    │                                    │
│  ┌─────────────────┐  ┌─────────────────┐              │
│  │  quietspace-    │  │  mailpit        │              │
│  │  frontend       │  │  (MailDev)      │              │
│  │  :3000          │  │  :1080, :1025   │              │
│  └─────────────────┘  └─────────────────┘              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Container Configuration

### Backend (quietspace-monolith)

| Setting | Value |
|---|---|
| Image | `ghcr.io/<repo>/quietspace:monolith-latest` |
| Port | `8080` |
| Health Check | `/actuator/health` (wget, 10s interval) |
| Depends On | `quietspace-monolith-db` (service_healthy) |
| Restart | `on-failure` |

### Database (quietspace-monolith-db)

| Setting | Value |
|---|---|
| Image | `mysql:8.0` |
| Port | `3306` |
| Health Check | `mysqladmin ping` (5s interval) |
| Volume | `quietspace_monolith_data` (persistent) |
| Restart | `on-failure` |

### Frontend (quietspace-frontend)

| Setting | Value |
|---|---|
| Image | `thural/quietspace:frontend` |
| Port | `80` → `3000` |
| Depends On | `quietspace-monolith` |

### Mailpit (mailpit)

| Setting | Value |
|---|---|
| Image | `maildev/maildev` |
| Ports | `1080` (UI), `1025` (SMTP) |
| Depends On | `quietspace-monolith` |

## Required GitHub Secrets

| Secret | Purpose | Example |
|---|---|---|
| `VPS_USERNAME` | SSH username for VPS | `ubuntu` |
| `VPS_IP` | VPS IP address | `192.168.1.100` |
| `DB_ROOT_PASSWORD` | MySQL root password | `rootpassword` |
| `DB_NAME` | Database name | `quietspace` |
| `DB_USER_USERNAME` | Database username | `quietspace_user` |
| `DB_USER_PASSWORD` | Database password | `userpassword` |
| `DB_PORT_NUMBER` | Database port | `3306` |
| `DB_HOST_NAME` | Database hostname on VPS | `quietspace-monolith-db` |
| `SERVER_PORT_NUMBER` | Application port | `8080` |
| `ADMIN_PASSWORD` | Admin account password | (your choice) |
| `JWT_SECRET_KEY` | JWT signing key | (your choice) |
| `ACTIVATION_URL` | Email activation link | `https://yourdomain.com/activate` |
| `FRONTEND_HOST` | Frontend hostname | `yourdomain.com` |
| `FRONTEND_PORT` | Frontend port | `3000` |
| `MAILDEV_HOST` | Mail server hostname | `mail.yourdomain.com` |
| `MAILDEV_PORT` | Mail server port | `1025` |

## Deployment Commands

### Manual Deployment

```bash
# SSH into VPS
ssh $VPS_USERNAME@$VPS_IP

# Navigate to deployment directory
cd deployment

# Pull latest images
docker compose -f docker-compose.yaml pull -q

# Start/restart containers
docker compose -f docker-compose.yaml up -d

# View logs
docker compose -f docker-compose.yaml logs -f quietspace-monolith

# Check container status
docker compose -f docker-compose.yaml ps
```

### Local Deployment (Development)

```bash
# Navigate to project root
cd /path/to/quietspace-backend

# Create network (first time only)
docker network create monolith-network

# Start all containers
docker compose -f docker-compose.yaml up -d

# Start only backend and database
docker compose -f docker-compose.yaml up -d quietspace-monolith-db quietspace-monolith

# View logs
docker compose -f docker-compose.yaml logs -f quietspace-monolith
```

## Health Checks

### Application Health

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### Container Health

```bash
# Check container health status
docker inspect --format='{{.State.Health.Status}}' quietspace-monolith
docker inspect --format='{{.State.Health.Status}}' quietspace-monolith-db
```

### Database Health

```bash
# Check MySQL connectivity
docker exec quietspace-monolith-db mysqladmin ping -u root -p$MYSQL_ROOT_PASSWORD
```

## Rollback Procedures

### Quick Rollback

```bash
# SSH into VPS
ssh $VPS_USERNAME@$VPS_IP

# Stop current containers
docker compose -f docker-compose.yaml down

# Pull previous version (if tagged)
docker pull ghcr.io/<repo>/quietspace:monolith-<previous-version>

# Update image tag in docker-compose.yaml and restart
docker compose -f docker-compose.yaml up -d
```

### Full Rollback

```bash
# Stop all containers
docker compose -f docker-compose.yaml down

# Remove persistent data (if needed)
docker volume rm quietspace_monolith_data

# Restore database from backup
# ...

# Start with previous version
docker compose -f docker-compose.yaml up -d
```

## Environment Variables

The pipeline generates a `.env` file on the VPS from GitHub Secrets. See [Configuration Guide](../guides/usage/configuration.md) for details.

| Variable | Source | Purpose |
|---|---|---|
| `DB_HOST_NAME` | GitHub Secret | MySQL hostname on VPS |
| `DB_PORT_NUMBER` | GitHub Secret | MySQL port |
| `DB_NAME` | GitHub Secret | Database name |
| `DB_USER_USERNAME` | GitHub Secret | Database username |
| `DB_USER_PASSWORD` | GitHub Secret | Database password |
| `DB_ROOT_PASSWORD` | GitHub Secret | MySQL root password |
| `SERVER_PORT_NUMBER` | GitHub Secret | Application port |
| `ADMIN_PASSWORD` | GitHub Secret | Admin account password |
| `JWT_SECRET_KEY` | GitHub Secret | JWT signing key |
| `ACTIVATION_URL` | GitHub Secret | Email activation URL |
| `MAILDEV_HOST` | GitHub Secret | Mail server hostname |
| `MAILDEV_PORT` | GitHub Secret | Mail server port |
| `FRONTEND_HOST` | GitHub Secret | Frontend hostname |
| `FRONTEND_PORT` | GitHub Secret | Frontend port |
| `SPRING_PROFILES_ACTIVE` | Hardcoded | Always `prod` on VPS |

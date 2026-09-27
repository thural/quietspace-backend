# CI/CD Architecture Overview

## System Architecture

```
 Developer Push ──> GitHub Repository
                          │
                          ▼
                  [ GitHub Actions ]
                          │
         ┌────────────────┴────────────────┐
         ▼                                 ▼
   [ CI Pipeline ]                   [ CD Pipeline ]
   - Test                           - Build Docker Image
   - (main + prod)                  - Push to GHCR
                                     - Deploy to VPS
                                     - (prod only)
         │                                 │
         └────────────────┬────────────────┘
                          ▼
                    [ Cloud VPS ]
                  (Ubuntu + Docker)
                          │
                          ▼
              [ Running Application ]
              - Spring Boot (REST + WebSocket)
              - MySQL 8.0
              - MailDev (dev only)
              - Frontend
```

## Pipeline Overview

| Stage | Tool | Purpose |
|---|---|---|
| Source Control | GitHub | Code hosting, branch protection, PR reviews |
| CI Engine | GitHub Actions | Automated test |
| Container Registry | GHCR (ghcr.io) | Docker image storage |
| Deployment Target | VPS (Ubuntu) | Running containers via Docker Compose |

## Branch Strategy

| Branch | CI Stages | CD Stages | Trigger |
|---|---|---|---|
| `main` | test | — | Push to `main` |
| `prod` | test | build, deploy | Push to `prod` |

## Key Files

| File | Purpose |
|---|---|
| `.github/workflows/ci-backend.yml` | GitHub Actions CI/CD pipeline |
| `Dockerfile` | Multi-stage Docker build |
| `docker-compose.yaml` | Container orchestration |
| `.env.example` | Environment variable template (committed) |
| `.env` | Environment variables (gitignored) |
| `.dockerignore` | Docker build context exclusions |

## Related Documentation

- [CI Pipeline Details](ci-pipeline.md)
- [CD Pipeline Details](cd-pipeline.md)
- [Configuration Guide](../guides/usage/configuration.md)

# Rule 05 — Configuration & Deployment Rules

> Extracted from: `guides/usage/configuration.md`, `usage-guide.md`, `pipeline/cicd-overview.md`, `ci-pipeline.md`, `cd-pipeline.md`, `gradle-modular-build-system.md` §8.

## 1. Layered config (single source of truth = `.env`, gitignored)

- Flow: `.env` → `application.yml` (base) → `application-{dev,prod}.yml` (overrides). `SPRING_PROFILES_ACTIVE` switches modes. Local Gradle runs do NOT auto-load `.env` — export first (`set -a && . ./.env && set +a`); Compose works via `env_file`.
- Dev profile hardcodes localhost hosts/ports + `ddl-auto: update`, Flyway off, live reload on. Prod reads `.env` service names + `ddl-auto: validate`, Flyway on, Hikari pool set.
- Sync points that MUST match (else link/auth failures): DB creds (`.env` ↔ compose `MYSQL_*` ↔ both profiles), `DB_HOST_NAME` (localhost dev / `quietspace-monolith-db` prod service name), `MAILDEV_HOST` (localhost dev / `mailpit` prod), ports (`DB 3306`, app `8080`, SMTP `1025`, frontend `3000`). Fix the matrix in `configuration.md` before changing any one side.

## 2. Secrets discipline

- Required: `ADMIN_PASSWORD`, `JWT_SECRET_KEY` (base64), `DB_USER_USERNAME/PASSWORD`; startup fails without SMTP (`MAILDEV_HOST/PORT`) and JWT key. Never commit `.env`; rotate keys; prod secrets via GitHub Secrets → VPS `.env` (CD writes it); K8s secrets never in repo.

## 3. Containers & health gates

- Multi-stage `Dockerfile` (JDK 25 build → JRE 25 runtime, `linux/amd64`); external `monolith-network`; startup order on health: DB `mysqladmin ping` (5s) → app `/actuator/health` (10s) → dependents. Backend `depends_on: service_healthy` DB; volumes persist `quietspace_monolith_data`. Never `down -v` in prod (destroys data); rollback = repin image tag + `pull` + `up -d`.
- App container always listens on `8080` internally; host mapping via `SERVER_PORT_NUMBER`.

## 4. CI/CD branch contract

- `main`: CI test only. `prod`: CI test → build+push (`monolith-<version>`, `monolith-latest` to GHCR) → SSH/compose deploy. CI path filter: `core/**`, `domain/**`, `app/**`, `buildSrc/**`, `gradle/**`, settings/root scripts, `Dockerfile`.
- Sharded test jobs (`test-core` 15m / `test-domain` 20m / `test-app` 30m) with Gradle build + configuration cache; no flaky-retry plugin (timeouts + fix flakes). New infra touching those paths must keep jobs green and cache-friendly.

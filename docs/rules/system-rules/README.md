# System Architecture Rules — Index

> Source of truth extracted from `docs/architecture/system-architecture-*.md`, `docs/architecture/observability.md`, `docs/architecture/events.md`, `docs/adr/002/003/005`, `docs/pipeline/`, `docs/guides/usage/`, `docs/guides/testing.md`, `docs/plans/extract-domain-iam-module.md`.
> Purpose: prevent system-level divergence when adding features or refactoring. Companion to `../module-rules/` (module boundaries); this directory covers **cross-cutting system guarantees**: composition, runtime, security, persistence, messaging infra, config, observability, API, deployment.
> Enforcement: Gradle + ArchUnit + CI gates + review checklists — violations fail the build or block merge.

## Rule files

| File | Category | When to use |
|------|----------|-------------|
| `01-system-design-principles.md` | SOLID at macro level, hexagonal, composition root, extraction readiness | Any new capability, new transport/infra, new module wiring |
| `02-runtime-persistence.md` | Stack, virtual threads, DB/Flyway/HikariCP, schema ownership | Touching runtime config, JPA, migrations, connection pools |
| `03-security-rules.md` | JWT chain, method security, CORS/CSRF, secrets, security tests | Touching auth, endpoints, WebSocket handshake, frontend origins |
| `04-messaging-infra.md` | Outbox infra, RabbitMQ topology, STOMP relay, email flow, trace propagation | Touching events infra, exchanges/queues, WebSocket, mail |
| `05-config-deployment.md` | Layered config, profile sync points, Docker/Compose, CI/CD | Touching `application*.yml`, `.env`, `Dockerfile`, compose, workflows |
| `06-observability-api.md` | Tracing, metrics, health, actuator, REST/OpenAPI + WS/AsyncAPI, errors, pagination | Adding endpoints, metrics, health indicators, error shapes |
| `07-feature-checklist.md` | Step-by-step checklist for features + system refactor sweep | **Start here** for feature/refactor work |

## The 10 non-negotiables (system level)

1. **Single composition root.** One `@SpringBootApplication` (`quietspace-app`); one executable `bootJar`. No second bootstrap, no web/server config in domain modules.
2. **Extend without modifying.** New capability = implement existing port / subscribe to existing event. Never edit core for a domain feature; never edit provider internals for a consumer need.
3. **Core stays domain-agnostic and swappable.** Transports (RabbitMQ→Kafka, WS→SSE), DB pools, mail backends change via wiring/config only — business logic never touches transport config.
4. **Virtual threads everywhere; no pinning.** `spring.threads.virtual.enabled=true`; HikariCP `maximum-pool-size ≈ CPU × 2`; no `synchronized` in hot paths (use `ReentrantLock`); monitor `jdk.VirtualThreadPinned`.
5. **One data story.** MySQL 8 prod (Flyway, `ddl-auto: validate`), H2 slice-only (`MODE=MYSQL;NON_KEYWORDS=user` + `Replace.NONE`), Testcontainers MySQL/RabbitMQ for app ITs. Migrations centralized in `app`; never `ddl-auto: update` in prod.
6. **AuthN in `core-security`, authZ decisions in domains, IAM data in `domain-user`.** No `User`/credential concepts in core; no JWT/filter logic in domains; security-posture tests live in `app` with the wired chain.
7. **Async state propagation only.** Cross-module state changes go outbox → `domain.events` → idempotent consumer. No direct cross-module service calls for state changes; no dual writes.
8. **Config is layered and synced.** `.env` (source of truth, gitignored) → `application.yml` → `application-{dev,prod}.yml`. DB/mail/host/port sync points across `.env` ↔ compose ↔ profiles must match; secrets via env/secrets-manager, never committed.
9. **Observable by default.** Trace propagation (HTTP → outbox → RabbitMQ → consumer → STOMP), `domain.*` metrics (Counter+Timer pairs), per-module `HealthIndicator`, actuator gated (`/actuator/health`, `/actuator/prometheus`), alerts on error rate / outbox lag / projector lag / pinning.
10. **Contracts first.** REST under `/api/v1` + OpenAPI (`core-web`), WebSocket via STOMP + AsyncAPI (`core-messaging`), `BaseResponse` envelope + `ProblemDetail` errors (`GlobalExceptionHandler`), paged defaults (page 0, size 25, sort `createdAt` desc).

## Canonical references (conflicts: these win)

- `docs/architecture/system-architecture-overview.md`, `system-architecture-review.md`
- `docs/architecture/observability.md`, `docs/architecture/events.md`
- `docs/adr/002-virtual-threads.md`, `003-transactional-outbox.md`, `005-feed-visibility-readmodel.md`
- `docs/pipeline/cicd-overview.md`, `ci-pipeline.md`, `cd-pipeline.md`
- `docs/guides/usage/configuration.md`, `usage-guide.md`, `docs/guides/testing.md`
- Module-boundary companion: `../module-rules/`

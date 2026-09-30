# Checklist 07 — Feature & System Refactor Checklist

> **Start here** for new features/system refactors. Module-boundary items live in `../module-rules/06-new-module-checklist.md` — use both when a feature adds a module.
> Legend: [01](01-system-design-principles.md) [02](02-runtime-persistence.md) [03](03-security-rules.md) [04](04-messaging-infra.md) [05](05-config-deployment.md) [06](06-observability-api.md)

## Part A — New feature (copy/paste, check off)

### A1. Design [01]
- [ ] Owner module identified (one writer); extension via existing port/event if possible — no multi-module internal edits.
- [ ] New contracts registered: port in consumer (`port/`) or query in provider (`api/`), event in `core-shared/event/` + `events.md` entry with routing key.
- [ ] Transport-swap safe: no broker/queue names, servlet/STOMP, or JDBC details in domain code.

### A2. Runtime & data [02]
- [ ] No stack drift (or ADR filed); virtual-thread safe (no `synchronized` hot path); pool impact considered.
- [ ] Migration (if any): centralized `app/.../V{next}__*.sql`, idempotent; prod-safe (`validate`-compatible); H2/Testcontainers matrix considered.

### A3. Security [03]
- [ ] Endpoint auth default-deny + `@PreAuthorize` for object checks; WS handshake path covered; CORS origins synced; errors via global handler (no leaks); secrets via env only.

### A4. Messaging [04]
- [ ] Publish via `TransactionalEventPublisher` in-TX; consumer idempotent (`processed_events`); topology changes confined to `core-messaging` + docs; mail only via `EmailEvent`; trace propagated.

### A5. Config & deploy [05]
- [ ] `.env` ↔ compose ↔ profiles sync matrix updated (DB/mail/hosts/ports); dev + prod profiles verified; compose health gates intact; CI paths/jobs green.

### A6. Observability & API [06]
- [ ] Metrics added (`domain.*.total` + `.duration`, event labels where relevant); `HealthIndicator` extended/added + tested; alerts/panels for new pipeline; REST under `/api/v1` in OpenAPI + WS in AsyncAPI; paged + enveloped responses.

## Part B — System refactor sweep (divergence hunt)

- [ ] No second bootstrap/`bootJar`/server config outside `app` [01][05].
- [ ] No transport imports in domains; no cross-schema joins; no SMTP-direct sends [01][04].
- [ ] No `User`/credential types in core; no JWT/filter code in domains [03].
- [ ] No `ddl-auto: update`/Flyway-on in prod; no committed `.env`/secrets; no `down -v` runbooks [02][05].
- [ ] No missing trace hops, unlabeled metrics, actuator over-exposure, or undocumented endpoints [06].
- [ ] Full `./gradlew check` + app ITs + security-posture tests green; dashboards/alerts verified.

# Rule 06 — Observability & API Contract Rules

> Extracted from: `architecture/observability.md`, `system-architecture-overview.md` §1/§10, `core-module-apis.md` (core-web/shared utils), `usage-guide.md` (API docs).

## 1. Tracing

- Micrometer Tracing (Brave) + Zipkin (`ZIPKIN_URL`, 100% sampling dev — lower deliberately for prod). Spans propagate HTTP → outbox → RabbitMQ → consumer → STOMP (Rule 04 §4). New service/consumer must appear in Zipkin with parent-child linkage — add/extend the tracing IT pattern when adding hops.

## 2. Metrics (naming is the contract)

- Domain metrics: `domain.<module>.<op>.total` (Counter, `result=success|error`) + `domain.<module>.<op>.duration` (Timer); event/projector: `domain.event.processing.duration` + `domain.post.visibility.projector.duration` labeled `eventType=...`; visibility table gauges (`...table.size`). Register via `MeterRegistry` Counter/Timer-builder pattern; record inside the operation (increment only on completion path).
- Standard JVM/HTTP/Hikari/RabbitMQ metrics are auto-configured — dashboard them, don't re-emit. Prometheus scrape at `/actuator/prometheus`; actuator exposure minimal (`health`, `metrics`, `prometheus`); health details `when-authorized`.

## 3. Health

- Every domain module ships a `*HealthIndicator` (`health/` package) reporting its aggregate count (`totalUsers`, `totalPosts`, …). Unit-test it (UP + details on success). App `/actuator/health` gates compose startup and deploys — never remove the health dependency edge.

## 4. Alerts (keep these live)

- `HighErrorRate` (5xx rate > 5%, 5m, critical); `OutboxLag` (unpublished > 5m, warning); `VirtualThreadPinning` (pinning increase, warning); `PostVisibilityProjectorLag` (visibility stale > 60s, warning). New durable pipeline/projector = new lag alert + Grafana panel (request/error/latency p99, outbox lag, projector lag, threads, DB connections).

## 5. API contracts

- REST: prefix `/api/v1/...`; OpenAPI config in `core-web` (`OpenApiConfig`, bearer scheme); live docs `swagger-ui.html` + `/v3/api-docs`. Responses use the `BaseResponse` envelope; errors via `GlobalExceptionHandler` (`ProblemDetail` + `ErrorResponse` with timestamp/status/error/message/path). Pagination via `PagingProvider`/`PageUtils` defaults (page 0, size 25, sort `createdAt` desc).
- WebSocket/STOMP: config + AsyncAPI in `core-messaging` (SpringWolf; docket YAML in app `application.yml`); live UI `springwolf/asyncapi-ui.html` + `/springwolf/docs`. New event pushed to clients = new documented AsyncAPI message + consumer queue binding (Rule 04 §2).

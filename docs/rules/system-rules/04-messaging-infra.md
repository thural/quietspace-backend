# Rule 04 — Messaging Infrastructure Rules

> Extracted from: `events.md`, ADR 003, `core-module-apis.md` (core-messaging), `system-architecture-overview.md` §5, `observability.md` (trace propagation). Port/event *usage* rules live in `../module-rules/03-communication-rules.md`; this file covers *infrastructure guarantees*.

## 1. Transactional outbox (the only publish path)

- Produce: `TransactionalEventPublisher` persists to `outbox_events` in the **same TX** as the aggregate. Poller (`@Scheduled`, ~1–5s) publishes to RabbitMQ, marks published. No direct broker writes from domain services; no in-memory-only `@Async` event fan-out for durable flows.
- Consume: `@EventListener` + `processed_events` dedup by `eventId` (at-least-once safe). Projectors (`PostVisibilityProjector`) follow the same idempotency rule. Outbox/event tables need TTL/cleanup + lag monitoring (Rule 06 alerts).

## 2. RabbitMQ topology (do not improvise)

- `domain.events` **topic** exchange → per-consumer queues (`notification.events`, `websocket.events`, …); routing keys `domain.<agg>.<action>` per `events.md` catalog.
- `email.exchange` (topic) → `email.queue` → `EmailEventConsumer` → `EmailService`. Any domain sends mail only via `EmailEventPublisher` → `EmailEvent`; never SMTP-direct from domains.
- Queue/exchange/relay config lives in `core-messaging` (`RabbitMQConfig`, `WebSocketConfig` STOMP broker relay `/topic`+`/queue`, `/user` prefix, 30s heartbeat). Domains reference event classes, never topology names — except routing-key conventions when adding events (document in `events.md`).

## 3. WebSocket delivery

- Flow: domain event → RabbitMQ → STOMP relay → user's connected node → WebSocket session. Multi-node safe by construction; do not add in-process session maps as the delivery path.
- Handshake/interceptor/listener code stays in `core-messaging`; user resolution via `WebSocketUserPort` adapter in `domain-user`.

## 4. Trace propagation across async boundary

- Outbox→RabbitMQ injects trace headers via `MessageConverter`; consumer extracts and continues the trace as a child span; STOMP frames carry trace context via headers. New async hops must propagate (not drop) the trace — verify with the `TracingIntegrationTest` pattern (root + child spans in Zipkin).

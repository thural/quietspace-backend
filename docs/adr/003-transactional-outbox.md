# ADR 003: Transactional Outbox Pattern for Domain Events

## Status
Accepted

## Context
The application needs to publish domain events (UserRegistered, PostCreated, MessageSent, etc.) to:
- Notification service (email, in-app notifications)
- WebSocket clients (real-time updates)
- Future microservices

Current in-memory `@Async` + `@EventListener` approach loses events on JVM crash/restart. We need **at-least-once delivery** guarantee.

## Decision
Implement **Transactional Outbox Pattern** with RabbitMQ:

### Components:
1. **Outbox Table** (`outbox_events`): Persistent event store (id, aggregate_type, aggregate_id, event_type, payload, created_at, published_at)
2. **Outbox Entity**: JPA entity mapping to outbox table
3. **Transactional Publisher**: Saves event to outbox within same DB transaction as aggregate changes
4. **Outbox Poller**: `@Scheduled` job (1-5s interval) polls unpublished events, publishes to RabbitMQ
5. **RabbitMQ Exchange**: Topic exchange `domain.events` with per-domain queues
6. **Idempotent Consumers**: Consumers track processed `eventId` to handle at-least-once delivery

### Flow:
```
Aggregate Change (e.g., UserServiceImpl.register())
    │
    ├── DB Transaction ──────────────────┐
    │   ├── Save User                    │
    │   └── Save OutboxEvent             │
    │        (UserRegisteredEvent)       │
    │                                    │
    ▼                                    ▼
Commit                           Outbox Poller (separate TX)
                                      │
                                      ▼
                              RabbitMQ (domain.events)
                                      │
                    ┌──────────────────┼──────────────────┐
                    ▼                  ▼                  ▼
              Notification         WebSocket           Future
              Service              Clients             Services
```

## Consequences
### Positive
- **At-least-once delivery**: Events persisted atomically with domain changes
- **No event loss**: Survives JVM crashes, restarts, deployments
- **Decoupled consumers**: Services consume via RabbitMQ, no direct dependencies
- **Scalable**: Poller can be tuned; RabbitMQ handles routing/load
- **Replayable**: Events in outbox can be reprocessed

### Negative
- **Eventual consistency**: Consumers process events with delay (poll interval)
- **Idempotency required**: Consumers must handle duplicate events
- **Operational complexity**: Outbox table growth, poller monitoring
- **Poller latency**: 1-5s default; can tune or use Debezium CDC for lower latency

### Neutral
- Start with simple `@Scheduled` poller; migrate to Debezium CDC only if needed
- Phase 6 multi-module extraction moves outbox components to core-data/core-messaging

## Alternatives Considered
- **In-memory @Async + @EventListener**: Loses events on crash (current state)
- **Debezium CDC + Kafka**: Lower latency but adds Kafka Connect cluster operational complexity
- **Direct HTTP calls to consumers**: Tight coupling, no resilience
- **Distributed transactions (2PC)**: Not supported by most databases, complexity

## Related ADRs
- ADR 001: Core/Domain Split Architecture
- ADR 002: Virtual Threads for Concurrency

## References
- "Transactional Outbox Pattern" - Microservices Patterns by Chris Richardson
- Architecture Improvement Plan Phase 4
- RabbitMQ STOMP Relay for WebSocket horizontal scaling (Phase 4.3)
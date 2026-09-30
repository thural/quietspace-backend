# Rule 03 — Inter-Module Communication Rules

> Extracted from: `modular-architecture-review.md` §3, `modular-architecture-overview.md` §4, `domain-module-contracts.md`, `core-module-apis.md`, `events.md`, ADR 003, ADR 005.
> Principle: modules communicate **exclusively** through consumer-owned ports (sync) and domain events (async).

## 1. Decision guide

| Need | Pattern | Example |
|------|---------|---------|
| Sync read of another domain's data (display name, count, existence) | **Query port** in provider's `api/` | `PostMapper` → `UserQueryPort.getUserSummary` |
| Sync action/query owned by consumer, implemented by provider | **Consumer-owned port** in consumer's `port/` + provider `adapter/` | `NotificationPostPort` → `PostNotificationAdapter` |
| Core/infra needs domain data (WS auth, photo upload) | **Shared SPI** in `core-shared` + domain adapter | `UserProfilePort` → `UserProfileAdapter`, `WebSocketUserPort` → `WebSocketUserAdapter` |
| State-change notification, side effect, projection, cross-module write | **Domain event** via transactional outbox | `UserRegisteredEvent` → welcome notification |
| Delete cascade across modules | **Explicit command port** | `CommentCommandPort.deleteAllByPostId` |

## 2. Query port rules (`api/`)

- Port interface + immutable **record** DTOs live in provider's `api/` (`api/dto/`); adapter (`*QueryAdapter`, `@Component`) in provider delegates to the owning repository.
- Ports return records (serializable, no lazy loading). **No entities leak across boundaries.**
- Auth (`getSignedUser`) stays on `UserService` — query ports carry **display data only**.
- Registry: `UserQueryPort`, `PhotoQueryPort`, `PostQueryPort`, `CommentQueryPort`, `ReactionQueryPort` (methods/DTOs in `domain-module-contracts.md`). Extend these before inventing ad-hoc reads.
- Spec/query usage: `PostSpecifications.containsText` → `UserQueryPort.searchUserIds` + `IN`; `commentedByUser` → `CommentQueryPort.findPostIdsByUserId` + `IN`; `visibleToUser` → local visibility tables (never cross-module joins).

## 3. Consumer-owned port rules (`port/` + `adapter/`)

- **Port interface lives in the consumer module; adapter implementation lives in the provider module.** Dependency direction: consumer owns, provider depends on consumer.
- Consumer keeps only the methods it needs; provider can change internals without touching the consumer.
- Registry: `NotificationUserPort` / `NotificationPostPort` / `NotificationCommentPort` (owner: notification), `CommentPostPort` + `CommentCommandPort` (owner: comment), `ChatMessagePort` (owner: chat).
- New sync need → first check: does a query port or existing consumer port cover it? Only then define a new minimal port in the consumer.

## 4. Domain event rules (ADR 003, `events.md`)

- **All event classes live in `core-shared/event/`** extending `DomainEvent` (`eventId`, `timestamp`, `aggregateType`, `aggregateId`, `eventType`). Never define domain events inside domain modules.
- **Publish via `TransactionalEventPublisher` in the same transaction as the aggregate change.** Never publish after commit manually, never call another module's service/listener directly.
- Delivery = outbox table → `@Scheduled` poller → RabbitMQ `domain.events` topic exchange → per-consumer queues + STOMP relay. **At-least-once** semantics.
- **Every consumer MUST be idempotent:** check `ProcessedEventRepository.existsByEventId(eventId)` first, then process, then save `ProcessedEvent`. Projectors (`PostVisibilityProjector`) follow the same rule.
- Eventual-consistency windows are deliberate (ADR 005: privacy/follow propagation is typically sub-second; authors without a visibility row default to public). Do not "fix" by re-adding sync joins.
- Adding an event: (1) class in `core-shared`, (2) roundtrip test in `EventSerializerTest`, (3) publish from service, (4) `@EventListener` consumer with idempotency, (5) end-to-end test in `app`.
- Catalog (publisher → consumers): `UserRegisteredEvent`, `PostCreatedEvent`, `CommentCreatedEvent`, `ReactionAddedEvent`, `MessageSentEvent`, `UserFollowedEvent`, `UserUnfollowedEvent`, `UserPrivacyChangedEvent`, `EmailEvent` (via `EmailEventPublisher` → `email.queue`). See `events.md` for payloads/routing keys.

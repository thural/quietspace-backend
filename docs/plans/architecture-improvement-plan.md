# Architecture Improvement Plan

## Overview

This plan consolidates all architectural weaknesses, recommendations, and strategic directions from `architecture-analysis.md` into an actionable, phased improvement roadmap. The plan is organized by priority and dependency order to ensure each phase delivers measurable architectural improvements.

---

## Phase 0: Foundation & Governance (Weeks 1-2)

### 0.1 Establish Architectural Guardrails

| Task | Description | Owner | Deliverable |
|------|-------------|-------|-------------|
| **ArchUnit Integration** | Add ArchUnit dependency; create baseline test suite | Platform Team | `ArchitectureRulesTest.java` in `shared/test/archunit/` |
| **Core Rules** | Enforce: no core→domain deps, no domain→domain internal access, repositories package-private | Platform Team | Passing ArchUnit tests in CI |
| **Dependency Graph Baseline** | Generate current dependency graph (jdeps/Gradle) for comparison | Platform Team | `dependency-graph-baseline.dot` |

### 0.2 Code Quality Gates

| Task | Description | Owner | Deliverable |
|------|-------------|-------|-------------|
| **Static Analysis** | Configure SpotBugs, PMD, Checkstyle in CI | Platform Team | Pipeline integration |
| **Architecture Decision Log** | Create ADR template; document key decisions (Core/Domain split, Event-driven, etc.) | Tech Lead | `/docs/adr/` new entries |

### 0.3 Scalability Foundations (Java 25 Virtual Threads)

| Task | Description | Owner | Deliverable |
|------|-------------|-------|-------------|
| **Enable Virtual Threads** | Configure `spring.threads.virtual.enabled=true` in `application.yml`; verify Tomcat uses virtual threads for HTTP & WebSocket | Platform Team | Running app with virtual threads; load test baseline |
| **Configure Virtual Thread Executor** | Update `AsyncConfig.java` to use `VirtualThreadPerTaskExecutor` for `@Async` event processing | Platform Team | `@Async` methods run on virtual threads |
| **Connection Pool Tuning** | Tune HikariCP for virtual thread compatibility (`maxPoolSize` ≈ CPU cores × 2) | Platform Team | Stable DB connections under high concurrency |
| **Pinning Detection Setup** | Configure JFR recording for `jdk.VirtualThreadPinned` events; add monitoring dashboard alert | Platform Team | JFR profile showing zero/low pinning; alert on sustained pinning |

**Rationale**: Java 25 (Project Loom) enables massive concurrency without WebFlux complexity. Virtual threads allow Tomcat to handle millions of concurrent WebSocket/REST connections and `@Async` event handlers to scale elastically.

**Critical Watch**: Thread pinning occurs when virtual threads enter `synchronized` blocks (common in older JDBC drivers, legacy libs). This blocks the carrier thread, negating virtual thread benefits. Monitor via JFR (`jdk.VirtualThreadPinned`) and avoid `synchronized` in hot paths — use `ReentrantLock` or `java.util.concurrent` primitives instead.

---

## Phase 1: Critical Fixes - Repository Leakage & Visibility (Weeks 3-4)

### 1.1 Fix `ChatController` Repository Injection (Critical)

| Task | File | Action |
|------|------|--------|
| Extract message operations to `MessageService` | `chat/controller/ChatController.java` | Move `MessageRepository` usage to `MessageServiceImpl` |
| Extract user operations to `UserService` | `chat/controller/ChatController.java` | Move `UserRepository` usage to `UserServiceImpl` |
| Add service methods | `message/MessageService.java`, `user/UserService.java` | Define required operations as interface methods |
| Implement in service layer | `message/MessageServiceImpl.java`, `user/UserServiceImpl.java` | Add `@Transactional`, authorization checks |
| Update controller | `chat/controller/ChatController.java` | Inject `MessageService`, `UserService` instead of repositories |

**Validation**: ArchUnit test fails if controller accesses repository directly.

### 1.2 Enforce Package-Private Repositories (Critical)

| Task | Files | Action |
|------|-------|--------|
| Change visibility | All `*Repository.java` files | Remove `public` modifier |
| Fix test access | Test files in `src/test/` | Move to same package or use `@TestConfiguration` |
| Update ArchUnit | `ArchitectureRulesTest.java` | Add rule: repositories must be package-private |

**Note**: Cross-feature repository access in `NotificationServiceImpl` is **deferred to Phase 4** where it will be resolved via Domain Events + Transactional Outbox, avoiding temporary `ReadService` interfaces that would be discarded.

---

## Phase 2: Shared Kernel Cleanup (Weeks 5-6)

### 2.1 Move Domain-Specific Enums to Owning Features

| Enum | Current Location | Target Location |
|------|------------------|-----------------|
| `ReactionType` | `shared/enums/ReactionType.java` | `reaction/ReactionType.java` |
| `NotificationType` | `shared/enums/NotificationType.java` | `notification/NotificationType.java` |
| `EntityType` | `shared/enums/EntityType.java` | `reaction/EntityType.java` (or shared if truly generic) |
| `EventType` | `shared/enums/EventType.java` | `websocket/event/EventType.java` |

**Action**: Move files, update all imports across codebase, verify build.

### 2.2 Define True Shared Kernel

**Keep in `shared/`** (domain-agnostic only):
- `BaseEntity`, `BaseResponse`, `ErrorResponse`, `CustomErrorResponse`
- `PagingProvider`, `PageUtils`, `ImageCompressionUtil`
- `Role`, `StatusType`, `Permission` (security-related, truly shared)
- `EmailTemplateName` (infrastructure)
- Exception classes (infrastructure)
- `CommonService` interface

**Rationale**: Moving enums first ensures Phase 3 (Rich Domain Models) references permanent package locations, avoiding import refactoring churn.

---

## Phase 3: Domain Model Enrichment (Weeks 7-9)

### 3.1 Enrich `User` Aggregate

| Task | File | Action |
|------|------|--------|
| Encapsulate follow logic | `user/User.java` | Add `follow(User)`, `unfollow(User)`, `block(User)`, `unblock(User)` methods |
| Encapsulate profile settings | `user/ProfileSettings.java` | Add validation, business rules |
| Remove setter misuse | `user/User.java` | Replace direct field sets with domain methods |
| Update `UserServiceImpl` | `user/UserServiceImpl.java` | Call domain methods instead of direct field manipulation |

### 3.2 Enrich `Post` Aggregate

| Task | File | Action |
|------|------|--------|
| Encapsulate poll voting | `post/Post.java` | Add `votePoll(userId, optionId)` with validation |
| Encapsulate save/unsave | `post/Post.java` | Add `saveBy(User)`, `unsaveBy(User)` |
| Encapsulate repost | `post/Post.java` | Add `repostBy(User, text)` factory method |

### 3.3 Enrich `Chat` & `Message` Aggregates

| Task | File | Action |
|------|------|--------|
| Encapsulate membership | `chat/Chat.java` | Add `addMember(User)`, `removeMember(User)` |
| Encapsulate message creation | `message/Message.java` | Factory method with validation |
| Add read receipts logic | `message/Message.java` | Domain methods for marking read |

### 3.4 Enrich `Reaction` Aggregate

| Task | File | Action |
|------|------|--------|
| Factory method | `reaction/Reaction.java` | `create(userId, username, contentId, contentType, reactionType)` |
| Validation | `reaction/Reaction.java` | Prevent duplicate reactions, validate enum values |

**Note**: Entities now reference enums at their final locations (e.g., `reaction.ReactionType`), enabling domain methods to natively generate domain events (e.g., `user.follow()` triggers `UserFollowedEvent`) before Phase 4 infrastructure.

## Phase 4: Domain Events Implementation (Weeks 10-12)

### 4.1 Event Infrastructure — Transactional Outbox Pattern

| Task | Location (Monolith) | Action |
|------|---------------------|--------|
| **Outbox table** | `src/main/resources/db/migration/Vx__outbox_table.sql` | Create `outbox_events` table (id, aggregate_type, aggregate_id, event_type, payload, created_at, published_at) |
| **Outbox entity** | `shared/event/OutboxEvent.java` | JPA entity mapping to `outbox_events` |
| **Outbox repository** | `shared/event/OutboxEventRepository.java` | Spring Data repository for outbox polling |
| **Transactional publisher** | `shared/event/TransactionalEventPublisher.java` | Save event to outbox within same transaction as aggregate (via `@Transactional` + `ApplicationEventPublisher` hook) |
| **Outbox poller** | `shared/messaging/OutboxEventPoller.java` | **Start with `@Scheduled` Spring job** polling `outbox_events` (1-5s interval). Only introduce Debezium CDC if polling causes DB CPU spikes or latency is unacceptable for chat. |
| **RabbitMQ exchange setup** | `config/RabbitMqConfig.java` | Declare topic exchange `domain.events`; bind queues per consumer domain |
| **Domain event base** | `shared/event/DomainEvent.java` | Abstract base with timestamp, eventId, aggregateId, aggregateType |
| **Event serializer** | `shared/event/EventSerializer.java` | Jackson-based JSON serializer for outbox payload |

**Why Transactional Outbox**: In-memory `@Async` + `@EventListener` loses events on JVM crash. Outbox guarantees **at-least-once delivery** by persisting events atomically with domain changes. RabbitMQ provides durable, ordered delivery across horizontally scaled instances.

**Poller Strategy**: Begin with simple `@Scheduled` poller. Monitor: (a) poll interval vs. latency requirements, (b) DB CPU from polling. Migrate to Debezium only if needed — adds operational complexity (Kafka Connect cluster).

**Note**: File paths use existing monolith packages. In Phase 6, these classes will be physically moved to `core-data`, `core-messaging`, `core-shared` modules with zero code changes — only package declarations and imports updated.

### 4.2 Define Domain Events

| Event | Publisher | Consumers |
|-------|-----------|-----------|
| `UserRegisteredEvent` | `auth/AuthServiceImpl` | `notification`, `user` (welcome) |
| `PostCreatedEvent` | `post/PostServiceImpl` | `notification`, `chat` (mentions) |
| `CommentCreatedEvent` | `comment/CommentServiceImpl` | `notification`, `post` (comment count) |
| `ReactionAddedEvent` | `reaction/ReactionServiceImpl` | `notification`, `post` (reaction count) |
| `MessageSentEvent` | `message/MessageServiceImpl` | `notification`, `chat` (last message) |
| `UserFollowedEvent` | `user/UserServiceImpl` | `notification` |

### 4.3 WebSocket Horizontal Scaling — External STOMP Broker

| Task | Location | Action |
|------|----------|--------|
| **Configure RabbitMQ STOMP relay** | `websocket/config/WebSocketConfig.java` | `registry.enableStompBrokerRelay("/topic", "/queue").setRelayHost("rabbitmq").setRelayPort(61613)` |
| **User destination prefix** | `websocket/config/WebSocketConfig.java` | `registry.setUserDestinationPrefix("/user");` — messages routed via RabbitMQ to correct node |
| **Session registry** | `websocket/event/listener/SocketEventListener.java` | Track connected users per node; sync via Redis if needed for presence |
| **Heartbeat config** | `application.yml` | `spring.websocket.client.heartbeat: 30000` (30s) |

**Rationale**: Default in-memory STOMP broker doesn't work across multiple backend instances. RabbitMQ STOMP relay routes messages to the node where the target user is connected, enabling horizontal scaling of WebSocket servers.

### 4.4 Refactor Consumers

| Consumer | Current Coupling | New Approach |
|----------|------------------|--------------|
| `NotificationServiceImpl` | Direct repo access | `@EventListener` on domain events (via RabbitMQ) |
| `WebSocket` notifications | Direct service calls | Event-driven via STOMP (RabbitMQ relay) |

**Idempotency Requirement**: Since Outbox + RabbitMQ provides **at-least-once delivery**, all event consumers **must be idempotent**. Implement by:
- Including `eventId` (UUID) in every `DomainEvent`
- Consumers check `processed_events` table (or cache) for `eventId` before processing
- Example: `NotificationEventListener` → `if (notificationRepository.existsByEventId(event.getEventId())) return;`

---

## Phase 5: Security & WebSocket Decoupling (Weeks 14-15)

### 5.1 Dependency Inversion for Authentication

| Task | File | Action |
|------|------|--------|
| Create core interface | `security/AuthenticationProvider.java` (new in core) | `UserDetails loadUserByUsername(String)` |
| Implement in user domain | `user/UserAuthenticationProvider.java` | Delegate to `UserRepository` |
| Update `WebSocketConfig` | `websocket/config/WebSocketConfig.java` | Inject `AuthenticationProvider` interface |
| Update `AppConfig` | `config/AppConfig.java` | Wire implementation |

### 5.2 JWT Service Abstraction

| Task | File | Action |
|------|------|--------|
| Create interface | `security/JwtTokenService.java` | `generateToken(UserDetails)`, `validateToken(String)` |
| Implement | `security/JwtTokenServiceImpl.java` | Current `JwtService` logic |
| Update dependents | `JwtFilter`, `WebSocketConfig`, `AuthService` | Inject interface |

### 5.3 WebSocket Virtual Thread Optimization

| Task | File | Action |
|------|------|--------|
| Configure STOMP client executor | `websocket/config/WebSocketConfig.java` | Use `Executors.newVirtualThreadPerTaskExecutor()` for inbound/outbound channels |
| WebSocket handshake handler | `websocket/config/CustomHandshakeHandler.java` | Ensure non-blocking; offload auth to virtual thread executor |
| Session registry concurrency | `websocket/event/listener/SocketEventListener.java` | Use `ConcurrentHashMap` with virtual-thread-friendly locking |

**Rationale**: With virtual threads enabled (Phase 0.3), WebSocket connection handling and STOMP message processing scale to millions of concurrent connections without thread pool exhaustion.

---

## Phase 6: Multi-Module Core/Domain Restructure (Weeks 16-22)

### 6.1 Gradle Multi-Module Setup

```
quietspace-platform/
├── settings.gradle.kts
├── core/
│   ├── core-data/
│   ├── core-web/
│   ├── core-security/
│   ├── core-messaging/
│   └── core-shared/
├── domain/
│   ├── domain-auth/
│   ├── domain-user/
│   ├── domain-post/
│   ├── domain-photo/
│   ├── domain-comment/
│   ├── domain-reaction/
│   ├── domain-chat/
│   ├── domain-message/
│   └── domain-notification/
└── app/
    └── quietspace-app/
```

### 6.2 Module Extraction Order (Dependency-Safe)

| Step | Module | Depends On | Notes |
|------|--------|------------|-------|
| 1 | `core-shared` | — | BaseEntity, PagingProvider, BaseResponse, exceptions |
| 2 | `core-data` | core-shared | JPA auditing, generic repositories |
| 3 | `core-web` | core-shared | Exception handler, OpenAPI, base controllers |
| 4 | `core-security` | core-shared, core-data | JWT, filter chain, AuthenticationProvider interface |
| 5 | `core-messaging` | core-shared, core-security | RabbitMQ, EmailEvent, WebSocket config |
| 6 | `domain-auth` | core-* | AuthController, AuthService, DTOs |
| 7 | `domain-user` | core-*, domain-auth | User aggregate, UserService, UserRepository |
| 8 | `domain-post` | core-*, domain-user | Post, Poll, PostService |
| 9 | `domain-photo` | core-*, domain-user, domain-post | Photo, PhotoService |
| 10 | `domain-comment` | core-*, domain-user, domain-post | Comment, CommentService |
| 11 | `domain-reaction` | core-*, domain-user, domain-post, domain-comment | Reaction, ReactionService |
| 12 | `domain-chat` | core-*, domain-user | Chat, ChatService |
| 13 | `domain-message` | core-*, domain-user, domain-chat | Message, MessageService |
| 14 | `domain-notification` | core-*, domain-user, domain-post, domain-comment, domain-message, domain-reaction, domain-chat | Notification, NotificationService |
| 15 | `quietspace-app` | all domain modules | SpringBootApplication, wiring |

### 6.3 Per-Module Tasks

For each domain module:
1. Create `build.gradle.kts` with dependencies
2. Move source files maintaining package structure
3. Update `package` declarations
4. Fix imports
5. Add module-specific ArchUnit tests
6. Verify `./gradlew :domain-xxx:build` passes

---

## Phase 7: Test Architecture Alignment (Weeks 23-24)

### 7.1 Restructure Test Tree

```
src/test/java/dev/thural/quietspace/
├── core/
│   ├── data/
│   ├── web/
│   ├── security/
│   ├── messaging/
│   └── shared/
├── domain/
│   ├── auth/
│   ├── user/
│   ├── post/
│   ├── photo/
│   ├── comment/
│   ├── reaction/
│   ├── chat/
│   ├── message/
│   └── notification/
└── app/
    └── integration/
```

### 7.2 Test Categories per Module

| Module | Unit Tests | Slice Tests | Integration Tests |
|--------|------------|-------------|-------------------|
| Each domain | `*ServiceImplTest`, `*MapperTest` | `*ControllerSliceTest` | `*FlowIT` |
| Core modules | Utility tests, config tests | N/A | N/A |
| App | N/A | N/A | Full integration, contract tests |

---

## Phase 8: Observability & Documentation (Weeks 25-26)

### 8.1 Architecture Documentation

| Document | Location | Content |
|----------|----------|---------|
| **Core Module APIs** | `/docs/architecture/core/` | Public interfaces, extension points |
| **Domain Module Contracts** | `/docs/architecture/domains/` | Service interfaces, events published/consumed |
| **Event Catalog** | `/docs/architecture/events.md` | All domain events with payloads |
| **Migration Guides** | `/docs/guides/migration/` | How to add new domain module |

### 8.2 Observability Enhancements

| Area | Action |
|------|--------|
| **Distributed Tracing** | Add Micrometer Tracing + Zipkin/OpenTelemetry |
| **Metrics** | Domain-level metrics (event latency, handler duration) |
| **Health Checks** | Per-domain health indicators |

---

## Dependency Map & Critical Path

```
Phase 0 (ArchUnit + Virtual Threads) 
    │
    ├─→ Phase 1 (Repository Leakage & Visibility)
    │
    ├─→ Phase 2 (Shared Kernel Cleanup / Enum Moving)
    │       ↓
    └─→ Phase 3 (Rich Domain Models)
            ↓
    Phase 4 (Domain Events + Outbox + STOMP Relay)
            ↓
    Phase 5 (Security Decoupling + WS Virtual Threads)
            ↓
    Phase 6 (Multi-Module Gradle Restructure)
            ↓
    Phase 7 (Test Restructure)
            ↓
    Phase 8 (Observability)
```

**Critical Path**: Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 (≈22 weeks)

**Parallelizable**: Phase 0.3 (Virtual Threads) can start immediately; Phase 2 (Shared Kernel) independent of Phase 1.

---

## Success Metrics

| Met | Baseline | Target | Measurement |
|-----|----------|--------|-------------|
| **Cross-feature repository access** | 2+ instances | 0 | ArchUnit test |
| **Controller→Repository calls** | 1 (`ChatController`) | 0 | ArchUnit test |
| **Public repositories** | All | 0 | ArchUnit test |
| **Domain enums in shared/** | 3 (`ReactionType`, `NotificationType`, `EntityType`) | 0 | File count |
| **Anemic entities** | 8/8 aggregates | 0/8 | Manual review + mutation testing |
| **Domain event coverage** | 2 (email, websocket) | 7+ (all aggregates) | Event catalog |
| **Event delivery guarantee** | In-memory (lossy) | Transactional Outbox (at-least-once) | Outbox table + RabbitMQ metrics |
| **Core reusability** | 0% (mixed) | 100% (zero domain deps) | ArchUnit: core↛domain |
| **Module build independence** | 1 (monolith) | 15+ (core + domain + app) | Gradle build graph |
| **WebSocket horizontal scaling** | Single node (in-memory broker) | Multi-node (RabbitMQ STOMP relay) | Load test: 100K+ concurrent connections |
| **Virtual thread utilization** | Platform threads (blocking) | Virtual threads (non-blocking) | `jcmd <pid> Thread.print` shows virtual threads |
| **Async event processing** | Thread pool (bounded) | Virtual threads (elastic) | `@Async` latency under load |

---

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Build breakage during multi-module split | High | High | Phase 6 incremental; each module builds independently before integration |
| Test failures during restructuring | High | Medium | Phase 7 parallel with Phase 6; fix tests per module |
| Performance regression from events | Low | Medium | Async event processing; benchmark before/after |
| Team velocity impact | Medium | High | Dedicate 20% capacity; pair on complex refactors |
| Circular dependency discovery | Medium | High | ArchUnit runs on every PR; fail fast |
| **Virtual thread compatibility issues** | Medium | High | Test all blocking libs (JDBC, Redis, etc.); use Spring Boot 3.2+ virtual thread support |
| **Virtual thread pinning** | Medium | High | Monitor `jdk.VirtualThreadPinned` via JFR; replace `synchronized` in hot paths with `ReentrantLock`; upgrade pinning-prone libraries |
| **Outbox poller latency** | Low | Medium | Tune poll interval (1-5s); monitor `published_at` lag; consider Debezium CDC for lower latency |
| **RabbitMQ STOMP relay bottleneck** | Low | High | Cluster RabbitMQ; use quorum queues; monitor connection count per node |
| **Event schema evolution** | Medium | Medium | Version events in payload; use Avro/Protobuf or JSON with backward-compatible fields |
| **Distributed tracing complexity** | Medium | Medium | Phase 8 observability; correlate trace IDs across outbox → RabbitMQ → consumer |
| **Duplicate event processing** | Medium | High | All consumers implement idempotency via `eventId` deduplication (Phase 4.4); store processed event IDs with TTL |

---

## Resource Allocation

| Role | Phase 0-1 | Phase 2-4 | Phase 5-8 |
|------|-----------|-----------|-----------|
| Platform/Architecture | 1.0 FTE | 0.5 FTE | 0.5 FTE |
| Backend Engineers | 2.0 FTE | 2.0 FTE | 2.0 FTE |
| QA/Testing | 0.5 FTE | 0.5 FTE | 1.0 FTE |
| DevOps/Infra (RabbitMQ, Virtual Threads tuning) | 0.5 FTE | 0.5 FTE | 0.5 FTE |

---

## Approval & Tracking

- **Document Owner**: Tech Lead / Platform Team
- **Review Cadence**: Bi-weekly architecture review
- **Tracking**: GitHub Project board with phases as milestones
- **Definition of Done**: All ArchUnit tests pass; build succeeds; test coverage ≥80% per module; load test validates 100K+ concurrent WebSocket connections

---

*Plan created: 2026-09-21*  
*Updated: 2026-09-21 (incorporated industry review: Transactional Outbox, Virtual Threads, RabbitMQ STOMP Relay)*  
*Based on: `docs/architecture/architecture-analysis.md`*  
*Related: `docs/plans/feature-based-restructure-plan.md` (supersedes Phases 1-12)*
# Architecture Improvement Plan

## Overview

This plan consolidates all architectural weaknesses, recommendations, and strategic directions from `architecture-analysis.md` into an actionable, phased improvement roadmap. The plan is organized by priority and dependency order to ensure each phase delivers measurable architectural improvements.

---

## Testing Convention (Mandatory for All Phases)

**Every implementation step must satisfy the following testing gates before moving to the next step:**

| Gate | Requirement | Command |
|------|-------------|---------|
| **Unit Tests** | All new/changed logic covered by JUnit 5 + Mockito tests (`*Test.java`) | `./gradlew test --tests "<affected_module>*"` |
| **Coverage** | ≥80% line coverage per module (enforced via JaCoCo) | `./gradlew jacocoTestReport` → verify `jacocoTestCoverageVerification` |
| **Architectural Rules** | ArchUnit tests pass (Phases 0+) | `./gradlew test --tests "*ArchitectureRulesTest*"` |
| **Integration Tests** | Required for Phase 4 (outbox, RabbitMQ), Phase 6 (multi-module), Phase 8 (observability) | `./gradlew test --tests "*FlowIT" --tests "*IntegrationTest*"` |
| **No Regressions** | Full test suite passes | `./gradlew test` |

**Definition of Done per Step**: All gates green → step complete. If tests don't exist for new code, create them first (TDD preferred).

**Git Commit Rule**: Upon completion of each step, **commit changes to git only after all testing gates pass**. No partial or failing commits. Each step = one clean commit (or squash-merge PR) with passing tests.

---

## Source Code Preservation Rule (Critical)

**The original monolith source code under `src/main/java/...` and `src/test/...` MUST NOT BE DELETED until the final step of Phase 8 is complete and all testing gates pass.**

During Phase 6 (Multi-Module Restructure):
- Files are **copied/moved** to new module locations (`core/`, `domain/`, `app/`)
- The original `src/` tree remains intact and buildable throughout
- Only after **all modules build independently**, **all tests pass** (unit + integration + ArchUnit), **coverage ≥80% per module**, and **full regression suite passes** — then the original `src/` tree may be removed

This ensures:
- Rollback capability at any point
- Continuous CI/CD from monolith during transition
- No "big bang" deletion risk
- Verification that new module structure is functionally identical before cleanup

---

## Phase 0: Foundation & Governance (Weeks 1-2)

### 0.1 Establish Architectural Guardrails

| Task | Description | Owner | Deliverable | Tests Required |
|------|-------------|-------|-------------|----------------|
| **ArchUnit Integration** | Add ArchUnit dependency; create baseline test suite | Platform Team | `ArchitectureRulesTest.java` in `shared/test/archunit/` | `ArchitectureRulesTest.java`: testNoCoreDependsOnDomain(), testNoDomainInternalAccess(), testRepositoriesPackagePrivate() |
| **Core Rules** | Enforce: no core→domain deps, no domain→domain internal access, repositories package-private | Platform Team | Passing ArchUnit tests in CI | Same as above; run `./gradlew test --tests "*ArchitectureRulesTest*"` |
| **Dependency Graph Baseline** | Generate current dependency graph (jdeps/Gradle) for comparison | Platform Team | `dependency-graph-baseline.dot` | Verify graph generation script works; no test code needed |

### 0.2 Code Quality Gates

| Task | Description | Owner | Deliverable | Tests Required |
|------|-------------|-------|-------------|----------------|
| **Static Analysis** | Configure SpotBugs, PMD, Checkstyle in CI | Platform Team | Pipeline integration | Verify all three tools run in CI; `./gradlew spotbugsMain pmdMain checkstyleMain` pass with zero violations |
| **Architecture Decision Log** | Create ADR template; document key decisions (Core/Domain split, Event-driven, etc.) | Tech Lead | `/docs/adr/` new entries | No unit tests; verify ADR template exists and sample ADRs render correctly |

### 0.3 Scalability Foundations (Java 25 Virtual Threads)

| Task | Description | Owner | Deliverable | Tests Required |
|------|-------------|-------|-------------|----------------|
| **Enable Virtual Threads** | Configure `spring.threads.virtual.enabled=true` in `application.yml`; verify Tomcat uses virtual threads for HTTP & WebSocket | Platform Team | Running app with virtual threads; load test baseline | `VirtualThreadsIntegrationTest.java`: verify Tomcat threads are virtual via `Thread.currentThread().isVirtual()`; load test with 10K concurrent connections |
| **Configure Virtual Thread Executor** | Update `AsyncConfig.java` to use `VirtualThreadPerTaskExecutor` for `@Async` event processing | Platform Team | `@Async` methods run on virtual threads | `AsyncConfigTest.java`: verify `@Async` methods execute on virtual threads; test exception propagation |
| **Connection Pool Tuning** | Tune HikariCP for virtual thread compatibility (`maxPoolSize` ≈ CPU cores × 2) | Platform Team | Stable DB connections under high concurrency | `HikariCPVirtualThreadTest.java`: stress test with 100+ concurrent DB operations; verify no connection exhaustion |
| **Pinning Detection Setup** | Configure JFR recording for `jdk.VirtualThreadPinned` events; add monitoring dashboard alert | Platform Team | JFR profile showing zero/low pinning; alert on sustained pinning | `PinningDetectionTest.java`: trigger known pinning scenario (synchronized block); verify JFR event captured |

**Rationale**: Java 25 (Project Loom) enables massive concurrency without WebFlux complexity. Virtual threads allow Tomcat to handle millions of concurrent WebSocket/REST connections and `@Async` event handlers to scale elastically.

**Critical Watch**: Thread pinning occurs when virtual threads enter `synchronized` blocks (common in older JDBC drivers, legacy libs). This blocks the carrier thread, negating virtual thread benefits. Monitor via JFR (`jdk.VirtualThreadPinned`) and avoid `synchronized` in hot paths — use `ReentrantLock` or `java.util.concurrent` primitives instead.

---

## Phase 1: Critical Fixes - Repository Leakage & Visibility (Weeks 3-4)

### 1.1 Fix `ChatController` Repository Injection (Critical)

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Extract message operations to `MessageService` | `chat/controller/ChatController.java` | Move `MessageRepository` usage to `MessageServiceImpl` | `MessageServiceImplTest.java`: addMessage(), getMessagesByChatId(), setMessageSeen(), getUnreadCount() — mock MessageRepository |
| Extract user operations to `UserService` | `chat/controller/ChatController.java` | Move `UserRepository` usage to `UserServiceImpl` | `UserServiceImplTest.java`: getSignedUser(), getUserById(), searchUsers() — mock UserRepository |
| Add service methods | `message/MessageService.java`, `user/UserService.java` | Define required operations as interface methods | Interface contract tests not needed; covered by impl tests |
| Implement in service layer | `message/MessageServiceImpl.java`, `user/UserServiceImpl.java` | Add `@Transactional`, authorization checks | `MessageServiceImplTest.java`, `UserServiceImplTest.java`: verify @Transactional rollback on exception; test authorization checks |
| Update controller | `chat/controller/ChatController.java` | Inject `MessageService`, `UserService` instead of repositories | `ChatControllerSliceTest.java`: test all endpoints with mocked services; verify no repository injection |

**Validation**: ArchUnit test fails if controller accesses repository directly.

### 1.2 Enforce Package-Private Repositories (Critical)

| Task | Files | Action | Tests Required |
|------|-------|--------|----------------|
| Change visibility | All `*Repository.java` files | Remove `public` modifier | ArchUnit test: `testRepositoriesPackagePrivate()`; verify all repos compile with package-private access |
| Fix test access | Test files in `src/test/` | Move to same package or use `@TestConfiguration` | All existing repository tests (`*RepositoryTest.java`) pass after visibility change |
| Update ArchUnit | `ArchitectureRulesTest.java` | Add rule: repositories must be package-private | `ArchitectureRulesTest.java`: testRepositoriesPackagePrivate() passes |

**Note**: Cross-feature repository access in `NotificationServiceImpl` is **deferred to Phase 4** where it will be resolved via Domain Events + Transactional Outbox, avoiding temporary `ReadService` interfaces that would be discarded.

---

## Phase 2: Shared Kernel Cleanup (Weeks 5-6)

### 2.1 Move Domain-Specific Enums to Owning Features

| Enum | Current Location | Target Location | Tests Required |
|------|------------------|-----------------|----------------|
| `ReactionType` | `shared/enums/ReactionType.java` | `reaction/ReactionType.java` | `ReactionTypeTest.java`: test enum values, serialization; update all referencing tests (`ReactionServiceImplTest`, `ReactionMapperTest`, `ReactionFlowIT`) |
| `NotificationType` | `shared/enums/NotificationType.java` | `notification/NotificationType.java` | `NotificationTypeTest.java`: test enum values; update `NotificationServiceImplTest`, `NotificationMapperTest`, `NotificationFlowIT` |
| `EntityType` | `shared/enums/EntityType.java` | `reaction/EntityType.java` (or shared if truly generic) | `EntityTypeTest.java`: test enum values; update `ReactionServiceImplTest`, `NotificationServiceImplTest` |
| `EventType` | `shared/enums/EventType.java` | `websocket/event/EventType.java` | `EventTypeTest.java`: test enum values; update `WebSocketConfigTest`, `SocketEventListenerTest`, WebSocket flow tests |

**Action**: Move files, update all imports across codebase, verify build.
**Verification**: Run `./gradlew test` — all tests must pass after enum relocation.

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

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Encapsulate follow logic | `user/User.java` | Add `follow(User)`, `unfollow(User)`, `block(User)`, `unblock(User)` methods | `UserTest.java`: test follow/unfollow/block/unblock behavior, duplicate follow prevention, self-follow prevention |
| Encapsulate profile settings | `user/ProfileSettings.java` | Add validation, business rules | `ProfileSettingsTest.java`: test validation rules (bio length, avatar URL format, etc.) |
| Remove setter misuse | `user/User.java` | Replace direct field sets with domain methods | `UserTest.java`: verify no public setters for business fields; all mutations via domain methods |
| Update `UserServiceImpl` | `user/UserServiceImpl.java` | Call domain methods instead of direct field manipulation | `UserServiceImplTest.java`: test followUser(), unfollowUser(), blockUser(), updateProfile() use domain methods |

### 3.2 Enrich `Post` Aggregate

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Encapsulate poll voting | `post/Post.java` | Add `votePoll(userId, optionId)` with validation | `PostTest.java`: test votePoll() validates option exists, prevents duplicate votes, updates vote counts |
| Encapsulate save/unsave | `post/Post.java` | Add `saveBy(User)`, `unsaveBy(User)` | `PostTest.java`: test saveBy()/unsaveBy() toggle saved state, prevent duplicate saves |
| Encapsulate repost | `post/Post.java` | Add `repostBy(User, text)` factory method | `PostTest.java`: test repostBy() creates new Post with correct reference, copies content |

### 3.3 Enrich `Chat` & `Message` Aggregates

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Encapsulate membership | `chat/Chat.java` | Add `addMember(User)`, `removeMember(User)` | `ChatTest.java`: test addMember()/removeMember() validate user not already member, update member list |
| Encapsulate message creation | `message/Message.java` | Factory method with validation | `MessageTest.java`: test factory method validates content not empty, sender is chat member |
| Add read receipts logic | `message/Message.java` | Domain methods for marking read | `MessageTest.java`: test markAsRead() updates seen status, prevents double-mark |

### 3.4 Enrich `Reaction` Aggregate

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Factory method | `reaction/Reaction.java` | `create(userId, username, contentId, contentType, reactionType)` | `ReactionTest.java`: test create() validates all params, sets correct enum, generates ID |
| Validation | `reaction/Reaction.java` | Prevent duplicate reactions, validate enum values | `ReactionTest.java`: test duplicate prevention (same user + content + type), invalid enum rejection |

**Note**: Entities now reference enums at their final locations (e.g., `reaction.ReactionType`), enabling domain methods to natively generate domain events (e.g., `user.follow()` triggers `UserFollowedEvent`) before Phase 4 infrastructure.

## Phase 4: Domain Events Implementation (Weeks 10-12)

### 4.1 Event Infrastructure — Transactional Outbox Pattern

| Task | Location (Monolith) | Action | Tests Required |
|------|---------------------|--------|----------------|
| **Outbox table** | `src/main/resources/db/migration/Vx__outbox_table.sql` | Create `outbox_events` table (id, aggregate_type, aggregate_id, event_type, payload, created_at, published_at) | `OutboxMigrationTest.java`: verify migration runs cleanly; `OutboxEventRepositoryTest.java`: test CRUD operations on outbox table |
| **Outbox entity** | `shared/event/OutboxEvent.java` | JPA entity mapping to `outbox_events` | `OutboxEventTest.java`: test entity mapping, serialization, equals/hashCode |
| **Outbox repository** | `shared/event/OutboxEventRepository.java` | Spring Data repository for outbox polling | `OutboxEventRepositoryTest.java`: test findUnpublishedEvents(), markAsPublished(), pagination |
| **Transactional publisher** | `shared/event/TransactionalEventPublisher.java` | Save event to outbox within same transaction as aggregate (via `@Transactional` + `ApplicationEventPublisher` hook) | `TransactionalEventPublisherTest.java`: test event saved in same TX as aggregate; verify rollback on failure; test publishEvent() with mocked outbox repo |
| **Outbox poller** | `shared/messaging/OutboxEventPoller.java` | **Start with `@Scheduled` Spring job** polling `outbox_events` (1-5s interval). Only introduce Debezium CDC if polling causes DB CPU spikes or latency is unacceptable for chat. | `OutboxEventPollerTest.java`: test pollAndPublish() processes unpublished events; test idempotency on retry; test error handling doesn't stop polling |
| **RabbitMQ exchange setup** | `config/RabbitMqConfig.java` | Declare topic exchange `domain.events`; bind queues per consumer domain | `RabbitMqConfigTest.java`: verify exchange/queue declarations; test message routing with Testcontainers RabbitMQ |
| **Domain event base** | `shared/event/DomainEvent.java` | Abstract base with timestamp, eventId, aggregateId, aggregateType | `DomainEventTest.java`: test serialization/deserialization; verify eventId is UUID, timestamp auto-generated |
| **Event serializer** | `shared/event/EventSerializer.java` | Jackson-based JSON serializer for outbox payload | `EventSerializerTest.java`: test serialize/deserialize roundtrip for all domain events; test unknown event handling |

**Why Transactional Outbox**: In-memory `@Async` + `@EventListener` loses events on JVM crash. Outbox guarantees **at-least-once delivery** by persisting events atomically with domain changes. RabbitMQ provides durable, ordered delivery across horizontally scaled instances.

**Poller Strategy**: Begin with simple `@Scheduled` poller. Monitor: (a) poll interval vs. latency requirements, (b) DB CPU from polling. Migrate to Debezium only if needed — adds operational complexity (Kafka Connect cluster).

**Note**: File paths use existing monolith packages. In Phase 6, these classes will be physically moved to `core-data`, `core-messaging`, `core-shared` modules with zero code changes — only package declarations and imports updated.

### 4.2 Define Domain Events

| Event | Publisher | Consumers | Tests Required |
|-------|-----------|-----------|----------------|
| `UserRegisteredEvent` | `auth/AuthServiceImpl` | `notification`, `user` (welcome) | `UserRegisteredEventTest.java`: test event structure; `AuthServiceImplTest`: test event published on registration; `NotificationEventListenerTest`: test welcome notification created |
| `PostCreatedEvent` | `post/PostServiceImpl` | `notification`, `chat` (mentions) | `PostCreatedEventTest.java`: test event structure; `PostServiceImplTest`: test event published on create; `NotificationEventListenerTest`: test mention notifications |
| `CommentCreatedEvent` | `comment/CommentServiceImpl` | `notification`, `post` (comment count) | `CommentCreatedEventTest.java`: test event structure; `CommentServiceImplTest`: test event published; `NotificationEventListenerTest`: test comment notifications; `PostServiceImplTest`: test comment count increment |
| `ReactionAddedEvent` | `reaction/ReactionServiceImpl` | `notification`, `post` (reaction count) | `ReactionAddedEventTest.java`: test event structure; `ReactionServiceImplTest`: test event published; `NotificationEventListenerTest`: test reaction notifications; `PostServiceImplTest`: test reaction count increment |
| `MessageSentEvent` | `message/MessageServiceImpl` | `notification`, `chat` (last message) | `MessageSentEventTest.java`: test event structure; `MessageServiceImplTest`: test event published; `ChatServiceImplTest`: test last message update; `NotificationEventListenerTest`: test message notifications |
| `UserFollowedEvent` | `user/UserServiceImpl` | `notification` | `UserFollowedEventTest.java`: test event structure; `UserServiceImplTest`: test event published on follow; `NotificationEventListenerTest`: test follow notification |

### 4.3 WebSocket Horizontal Scaling — External STOMP Broker

| Task | Location | Action | Tests Required |
|------|----------|--------|----------------|
| **Configure RabbitMQ STOMP relay** | `websocket/config/WebSocketConfig.java` | `registry.enableStompBrokerRelay("/topic", "/queue").setRelayHost("rabbitmq").setRelayPort(61613)` | `WebSocketConfigTest.java`: verify STOMP relay configured (not simple broker); integration test with Testcontainers RabbitMQ: test message routed across nodes |
| **User destination prefix** | `websocket/config/WebSocketConfig.java` | `registry.setUserDestinationPrefix("/user");` — messages routed via RabbitMQ to correct node | `WebSocketConfigTest.java`: verify user destination prefix set |
| **Session registry** | `websocket/event/listener/SocketEventListener.java` | Track connected users per node; sync via Redis if needed for presence | `SocketEventListenerTest.java`: test sessionAdded/sessionRemoved updates registry; test presence events published |
| **Heartbeat config** | `application.yml` | `spring.websocket.client.heartbeat: 30000` (30s) | `WebSocketConfigTest.java`: verify heartbeat config loaded; integration test: verify connection survives 30s idle |

**Rationale**: Default in-memory STOMP broker doesn't work across multiple backend instances. RabbitMQ STOMP relay routes messages to the node where the target user is connected, enabling horizontal scaling of WebSocket servers.

### 4.4 Refactor Consumers

| Consumer | Current Coupling | New Approach | Tests Required |
|----------|------------------|--------------|----------------|
| `NotificationServiceImpl` | Direct repo access | `@EventListener` on domain events (via RabbitMQ) | `NotificationEventListenerTest.java`: test @EventListener methods for each domain event; test idempotency via eventId deduplication; test notification created via event |
| `WebSocket` notifications | Direct service calls | Event-driven via STOMP (RabbitMQ relay) | `WebSocketNotificationFlowIT.java`: test notification delivered via STOMP to correct user session; test multi-node delivery with Testcontainers |

**Idempotency Requirement**: Since Outbox + RabbitMQ provides **at-least-once delivery**, all event consumers **must be idempotent**. Implement by:
- Including `eventId` (UUID) in every `DomainEvent`
- Consumers check `processed_events` table (or cache) for `eventId` before processing
- Example: `NotificationEventListener` → `if (notificationRepository.existsByEventId(event.getEventId())) return;`

**Idempotency Tests Required**:
- `ProcessedEventRepositoryTest.java`: test existsByEventId(), save()
- `NotificationEventListenerTest.java`: test duplicate eventId ignored; test processed_events table populated

---

## Phase 5: Security & WebSocket Decoupling (Weeks 14-15)

### 5.1 Dependency Inversion for Authentication

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Create core interface | `security/AuthenticationProvider.java` (new in core) | `UserDetails loadUserByUsername(String)` | `AuthenticationProviderTest.java`: interface contract test (not needed; covered by impl test) |
| Implement in user domain | `user/UserAuthenticationProvider.java` | Delegate to `UserRepository` | `UserAuthenticationProviderTest.java`: test loadUserByUsername() finds user; test UsernameNotFoundException for missing user |
| Update `WebSocketConfig` | `websocket/config/WebSocketConfig.java` | Inject `AuthenticationProvider` interface | `WebSocketConfigTest.java`: verify AuthenticationProvider injected; test STOMP CONNECT uses provider |
| Update `AppConfig` | `config/AppConfig.java` | Wire implementation | `AppConfigTest.java`: verify UserAuthenticationProvider bean created and wired |

### 5.2 JWT Service Abstraction

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Create interface | `security/JwtTokenService.java` | `generateToken(UserDetails)`, `validateToken(String)` | `JwtTokenServiceTest.java`: interface contract test (covered by impl) |
| Implement | `security/JwtTokenServiceImpl.java` | Current `JwtService` logic | `JwtTokenServiceImplTest.java`: test generateToken() includes claims; test validateToken() verifies signature, expiration; test expired/revoked token rejection |
| Update dependents | `JwtFilter`, `WebSocketConfig`, `AuthService` | Inject interface | `JwtFilterTest.java`: test filter uses JwtTokenService; `WebSocketConfigTest.java`: test handshake uses JwtTokenService; `AuthServiceTest.java`: test login/refresh use JwtTokenService |

### 5.3 WebSocket Virtual Thread Optimization

| Task | File | Action | Tests Required |
|------|------|--------|----------------|
| Configure STOMP client executor | `websocket/config/WebSocketConfig.java` | Use `Executors.newVirtualThreadPerTaskExecutor()` for inbound/outbound channels | `WebSocketConfigTest.java`: verify inbound/outbound executors are virtual thread executors |
| WebSocket handshake handler | `websocket/config/CustomHandshakeHandler.java` | Ensure non-blocking; offload auth to virtual thread executor | `CustomHandshakeHandlerTest.java`: test handshake completes on virtual thread; test auth delegation |
| Session registry concurrency | `websocket/event/listener/SocketEventListener.java` | Use `ConcurrentHashMap` with virtual-thread-friendly locking | `SocketEventListenerTest.java`: stress test concurrent session add/remove; verify no thread pinning under load |

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

| Step | Module | Depends On | Notes | Tests Required |
|------|--------|------------|-------|----------------|
| 1 | `core-shared` | — | BaseEntity, PagingProvider, BaseResponse, exceptions | `:core-shared:test` — all utility tests pass; ArchUnit: no domain deps |
| 2 | `core-data` | core-shared | JPA auditing, generic repositories | `:core-data:test` — repository base tests pass; ArchUnit: only depends on core-shared |
| 3 | `core-web` | core-shared | Exception handler, OpenAPI, base controllers | `:core-web:test` — controller base tests pass |
| 4 | `core-security` | core-shared, core-data | JWT, filter chain, AuthenticationProvider interface | `:core-security:test` — JWT, filter tests pass; ArchUnit: no domain deps |
| 5 | `core-messaging` | core-shared, core-security | RabbitMQ, EmailEvent, WebSocket config | `:core-messaging:test` — RabbitMQ, WebSocket config tests pass |
| 6 | `domain-auth` | core-* | AuthController, AuthService, DTOs | `:domain-auth:test` — AuthControllerSliceTest, AuthServiceTest, AuthFlowIT pass |
| 7 | `domain-user` | core-*, domain-auth | User aggregate, UserService, UserRepository | `:domain-user:test` — UserTest, UserServiceImplTest, UserControllerSliceTest, UserFlowIT pass |
| 8 | `domain-post` | core-*, domain-user | Post, Poll, PostService | `:domain-post:test` — PostTest, PostServiceImplTest, PostControllerSliceTest, PostFlowIT pass |
| 9 | `domain-photo` | core-*, domain-user, domain-post | Photo, PhotoService | `:domain-photo:test` — PhotoServiceImplTest, PhotoControllerSliceTest, PhotoFlowIT pass |
| 10 | `domain-comment` | core-*, domain-user, domain-post | Comment, CommentService | `:domain-comment:test` — CommentTest, CommentServiceImplTest, CommentControllerSliceTest, CommentFlowIT pass |
| 11 | `domain-reaction` | core-*, domain-user, domain-post, domain-comment | Reaction, ReactionService | `:domain-reaction:test` — ReactionTest, ReactionServiceImplTest, ReactionControllerSliceTest, ReactionFlowIT pass |
| 12 | `domain-chat` | core-*, domain-user | Chat, ChatService | `:domain-chat:test` — ChatTest, ChatServiceImplTest, ChatControllerSliceTest, ChatFlowIT pass |
| 13 | `domain-message` | core-*, domain-user, domain-chat | Message, MessageService | `:domain-message:test` — MessageTest, MessageServiceImplTest, MessageControllerSliceTest, MessageFlowIT pass |
| 14 | `domain-notification` | core-*, domain-user, domain-post, domain-comment, domain-message, domain-reaction, domain-chat | Notification, NotificationService | `:domain-notification:test` — NotificationTest, NotificationServiceImplTest, NotificationControllerSliceTest, NotificationFlowIT pass |
| 15 | `quietspace-app` | all domain modules | SpringBootApplication, wiring | `:quietspace-app:test` — QuietspaceApplicationIT, full integration tests pass |

### 6.3 Per-Module Tasks

For each domain module:
1. Create `build.gradle.kts` with dependencies
2. **Copy** source files to new module location (maintain package structure) — **DO NOT DELETE originals**
3. Update `package` declarations in new location
4. Fix imports in new location
5. Add module-specific ArchUnit tests
6. Verify `./gradlew :domain-xxx:build` passes
7. **Run `./gradlew :domain-xxx:test` — all tests pass with ≥80% coverage**
8. **Run `./gradlew :domain-xxx:archUnitTest` — architectural rules pass**
9. **Only after ALL modules pass steps 1-8**: delete original `src/` tree (final cleanup step, Phase 8)

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

| Module | Unit Tests | Slice Tests | Integration Tests | Tests Required |
|--------|------------|-------------|-------------------|----------------|
| Each domain | `*ServiceImplTest`, `*MapperTest` | `*ControllerSliceTest` | `*FlowIT` | All 3 categories ≥80% coverage; run `./gradlew :domain-xxx:test` |
| Core modules | Utility tests, config tests | N/A | N/A | `:core-xxx:test` ≥80% coverage |
| App | N/A | N/A | Full integration, contract tests | `:quietspace-app:test` — all integration tests pass |

**Testing Gate**: No module promoted until its test suite passes with ≥80% coverage.

---

## Phase 8: Observability & Documentation (Weeks 25-26)

### 8.1 Architecture Documentation

| Document | Location | Content | Tests Required |
|----------|----------|---------|----------------|
| **Core Module APIs** | `/docs/architecture/core/` | Public interfaces, extension points | No unit tests; verify docs generate via `./gradlew asciidoctor` or similar |
| **Domain Module Contracts** | `/docs/architecture/domains/` | Service interfaces, events published/consumed | No unit tests; verify cross-references valid |
| **Event Catalog** | `/docs/architecture/events.md` | All domain events with payloads | No unit tests; verify all Phase 4.2 events documented |
| **Migration Guides** | `/docs/guides/migration/` | How to add new domain module | No unit tests; verify guide accuracy by following it |

### 8.2 Observability Enhancements

| Area | Action | Tests Required |
|------|--------|----------------|
| **Distributed Tracing** | Add Micrometer Tracing + Zipkin/OpenTelemetry | `TracingIntegrationTest.java`: verify trace context propagates across outbox → RabbitMQ → consumer |
| **Metrics** | Domain-level metrics (event latency, handler duration) | `MetricsTest.java`: verify custom metrics registered and exported; test metric values under load |
| **Health Checks** | Per-domain health indicators | `HealthIndicatorTest.java`: test each domain health indicator returns UP/DOWN correctly |

**Verification**: All observability tests pass; dashboards show expected data.

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
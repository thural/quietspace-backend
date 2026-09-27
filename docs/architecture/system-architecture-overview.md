# System Architecture Overview — quietspace-platform

> **Purpose.** Factual reference of the codebase architecture *as it is* (main branch,
> post Phase 1.1 / 3.x / 4.x work).
> Claims below were verified against `build.gradle.kts` files, imports, and tests;
> package paths are exact.

---

## 1. Stack & Runtime

| Concern | Choice |
|---------|--------|
| Language / toolchain | Java 21 build target (JDK 25 runtime via toolchain), Gradle 8.14 (`gradle/wrapper/gradle-wrapper.properties`), Kotlin DSL |
| Framework | Spring Boot 4.1.0, Spring Data JPA (Hibernate), Spring Security, Spring WebSocket (STOMP), Spring AMQP |
| Concurrency | Virtual threads enabled (`spring.threads.virtual.enabled=true` in app `application.yml`) |
| Database / migrations | MySQL 8 (production), Flyway (`spring-boot-starter-flyway` + `flyway-mysql` in app), H2 (embedded, test slices only) |
| Messaging | RabbitMQ (domain events, transactional outbox relay), STOMP relay for WebSocket horizontal scaling |
| API docs | OpenAPI/Springdoc in `core-web` (`core/web/OpenApiConfig.java`); SpringWolf STOMP docs deps in `core-messaging` (docket YAML stays in app `application.yml`) |
| Observability | Actuator, Micrometer Prometheus registry, Micrometer Tracing (Brave) + Zipkin reporter, per-domain `*HealthIndicator` |
| Test infra | JUnit 5 + Mockito + AssertJ, ArchUnit per module, Testcontainers (MySQL, RabbitMQ) for app integration tests, H2 for domain `@DataJpaTest` slices |

Root project name: `quietspace-platform` (`settings.gradle.kts`). Shared build logic:
`buildSrc/.../quietspace.domain-conventions.gradle.kts` (domain deps, test gates,
Testcontainers baseline, H2 test runtime). Only `quietspace-app` produces a boot jar.

---

## 2. Module Map (15 modules)

```
core-shared ◄── core-data, core-web, core-security, core-messaging
core-security ◄── core-messaging
all core + selected domain modules ◄── each domain module (table below)
everything ◄── app/quietspace-app (composition root, sole @SpringBootApplication)
```

### 2.1 Core modules (library, zero domain dependencies — enforced by build files)

| Module | Gradle deps (project) | Owns |
|--------|----------------------|------|
| `core-shared` | — (root) | `BaseEntity`, `BaseResponse`, enums (`Role`, `StatusType`, `Permission`, `EntityType`, `ReactionType`, `EmailTemplateName`), exceptions, utils (`PagingProvider`, `PageUtils`, `ImageCompressionUtil`), event infra (`DomainEvent`, `TransactionalEventPublisher`, `OutboxEvent`, `OutboxEventRepository`, `ProcessedEvent`, `EventSerializer`), security primitives (`Token`, `TokenRepository`, `JwtTokenService`), shared ports (`UserProfilePort`, `WebSocketUserPort`) |
| `core-data` | `core-shared` | JPA auditing/base config |
| `core-web` | `core-shared` | Global exception advice, `OpenApiConfig` |
| `core-security` | `core-shared` | JWT filter chain, `AuthenticationProvider` interface, `JwtTokenServiceImpl` |
| `core-messaging` | `core-shared`, `core-security` | `WebSocketConfig` (STOMP + RabbitMQ relay), handshake handler/interceptor, `SocketEventListener`, STOMP message types, mail/AMQP, SpringWolf deps |

### 2.2 Domain modules (Gradle `implementation` edges — the real coupling)

| Module | Depends on (domain part) | Role |
|--------|--------------------------|------|
| `domain-user` | `domain-photo`, `domain-notification` | `User` aggregate; implements notification ports; reads photo views; IAM bounded context |
| `domain-post` | `domain-user`, `domain-photo`, `domain-reaction`, `domain-comment`, `domain-notification` | `Post`/`Poll` aggregates; implements notification/comment ports |
| `domain-photo` | *(none — leaf)* | `Photo` aggregate; consumes `UserProfilePort` from core-shared |
| `domain-comment` | `domain-user`, `domain-reaction`, `domain-notification` (+ `domain-post` **test-only**) | `Comment` aggregate |
| `domain-reaction` | `domain-user`, `domain-notification` (+ `domain-post` **test-only**) | `Reaction` aggregate |
| `domain-chat` | `domain-user` | `Chat` aggregate (owns message context) |
| `domain-message` | `domain-user`, `domain-chat`, `domain-photo` | `Message` aggregate (child of chat) |
| `domain-notification` | *(none — sink)* (+ `domain-user` **test-only**) | `Notification` aggregate; owns `port/` interfaces implemented by providers |

Critical edges for redesign discussions:
- **`user→photo` exists** (`UserServiceImpl` → `PhotoService.deletePhotoByEntityId`). Therefore a `photo→user` edge (which moving `UserProfilePort` into `domain-user.api` would require) creates a **user↔photo cycle** — Gradle fails the build. This is why the port stays in `core-shared`.
- **`message→chat` is aggregate ownership** (`Message.chat` `@ManyToOne`), deliberately kept per plan §5.1 — not a layering violation to "fix".
- Test-only edges (`comment/reaction→post`, `notification→user`) exist solely for slice-test fixtures and don't affect production coupling or ArchUnit (tests excluded from ArchUnit imports).

### 2.3 App module

`app/quietspace-app` = `QuietspaceApplication` (`dev.thural.quietspace` package, file lives under `.../quietspace/app/`), `application.yml`, Flyway migrations, and **all integration tests** (14 FlowITs, `UserServiceIT`, security-posture tests, `FlywayMigrationIT`, `QuietspaceApplicationIT`). It is the only module that can host full-context `@SpringBootTest`s.

---

## 3. Package Layout (flat root + subpackages)

Domains do **not** use a layered `service/repository/model` package tree. Typical layout
(`domain-user` shown; others analogous):

```
dev.thural.quietspace.domain.user/
├── User.java, UserService.java, UserServiceImpl.java, UserRepository.java,
│   UserMapper.java, UserQuery.java, ProfileSettings.java   # flat root
├── api/                # public query contracts (Phase 3.1): *QueryPort, dto/*.java (records)
├── adapter/            # consumer-port implementations (notification, websocket, profile)
├── auth/               # AuthService (user domain hosts auth)
├── controller/         # REST controllers (+ auth/controller)
├── dto/                # request/response DTOs (mutable Lombok, extend BaseResponse)
├── health/             # *HealthIndicator
└── security/           # UserAuthenticationProvider, beans
```

`domain-post` adds `PostSecurityService` (SpEL bean `postSecurity`), `PostSpecifications`, `PostVisibilityProjector`;
`domain-comment` has `CommentCommandPort`/`Adapter` + `CommentQueryPort` + `CommentQueryAdapter`;
`domain-notification` adds `NotificationEventListener` + `port/` (consumer-owned ports);
`domain-chat` has `port/ChatMessagePort`; `domain-comment` has `port/CommentPostPort`;
`domain-reaction` has `ReactionQueryPort`/`Adapter`; `domain-photo` has `PhotoQueryPort`/`Adapter`;
`domain-post` has `PostQueryPort`/`Adapter`; `domain-user` has `UserQueryPort`/`Adapter`.

> **Caveat for reviewers:** per-module ArchUnit tests now reference **real packages**
> (`domain.post..`, `domain.user..`, etc.) with explicit forbidden-dependency lists and
> a cross-module repository-import ban. No vacuous `allowEmptyShould(true)` rules.

---

## 4. Port Inventory

### 4.1 Core-shared SPI (stays in core — infrastructure, not domain queries)

| Port | Provider | Consumers | Rationale for core placement |
|------|----------|-----------|------------------------------|
| `UserProfilePort` (`currentUserId`, `setProfilePhoto`) | `domain-user/.../adapter/UserProfileAdapter` | `domain-photo/PhotoServiceImpl` (`@Lazy` inject) | Photo is a leaf; moving it would force a `photo→user` edge → cycle with `user→photo` |
| `WebSocketUserPort` | `domain-user/.../adapter/WebSocketUserAdapter` | `core-messaging` WebSocket auth | Core-driven outbound SPI |

### 4.2 Domain `api` query ports (Phase 3.1/3.4, additive, record DTOs)

| Port (module) | Methods | DTO |
|---------------|---------|-----|
| `UserQueryPort` (user) | `getUserSummary`, `getUsersSummary`, `findUserIdByUsernameOrEmail`, `searchUserIds` | `UserSummaryDTO(id, username, displayName, photoId, statusType)` |
| `PhotoQueryPort` (photo) | `getPhotoSummary`, `getPhotoSummaryByEntityId`, `getPhotosSummary` | `PhotoSummaryDTO(id, name, type, userId, entityId, entityType)` |
| `PostQueryPort` (post) | `getPostSummary`, `getPostsSummary` | `PostSummaryDTO(id, authorId, title, text, photoId)` |
| `CommentQueryPort` (comment) | `getCommentSummary`, `getCommentsSummary`, `countCommentsByPostId`, `findPostIdsByUserId` | `CommentSummaryDTO(id, postId, authorId, parentId, text)` |
| `ReactionQueryPort` (reaction) | `getReactionSummary`, `getReactionsSummary`, `countReactions` | `ReactionSummaryDTO(id, userId, username, contentId, contentType, reactionType)` |

Adapters (`*QueryAdapter`, `@Component`) delegate to the owning repository; unit-tested
with Mockito. No consumer rewiring yet, except:
- `CommentMapper.getLikeCount` → `ReactionQueryPort.countReactions`
- `CommentMapper.resolveUsername` → `UserQueryPort.getUserSummary`
- `MessageMapper.resolveUsername` → `UserQueryPort.getUserSummary`
- `PostMapper.resolveUsername` → `UserQueryPort.getUserSummary`
- `PostMapper.commentCount` → `CommentQueryPort.countCommentsByPostId`
- `PostSecurityService.canAccess` → `UserQueryPort.findUserIdByUsernameOrEmail`
- `PostSpecifications.containsText` → `UserQueryPort.searchUserIds`
- `PostSpecifications.commentedByUser` → `CommentQueryPort.findPostIdsByUserId`
- `PostSpecifications.visibleToUser` → `PostAuthorVisibility` + `ViewerAuthorAccess` (local tables)

Auth (`getSignedUser`) intentionally stays on `UserService` — ports carry display data only.

### 4.3 Consumer-owned ports (`port/` packages, provider implements)

| Port | Owner module | Provider adapter | Purpose |
|------|--------------|------------------|---------|
| `NotificationUserPort` | notification | `UserNotificationAdapter` | Resolve notification recipient |
| `NotificationPostPort` | notification | `PostNotificationAdapter` | Find post owner for notifications |
| `NotificationCommentPort` | notification | `CommentNotificationAdapter` | Find comment owner for notifications |
| `ChatMessagePort` | chat | `MessageChatAdapter` | Message send entry point |
| `CommentPostPort` | comment | `PostCommentAdapter` | Verify post exists for comment creation |
| `CommentCommandPort` | comment | `CommentCommandAdapter` | **Explicit cascade** — post deletion triggers comment cleanup |

Adapters live in provider modules; consumer modules depend only on the port interfaces.

---

## 5. Events & Messaging

- **Transactional outbox**: `OutboxEvent` + `OutboxEventRepository` (core-shared), written
  atomically with aggregates via `TransactionalEventPublisher`; `ProcessedEvent` table
  gives consumers idempotency (`eventId` dedup).
- **Domain events** (`core-shared/event/`): `UserRegisteredEvent`, `UserFollowedEvent`,
  `UserUnfollowedEvent`, `UserPrivacyChangedEvent`, `PostCreatedEvent`, `CommentCreatedEvent`,
  `ReactionAddedEvent`, `MessageSentEvent` (+ `EmailEvent`, `DomainEvent` base, `EventSerializer`).
- **Consumers**: `NotificationEventListener` (all 6+ domain events → notifications),
  `SocketEventListener` (presence), chat last-message updates, `PostVisibilityProjector`
  (feed-visibility read model from `UserPrivacyChangedEvent`, `UserFollowedEvent`,
  `UserUnfollowedEvent`, `UserRegisteredEvent`).
- **Transport**: RabbitMQ (durable, at-least-once); STOMP broker relay in
  `core-messaging/.../config/WebSocketConfig.java` for multi-node delivery;
  `WebSocketSecurityConfig` (CSRF disabled for handshake, protocol frames permitted).

---

## 6. Persistence & Shared Kernel

- Entities use Lombok `@SuperBuilder` over `core-shared/entity/BaseEntity.java`
  (id, audit fields). Cross-aggregate JPA associations exist and are load-bearing:
  `Message.chat` (ownership, kept), `Message.sender/recipient` → decoupled to plain
  `senderId`/`recipientId` UUID columns (E.2), `Comment.user` → plain `userId` (E.1),
  `Post.user` → **transitional dual mapping**: association retained as single writer for
  feed-privacy SQL joins (`PostSpecifications.visibleToUser`), plus read-only `authorId`
  FK view used by all new code (E.3). Ports return detached snapshots and **cannot**
  populate associations — which is why the remaining joins stay.
- **Removed**: `Post.comments` collection (E.1). `Post.comments` was `@OneToMany` with
  cascade; replaced by `CommentQueryPort.countCommentsByPostId` (mapper), subquery
  `findByCommentsUserId` (spec), and `CommentCommandPort.deleteAllByPostId` (cascade).
- Response DTOs (`*Response`) are mutable Lombok `@SuperBuilder` hierarchies extending
  `BaseResponse`, built by hand-written `@Component` mappers (no MapStruct `@Mapper`
  interfaces in domains). Records are used only for the new `api.dto` summaries.
- Test persistence: domain `@DataJpaTest` slices run on **H2** (`MODE=MYSQL`,
  `NON_KEYWORDS=user` — `user` is an H2 reserved word) with
  `@AutoConfigureTestDatabase(Replace.NONE)`; app integration tests use
  Testcontainers MySQL + RabbitMQ (`TestcontainersConfig`).

---

## 7. Security

- `core-security`: JWT filter chain, `JwtTokenServiceImpl`, `AuthenticationProvider`
  interface → implemented by `domain-user/.../security/UserAuthenticationProvider`
  (backed by `UserRepository`).
- Method security: `@PreAuthorize("@postSecurity.canAccess(#postId, authentication.name)")`
  on selected `PostController` endpoints; `PostSecurityService` resolves username→entity
  via **`UserQueryPort.findUserIdByUsernameOrEmail`** (no direct `UserRepository`).
- Security-posture tests (`PostControllerSecurityTest`, `CorsSecurityTest`, `CsrfTest`,
  `SecurityHeadersTest`, `SecurityFlowIT`) require the wired filter chain and live in app:
  a `@WebMvcTest` slice without `core-security` returns 200/404 instead of 401 (verified).

---

## 8. Testing Architecture (actual)

| Layer | Location | Tech |
|-------|----------|------|
| Unit (services, mappers, adapters, events, utils) | each module `src/test` (Mockito, no context) | fast |
| Slice — web | `*ControllerSliceTest` (`@WebMvcTest`, `addFilters=false`) | per domain |
| Slice — persistence | `*.repository.*RepositoryTest` (`@DataJpaTest`, H2) — moved out of app in Phase 1.1 | per domain |
| Contract | `*.api.*QueryAdapterTest` (Mockito) | per `api` port |
| Architecture | `*.archunit.*ArchitectureRulesTest` (+ global `ArchitectureRulesTest`) | per module (see §3 caveat) |
| Integration | app only: `*FlowIT`, `UserServiceIT`, `WebSocketFlowIT`, `FlywayMigrationIT` (`@SpringBootTest` + Testcontainers) | full context |

**H2 rules** (all three mandatory): (1) `application.yml` with `NON_KEYWORDS=user`; (2)
`@AutoConfigureTestDatabase(Replace.NONE)`; (3) `testRuntimeOnly("com.h2database:h2")`
from domain-conventions plugin.

---

## 9. Open Design Questions (resolved)

1. **FlowIT placement**: ✅ Accept app as integration-test module; criterion → "zero unit/slice tests in app".
2. **`UserProfilePort`**: ✅ Keep in `core-shared` (outbound SPI). Move would create `user↔photo` cycle.
3. **Response DTOs**: ✅ Freeze mutable hierarchy; records only for new `api.dto` summaries.
4. **Phase 4 visibility**: ✅ Drop `package-private` enforcement; enforce via corrected ArchUnit rules.
5. **Remaining direct repo access**: `Message*→ChatRepository` (ownership — keep);
   `PostSecurityService` → `UserRepository` eliminated (now uses `UserQueryPort`);
   `CommentMapper`/`ReactionMapper`/`MessageMapper`/`PostMapper`/`PostSecurityService`
   all use query ports; `Post.comments` collection removed.

---

## 10. CI/CD

- **Jobs**: `test-core` (5 core modules), `test-domain` (8 domain modules), `test-app`
  (app integration tests), `build-and-push` (Docker, needs all three).
- **Paths filter**: triggers on `core/**`, `domain/**`, `app/**`, `buildSrc/**`, `gradle/**`.
- **Caches**: `org.gradle.caching=true`, `org.gradle.configuration-cache=true` in `gradle.properties`.
- **Timeouts**: 15/20/30 min per job; no flaky-test retry plugin (timeouts preferred).

---

## 11. ADRs

| ADR | Title | Status |
|-----|-------|--------|
| 001 | Core/Domain Split Architecture | Accepted |
| 002 | Virtual Threads (Project Loom) | Accepted |
| 003 | Transactional Outbox Pattern | Accepted |
| 004 | IAM Bounded Context in domain-user | Accepted |
| 005 | Feed-Visibility Read Model (Event-Driven) | Accepted |

---

*Document maintained alongside `docs/guides/testing.md` (testing conventions).*
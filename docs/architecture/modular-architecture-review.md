# QuietSpace Platform - Modular Architecture Review

## Executive Summary

The QuietSpace backend architecture implements a strict **Modular Monolith (Modulith)** utilizing **Domain-Driven Design (DDD)** and **Hexagonal Architecture** principles. This review documents the architectural assessment covering module separation, internal structure, inter-module communication patterns, and build-time enforcement mechanisms.

---

## 1. Architectural Design and Separation of Concerns

### Two-Tier Architecture

The architecture strictly divides the system into two primary tiers:

| Tier | Modules | Responsibility | Dependencies |
|------|---------|----------------|--------------|
| **Core (Infrastructure)** | `core-shared`, `core-data`, `core-web`, `core-security`, `core-messaging` | Shared infrastructure, cross-cutting concerns | **Zero** domain dependencies |
| **Domain (Business Logic)** | `domain-user`, `domain-post`, `domain-comment`, `domain-reaction`, `domain-chat`, `domain-message`, `domain-notification`, `domain-photo` | Self-contained vertical slices, single aggregate ownership | Core modules only |

### Core Modules

Core modules provide **reusable technical scaffolding** with strictly **zero dependencies on any domain modules**:

| Module | Package | Key Responsibilities |
|--------|---------|---------------------|
| `core-shared` | `dev.thural.quietspace.core.shared` | Base entities, value objects, utilities, domain events, outbox infrastructure, email abstraction |
| `core-data` | `dev.thural.quietspace.core.data` | JPA configuration, auditing, repository base classes, HikariCP tuning, Flyway integration |
| `core-web` | `dev.thural.quietspace.core.web` | Global exception handling, OpenAPI config, pagination, base controllers, health endpoints |
| `core-security` | `dev.thural.quietspace.core.security` | JWT filter chain, authentication manager, password encoding, authority evaluators |
| `core-messaging` | `dev.thural.quietspace.core.messaging` | Event bus abstraction, RabbitMQ config, STOMP/WebSocket relay, email event pipeline |

**Reusability Guarantee**: Because core modules know nothing about "User", "Post", or "Chat", this exact `core` directory can be extracted and used to bootstrap a completely different system (e.g., e-commerce) with zero modifications.

### Domain Modules

Each domain module is a **self-contained vertical slice** owning a **single aggregate** and its behavior:

| Module | Aggregate(s) | Key Characteristics |
|--------|--------------|---------------------|
| `domain-user` | `User`, `ProfileSettings`, `Follow` | IAM, authentication provider, user lifecycle events |
| `domain-post` | `Post`, `Poll`, `PollOption`, `PollVote`, `Save`, `Repost` | Content creation, feeds, polls, visibility projection |
| `domain-comment` | `Comment`, `CommentReaction` | Discussion threads, replies, comment reactions |
| `domain-reaction` | `Reaction` | Generic reactions on any content type |
| `domain-chat` | `Chat`, `ChatMember` | Chat rooms, membership, last-message tracking |
| `domain-message` | `Message`, `ReadReceipt` | Message persistence, read status, unread counts |
| `domain-notification` | `Notification` | Sink module — consumes 8+ event types, no outbound deps |
| `domain-photo` | `Photo`, `PhotoMetadata` | Leaf module — upload, storage, metadata, no consumers |

### Dependency Rules (Enforced at Compile & Test Time)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ALLOWED DEPENDENCIES                         │
├─────────────────────────────────────────────────────────────────────┤
│  domain-* ──────→ core-*     (Domain MAY depend on Core)           │
│  domain-* ──────✗ domain-*   (Domain MUST NOT depend on Domain)    │
│  core-* ───────✗ domain-*    (Core MUST NOT depend on Domain)      │
└─────────────────────────────────────────────────────────────────────┘
```

- **Domain modules MAY depend on core modules** (explicit `implementation(project(":core:core-*"))`)
- **Domain modules MUST NOT depend on other domain modules' internal packages** (adapter, service, controller, model, repository)
- **Domain modules MAY implement ports owned by other domains** (Consumer-Owned Ports pattern)

---

## 2. Module Internal Structure and Encapsulation

### Package-Private Persistence

**Repositories are package-private** — physical encapsulation prevents external access:

```java
// domain/user/repository/UserRepository.java
package dev.thural.quietspace.domain.user.repository;  // package-private

interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(Email email);
    Optional<User> findByUsername(String username);
}
```

**Enforcement Mechanisms:**
- No `public` modifier on repository interfaces
- ArchUnit test: `classes().that().resideInAPackage("..repository..").should().notBeAccessed().fromOutside()`
- Gradle: Domain modules don't export repository packages in `api` configuration

### Cross-Domain Query Ports (Phase 3.1/3.4)

For display data needs, domains expose **lightweight, immutable read-models** via query ports:

```
<domain-module>/
├── api/
│   ├── dto/                    # Java record DTOs (immutable)
│   │   ├── UserSummaryDTO.java
│   │   ├── PostSummaryDTO.java
│   │   └── ...
│   └── port/
│       └── UserQueryPort.java  # Interface only
```

**Port Contract Example:**
```java
// domain/user/api/port/UserQueryPort.java
public interface UserQueryPort {
    Optional<UserSummaryDTO> getUserSummary(UUID userId);
    Set<UserSummaryDTO> getUsersSummary(Set<UUID> userIds);
    Optional<UUID> findUserIdByUsernameOrEmail(String identifier);
    Set<UUID> searchUserIds(String query);
}
```

**Adapter Implementation (in provider module):**
```java
// domain/user/api/adapter/UserQueryAdapter.java
@Component
@RequiredArgsConstructor
class UserQueryAdapter implements UserQueryPort {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Optional<UserSummaryDTO> getUserSummary(UUID userId) {
        return userRepository.findById(userId).map(userMapper::toSummaryDTO);
    }
    // ...
}
```

**Key Principles:**
- Ports return **Java records** (immutable, serializable, no lazy loading)
- Ports live in **consumer-accessible** `api` package
- Adapters delegate to **package-private repositories**
- No domain entities leak across module boundaries

---

## 3. Inter-Module Communication

To maintain decoupled domain boundaries, modules communicate **exclusively** through two standardized patterns.

### Pattern A: Consumer-Owned Ports (Synchronous Queries/Commands)

**Reverses traditional dependency direction:**

```
┌─────────────────────────────────────────────────────────────────────┐
│  CONSUMER MODULE (e.g., domain-notification)                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ interface NotificationPostPort {                            │   │
│  │     UUID findPostOwnerId(UUID postId);                      │   │
│  │ }                                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                ▲
                                │ implements
                                │
┌─────────────────────────────────────────────────────────────────────┐
│  PROVIDER MODULE (e.g., domain-post)                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ class PostNotificationAdapter implements                     │   │
│  │     NotificationPostPort {                                   │   │
│  │     UUID findPostOwnerId(UUID postId) {                      │   │
│  │         return postRepository.findAuthorIdById(postId);      │   │
│  │     }                                                        │   │
│  │ }                                                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

**Dependency Flow:** `consumer → port (own)` + `provider → port (depends on consumer)`

| Consumer (Owns Port) | Port Interface | Provider (Implements) | Adapter Class |
|---------------------|----------------|----------------------|---------------|
| `domain-notification` | `NotificationUserPort` | `domain-user` | `UserNotificationAdapter` |
| `domain-notification` | `NotificationPostPort` | `domain-post` | `PostNotificationAdapter` |
| `domain-notification` | `NotificationCommentPort` | `domain-comment` | `CommentNotificationAdapter` |
| `domain-comment` | `CommentPostPort` | `domain-post` | `PostCommentAdapter` |
| `domain-chat` | `ChatUserPort` | `domain-user` | `WebSocketUserAdapter` |
| `core-messaging` | `WebSocketUserPort` | `domain-user` | `WebSocketUserAdapter` |

**Benefits:**
- Consumer controls the contract (only methods it needs)
- Provider can change implementation without touching consumer
- Compile-time verification of contract satisfaction
- Enables microservice extraction (port becomes service interface)

### Pattern B: Domain Events (Asynchronous State Propagation)

**All domain events defined in `core-shared`:**

```java
// core-shared/src/main/java/.../event/UserRegisteredEvent.java
public record UserRegisteredEvent(
    UUID eventId,
    Instant timestamp,
    UUID userId,
    String username,
    String email
) implements DomainEvent { }
```

#### Event Publication: Transactional Outbox Pattern

```
┌─────────────┐     Same Transaction      ┌──────────────┐
│  Domain     │ ─────────────────────────→ │  Outbox      │
│  Service    │  1. Save Aggregate         │  Table       │
│             │  2. Persist DomainEvent    │  (OutboxEvent)│
└─────────────┘                             └──────┬───────┘
                                                   │
                                                   ▼
                                    ┌────────────────────────┐
                                    │  OutboxPoller          │
                                    │  (core-messaging)      │
                                    │  1. Poll unpublished   │
                                    │  2. Serialize to JSON  │
                                    │  3. Publish to RabbitMQ│
                                    │  4. Mark published     │
                                    └───────────┬────────────┘
                                                │
                                                ▼
                              ┌───────────────────────────────────┐
                              │  RabbitMQ Topic Exchange           │
                              │  "domain.events"                   │
                              │  Routes to domain-specific queues  │
                              │  + STOMP WebSocket (/topic/...)    │
                              └───────────────────────────────────┘
```

**Implementation:**
```java
// In domain service (e.g., UserServiceImpl)
@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {
    private final TransactionalEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Transactional
    public User register(RegistrationCommand cmd) {
        User user = User.create(cmd);
        userRepository.save(user);

        // Event persisted atomically with user
        eventPublisher.publish(new UserRegisteredEvent(
            UUID.randomUUID(), Instant.now(),
            user.getId(), user.getUsername(), user.getEmail()
        ));
        return user;
    }
}
```

#### Event Consumption: Idempotent Listeners

```java
// domain/notification/NotificationEventListener.java
@Component
@RequiredArgsConstructor
class NotificationEventListener {
    private final ProcessedEventRepository processedEvents;
    private final NotificationService notificationService;

    @EventListener
    @Transactional
    public void handle(UserRegisteredEvent event) {
        // Idempotency check
        if (processedEvents.existsByEventId(event.eventId())) {
            return; // Already processed
        }

        notificationService.createWelcomeNotification(event.userId());

        // Record processed event
        processedEvents.save(new ProcessedEvent(event.eventId(), event.getClass().getSimpleName()));
    }
}
```

**Guarantees:**
- **At-least-once delivery** via outbox polling
- **Idempotency** via `processed_events` table (PK = eventId)
- **Eventual consistency** — no synchronous blocking
- **Failure isolation** — consumer failure doesn't roll back producer

#### Event Catalog

| Event | Publisher | Consumers | Payload |
|-------|-----------|-----------|---------|
| `UserRegisteredEvent` | domain-user | domain-notification | userId, username, email |
| `PostCreatedEvent` | domain-post | domain-notification | postId, authorId, title, text |
| `CommentCreatedEvent` | domain-comment | domain-notification | commentId, postId, authorId, text |
| `ReactionAddedEvent` | domain-reaction | domain-notification | reactionId, contentId, contentType, reactionType, actorId |
| `MessageSentEvent` | domain-message | domain-notification, domain-chat | messageId, chatId, senderId, text |
| `UserFollowedEvent` | domain-user | domain-notification, domain-post (projector) | followerId, followedId |
| `UserUnfollowedEvent` | domain-user | domain-post (projector) | followerId, followedId |
| `UserPrivacyChangedEvent` | domain-user | domain-post (projector) | userId, isPrivate |
| `EmailEvent` | any (via EmailEventPublisher) | core-messaging (EmailEventConsumer) | to, subject, templateName, variables |

---

## 4. Assembly and Enforcement via Gradle

The Gradle multi-module build system is the **primary enforcement mechanism** for architectural boundaries.

### 4.1 Explicit Module Declaration

`settings.gradle.kts` explicitly defines all 15 modules:

```kotlin
// settings.gradle.kts
rootProject.name = "quietspace"

// Core modules
include("core:core-shared")
include("core:core-data")
include("core:core-web")
include("core:core-security")
include("core:core-messaging")

// Domain modules
include("domain:domain-user")
include("domain:domain-post")
include("domain:domain-comment")
include("domain:domain-reaction")
include("domain:domain-chat")
include("domain:domain-message")
include("domain:domain-notification")
include("domain:domain-photo")

// Application assembly
include("app:quietspace-app")

// Build logic
include("buildSrc")
```

### 4.2 Centralized Build Logic

Build logic maintained as **Kotlin code in `buildSrc`**:

```
buildSrc/
├── src/main/kotlin/
│   ├── conventions/
│   │   ├── quietspace-java-conventions.gradle.kts      # Java toolchain, encoding
│   │   ├── quietspace-test-conventions.gradle.kts      # JUnit, Testcontainers, ArchUnit
│   │   ├── quietspace-core-conventions.gradle.kts      # Core module defaults
│   │   └── quietspace-domain-conventions.gradle.kts    # Domain module defaults
│   └── dependencies/
│       └── Versions.kt                                  # Centralized version catalog
└── build.gradle.kts
```

**Domain Convention Plugin** (`quietspace-domain-conventions.gradle.kts`):
```kotlin
// Applies to all domain modules
plugins {
    id("io.github.archunit")  // ArchUnit test support
}

dependencies {
    // Core dependencies (explicit allowlist)
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-security"))
    implementation(project(":core:core-messaging"))

    // Test dependencies
    testImplementation(platform(versions.junitPlatform))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.tngtech.archunit:archunit-junit5")
}

// ArchUnit test configuration
tasks.withType<Test> {
    useJUnitPlatform()
    // Fail build on architecture violations
}
```

### 4.3 Executable Isolation

**Only `quietspace-app` produces an executable JAR:**

```kotlin
// quietspace-app/build.gradle.kts
plugins {
    id("org.springframework.boot")  // Enables bootJar
}

tasks.named<BootJar>("bootJar") {
    mainClass.set("dev.thural.quietspace.QuietspaceApplication")
}
```

**All core and domain modules explicitly disable `bootJar`:**

```kotlin
// In core-* and domain-* build.gradle.kts
tasks.named<BootJar>("bootJar") {
    enabled = false  // Compile as library JAR only
}
```

**Benefits:**
- Prevents accidental coupling (no main class in libraries)
- Ensures immediate readiness for microservice extraction
- Clear separation: libraries vs. deployable artifact
- Enforces module boundaries at build time

### 4.4 Automated Quality Gates

Root build applies plugins across **all subprojects**:

```kotlin
// build.gradle.kts (root)
subprojects {
    // Static analysis
    id("com.github.spotbugs") version "6.2.0" apply false
    id("pmd") apply false
    id("checkstyle") apply false

    // Configure to fail build on violations
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        ignoreFailures = false
        effort = "max"
        reportLevel = "high"
    }
    tasks.withType<PmdTask> {
        ignoreFailures = false
        ruleSets = listOf("category/java/bestpractices.xml", "category/java/design.xml")
    }
    tasks.withType<CheckstyleTask> {
        ignoreFailures = false
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }
}
```

**Enforced Standards:**
- **SpotBugs**: Detects bug patterns (null derefs, resource leaks, etc.)
- **PMD**: Code style, design issues, unused code
- **Checkstyle**: Formatting, naming conventions, import order

### 4.5 ArchUnit Synergies

**Gradle dependencies = Explicit Allowlist** → **ArchUnit = Runtime Verification**

```java
// domain/user/src/test/.../DomainUserArchitectureRulesTest.java
@AnalyzeClasses(packages = "dev.thural.quietspace.domain.user")
class DomainUserArchitectureRulesTest {

    // 1. No cross-module repository imports
    @ArchTest
    static final ArchRule no_cross_module_repository_dependency =
        noClasses()
            .should()
            .accessClassesThat()
            .resideInAnyPackage("..domain..repository..")
            .fromOutsideOfModule("domain-user");

    // 2. No internal package access
    @ArchTest
    static final ArchRule no_internal_package_access =
        noClasses()
            .that().resideOutsideOfPackage("..domain.user.internal..")
            .should()
            .accessClassesThat()
            .resideInAPackage("..domain.user.internal..");

    // 3. Domain modules only depend on allowed modules
    @ArchTest
    static final ArchRule domain_dependency_rules =
        slices()
            .matching("dev.thural.quietspace.domain.(*)..")
            .should()
            .onlyDependOn(
                "dev.thural.quietspace.core..",
                "dev.thural.quietspace.domain.user..",  // self
                "java..", "jakarta..", "org.springframework.."
            );

    // 4. Package-private repositories not exposed
    @ArchTest
    static final ArchRule repositories_not_public =
        classes()
            .that().resideInAPackage("..repository..")
            .should()
            .notBePublic();

    // 5. Services only implement ports, not expose internals
    @ArchTest
    static final ArchRule services_implement_ports_only =
        classes()
            .that().resideInAPackage("..service..")
            .and().haveSimpleNameEndingWith("Impl")
            .should()
            .notBePublic();
}
```

**Verification Layers:**

| Layer | Tool | What It Catches |
|-------|------|-----------------|
| **Compile-time** | Gradle dependency declarations | Missing/incorrect `implementation()` statements |
| **Test-time** | ArchUnit | Cross-module imports, internal package access, public internals |
| **Static Analysis** | SpotBugs/PMD/Checkstyle | Bug patterns, code smells, style violations |
| **Runtime** | Spring Boot + Testcontainers | Integration correctness, event flow |

---

## 5. Module Communication Summary Matrix

| Communication Need | Pattern | Example |
|-------------------|---------|---------|
| Notification needs post owner | Consumer-Owned Port | `NotificationPostPort` → `PostNotificationAdapter` |
| Comment needs post existence | Consumer-Owned Port | `CommentPostPort` → `PostCommentAdapter` |
| WebSocket needs user lookup | SPI (Core-owned Port) | `WebSocketUserPort` → `WebSocketUserAdapter` |
| Photo needs user profile | Shared SPI | `UserProfilePort` → `UserProfileAdapter` |
| Notification on user registration | Domain Event | `UserRegisteredEvent` (async) |
| Notification on new comment | Domain Event | `CommentCreatedEvent` (async) |
| Chat updates last message | Domain Event | `MessageSentEvent` (async) |
| Feed visibility projection | Domain Event | `UserFollowedEvent` → `PostVisibilityProjector` |

---

## 6. Extraction Readiness

The architecture is **pre-wired for microservice extraction**:

| Extraction Step | Current State | Effort |
|-----------------|---------------|--------|
| Database isolation | Separate schemas per domain | ✅ Zero changes |
| API contracts | Consumer-owned ports (interfaces) | ✅ Becomes service contract |
| Event contracts | Domain events in `core-shared` | ✅ Becomes message contract |
| Async communication | RabbitMQ + outbox | ✅ Infrastructure ready |
| No shared runtime | Library JARs, no bootJar | ✅ Independent deployable |
| Config isolation | Module-scoped properties | ✅ Independent config |

**To extract `domain-notification` as a microservice:**
1. Create new Spring Boot app with `domain-notification` as dependency
2. Implement `NotificationUserPort`, `NotificationPostPort`, `NotificationCommentPort` via REST/gRPC
3. Subscribe to same RabbitMQ events (or Kafka)
4. Deploy independently — **zero changes to other modules**

---

## Conclusion

The QuietSpace modular architecture achieves:

| Quality | Mechanism |
|---------|-----------|
| **Strong Encapsulation** | Package-private repos, ArchUnit bans, API-only exposure |
| **Decoupled Communication** | Consumer-owned ports (sync) + Domain events (async) |
| **Compile-Time Safety** | Gradle explicit dependencies + convention plugins |
| **Test-Time Verification** | ArchUnit rules per domain module |
| **Reusability** | Domain-agnostic core, portable across projects |
| **Extraction Readiness** | Library JARs, isolated schemas, interface contracts |
| **Quality Gates** | SpotBugs, PMD, Checkstyle on every build |

This represents a **production-grade Modular Monolith** that balances the operational simplicity of a monolith with the architectural discipline of microservices.

---

## Appendix: Module Dependency Graph

```
                           ┌─────────────────────┐
                           │   quietspace-app    │  (executable)
                           └──────────┬──────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────┐            ┌───────────────┐            ┌───────────────┐
│  core-shared  │            │  core-data    │            │  core-web     │
└───────┬───────┘            └───────┬───────┘            └───────┬───────┘
        │                            │                            │
        ▼                            ▼                            ▼
┌───────────────┐            ┌───────────────┐            ┌───────────────┐
│ core-security │            │core-messaging │            │  (other core) │
└───────┬───────┘            └───────┬───────┘            └───────┬───────┘
        │                            │                            │
        └────────────────────────────┼────────────────────────────┘
                                     │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
        ▼                            ▼                            ▼
┌───────────────┐            ┌───────────────┐            ┌───────────────┐
│ domain-user   │            │ domain-post   │            │ domain-comment│
│  (IAM, auth)  │            │  (content)    │            │  (threads)    │
└───────┬───────┘            └───────┬───────┘            └───────┬───────┘
        │                            │                            │
        │         ┌──────────────────┘                            │
        │         ▼                                               │
        │  ┌───────────────┐                                       │
        │  │ domain-       │                                       │
        │  │ notification  │  (consumes all events)               │
        │  │  (sink)       │                                       │
        │  └───────────────┘                                       │
        │                                                           │
        ▼                                                           ▼
┌───────────────┐            ┌───────────────┐            ┌───────────────┐
│ domain-reaction│           │ domain-chat   │            │ domain-message│
└───────────────┘            └───────┬───────┘            └───────────────┘
                                     │
                                     ▼
                            ┌───────────────┐
                            │ domain-photo  │  (leaf)
                            └───────────────┘

Events flow: All domains → core-shared events → RabbitMQ → Consumers
Ports flow:  Consumer defines → Provider implements (reverse dependency)
```

---

*Document Version: 1.0*  
*Last Updated: September 27, 2026*  
*Reviewed By: Architecture Review Team*
# Modular Architecture Overview

## Executive Summary

This document describes the modular architecture of the QuietSpace platform, a Spring Boot 4.x application built with Java 21 targeting Java 25. The architecture follows a **Core/Domain split** pattern with 15 Gradle modules organized into three layers: **Core** (5 modules), **Domain** (8 modules), and **App** (1 module). This document explains the architecture, dependency flows, Gradle conventions, and how this modular design enables scalability, testability, maintainability, and deployability.

---

## 1. Module Structure Overview

```
quietspace-platform/
├── core/                    # 5 infrastructure modules (library, zero domain deps)
│   ├── core-shared          # Base entities, utilities, events, shared ports
│   ├── core-data            # JPA auditing, base repositories
│   ├── core-web             # Global exception handling, OpenAPI config
│   ├── core-security        # JWT, filter chain, AuthenticationProvider
│   └── core-messaging       # RabbitMQ, WebSocket STOMP relay, email
├── domain/                  # 8 business capability modules (DDD bounded contexts)
│   ├── domain-user          # User aggregate, auth, profiles, follows
│   ├── domain-post          # Post, Poll, Save, Repost aggregates
│   ├── domain-photo         # Photo upload/storage (leaf module)
│   ├── domain-comment       # Comment thread aggregate
│   ├── domain-reaction      # Reaction aggregate
│   ├── domain-chat          # Chat aggregate (owns messages)
│   ├── domain-message       # Message aggregate (child of chat)
│   └── domain-notification  # Notification sink (event-driven)
└── app/
    └── quietspace-app       # Composition root, wiring, Flyway, integration tests
```

**Total: 15 modules** (5 core + 8 domain + 1 app)

---

## 2. Dependency Flow and Direction

### 2.1 Core Layer (Zero Domain Dependencies)

```
core-shared (root)
    ├── core-data      → depends on: core-shared
    ├── core-web       → depends on: core-shared
    ├── core-security  → depends on: core-shared, core-data
    └── core-messaging → depends on: core-shared, core-security
```

**Rule**: Core modules have **zero dependencies on domain modules**. They provide shared infrastructure, utilities, and contracts.

### 2.2 Domain Layer (Consumer-Owned Ports Pattern)

Domain modules depend on core modules and **selectively** on other domain modules via **query ports** (not direct repository access):

```
domain-user
    → core-shared, core-data, core-messaging
    → domain-photo (reads photo views)
    → domain-notification (implements notification ports)

domain-post
    → core-shared, core-data, core-web, core-security
    → domain-user (via UserQueryPort)
    → domain-photo, domain-reaction, domain-comment
    → domain-notification (implements ports)

domain-photo (leaf)
    → core-shared, core-data
    → NO domain dependencies (consumes UserProfilePort from core-shared)

domain-comment
    → core-shared, core-data
    → domain-user, domain-reaction
    → domain-notification (implements ports)
    → domain-post (test-only for fixtures)

domain-reaction
    → core-shared, core-data
    → domain-user
    → domain-notification (implements ports)

domain-chat
    → core-shared, core-data
    → domain-user
    → core-messaging, core-security

domain-message
    → core-shared, core-data, core-messaging
    → domain-user, domain-chat, domain-photo

domain-notification (sink)
    → core-shared, core-data, core-messaging
    → NO domain dependencies (implements ports for others)
```

### 2.3 Dependency Direction Rules

| Rule | Enforcement |
|------|-------------|
| Core → Domain | **FORBIDDEN** (ArchUnit) |
| Domain → Domain (internal packages) | **FORBIDDEN** (ArchUnit) |
| Domain → Domain (other module's repository) | **FORBIDDEN** (ArchUnit `has_no_cross_module_repository_dependency`) |
| Domain → Domain (query port) | **ALLOWED** (explicit allowlist) |
| Domain → Core | **ALLOWED** |
| App → All | **ALLOWED** (composition root) |

**Key Principle**: Cross-domain communication happens exclusively through **consumer-owned query ports** (`*QueryPort` interfaces in `domain.*.api` packages) and **domain events** (published via transactional outbox).

---

## 3. Gradle Build System and Conventions

### 3.1 Root Build Configuration (`build.gradle.kts`)

The root build applies conventions to **all subprojects**:

- **Java toolchain**: Compile with Java 21 (`release = 21`), target Java 25 runtime
- **Spring Boot 4.1.0** + Spring Dependency Management 1.1.7
- **Quality gates** (applied to every module):
  - **SpotBugs** 4.9.0 (fail on violation)
  - **PMD** 6.55.0
  - **Checkstyle** 10.17.0 (custom config)
  - **JaCoCo** code coverage
- **BootJar**: Only `quietspace-app` produces executable JAR (`bootJar.enabled = project.name == "quietspace-app"`)
- **Test framework**: JUnit Platform (JUnit 5)

### 3.2 Domain Conventions Plugin (`buildSrc/quietspace.domain-conventions.gradle.kts`)

A custom Gradle convention plugin (`quietspace.domain-conventions`) applied to **all domain modules**:

```kotlin
plugins {
    java
    `java-library`
}

dependencies {
    // Mechanical deps every domain module needs
    implementation("spring-boot-starter-web")
    implementation("spring-boot-starter-data-jpa")
    implementation("spring-boot-starter-validation")
    implementation("spring-boot-starter-security")
    api("spring-boot-starter-actuator")           // exported to consumers
    implementation("micrometer-core")
    implementation("micrometer-registry-prometheus")
    implementation("springdoc-openapi-starter-webmvc-ui")
    implementation("mapstruct")
    implementation("lombok")
    // ... annotation processors
    
    // Mandatory test gates for ALL phases
    testImplementation("spring-boot-starter-test")
    testImplementation("spring-boot-starter-webmvc-test")
    testImplementation("spring-boot-starter-data-jpa-test")
    testImplementation("spring-security-test")
    testImplementation("junit-jupiter-params")
    testImplementation("archunit-junit5:1.4.1")  // ArchUnit mandatory
    testRuntimeOnly("junit-platform-launcher")
    
    // Testcontainers baseline (MySQL)
    testImplementation("spring-boot-testcontainers")
    testImplementation("testcontainers:1.21.4")
    testImplementation("testcontainers:mysql:1.21.4")
    testImplementation("testcontainers:junit-jupiter:1.21.4")
    testRuntimeOnly("com.h2database:h2")         // H2 for @DataJpaTest slices
}
```

**Key Convention**: Domain modules only declare **WHAT** they depend on (`project(":domain:domain-user")`), not **HOW** to build. The convention plugin provides all mechanical dependencies.

### 3.3 Module-Level Build Files

Each domain module declares only its **semantic dependencies**:

```kotlin
// domain-post/build.gradle.kts
plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    implementation(project(":domain:domain-user"))
    implementation(project(":domain:domain-photo"))
    implementation(project(":domain:domain-reaction"))
    implementation(project(":domain:domain-comment"))
    implementation(project(":domain:domain-notification"))
    
    // Test-only: exception advice for web-slice tests
    testImplementation(project(":core:core-web"))
}
```

### 3.4 Enforced Quality Gates (Gradle `check` task)

```kotlin
tasks.named("check") {
    dependsOn("spotbugsMain", "pmdMain", "checkstyleMain")
}
```

Every module must pass:
- `spotbugsMain` / `spotbugsTest`
- `pmdMain` / `pmdTest`
- `checkstyleMain` / `checkstyleTest`
- `test` (unit + slice tests)
- `jacocoTestCoverageVerification` (≥80% coverage)

---

## 4. Architectural Patterns Enforced

### 4.1 Core/Domain Split

- **Core modules**: Infrastructure, reusable across applications, zero domain knowledge
- **Domain modules**: Business capabilities, own their data and behavior, communicate via ports/events

### 4.2 Consumer-Owned Ports Pattern

```java
// Consumer module defines the port it needs
// domain-comment/port/NotificationCommentPort.java
public interface NotificationCommentPort {
    UUID findCommentOwnerId(UUID commentId);
}

// Provider module implements the port
// domain-comment/adapter/CommentNotificationAdapter.java
@Component
class CommentNotificationAdapter implements NotificationCommentPort {
    private final CommentRepository repo;
    public UUID findCommentOwnerId(UUID commentId) {
        return repo.findById(commentId).map(Comment::getUserId).orElseThrow();
    }
}
```

**Rule**: Port interface lives in **consumer** module; implementation lives in **provider** module. Dependency direction: consumer → port (owns), provider → port (implements).

### 4.3 Domain Events + Transactional Outbox

- Events defined in `core-shared/event/` (`DomainEvent` base + specific events)
- `TransactionalEventPublisher` saves events to outbox table **within same transaction** as aggregate changes
- `OutboxEventPoller` (@Scheduled) polls outbox, publishes to RabbitMQ topic exchange
- Consumers use `ProcessedEventRepository` for idempotency (`eventId` deduplication)

### 4.4 Query Ports for Cross-Domain Reads

Each domain exposes an `api` package with query ports and record DTOs:

```java
// domain-user/api/UserQueryPort.java
public interface UserQueryPort {
    Optional<UserSummaryDTO> getUserSummary(UUID userId);
    Map<UUID, UserSummaryDTO> getUsersSummary(Set<UUID> userIds);
    Optional<UUID> findUserIdByUsernameOrEmail(String usernameOrEmail);
    Set<UUID> searchUserIds(String keyword);  // for containsText search
}

// domain-user/api/dto/UserSummaryDTO.java
public record UserSummaryDTO(
    UUID id, String username, String displayName, UUID photoId, StatusType statusType
) {}
```

**Consumers use query ports instead of direct repository access**.

### 4.5 Domain Events for Async Communication

Events published from domain services:
```java
// UserServiceImpl
userRepository.save(user);
eventPublisher.publish(new UserRegisteredEvent(user.getId(), user.getUsername(), user.getEmail()));
```

Consumers react via `@EventListener` with idempotency:
```java
@EventListener
@Transactional
public void onUserRegistered(UserRegisteredEvent event) {
    if (processedEventRepository.existsByEventId(event.getEventId())) return;
    // create welcome notification
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

### 4.6 Transitional Dual Mapping (Post.user → authorId)

For `Post.user` `@ManyToOne` association that blocks full decoupling:
- Retained `@ManyToOne User user` as **single writer** (feed-privacy SQL joins)
- Added read-only `@Column(insertable=false, updatable=false) UUID authorId` FK view
- All **new code** uses `authorId` + `UserQueryPort`; association retained only for `PostSpecifications.visibleToUser()` join

---

## 5. Benefits of This Modular Architecture

### 5.1 Modularity

| Aspect | How Achieved |
|--------|--------------|
| **Clear boundaries** | Each domain module = single bounded context (DDD) |
| **Explicit contracts** | Query ports (`*QueryPort`), command ports (`*CommandPort`), events (`*Event`) |
| **No cyclic dependencies** | ArchUnit forbids domain↔domain internal access; dependency graph is a DAG |
| **Explicit contracts** | Ports are Java interfaces; events are serializable classes |
| **Independent deployment readiness** | Each domain module is a `java-library` JAR; can be extracted to separate service |

### 5.2 Scalability

| Dimension | How Supported |
|-----------|---------------|
| **Horizontal scaling** | Domain modules can be extracted to separate services (JAR → Docker) |
| **Database isolation** | Each domain owns its tables; cross-domain reads via query ports (no shared tables) |
| **Event-driven async** | Outbox + RabbitMQ enables async processing; projector decouples feed reads |
| **Virtual threads** | Spring Boot 4 + Java 21 virtual threads handle millions of WebSocket connections |
| **Read model separation** | `PostAuthorVisibility` / `ViewerAuthorAccess` denormalized tables avoid cross-module joins |

### 5.3 Testability

| Test Layer | Scope | Speed | Examples |
|------------|-------|-------|----------|
| **Unit** | Single class, mocks only | ~ms | `*Test.java` (services, mappers, domain logic) |
| **Slice — Web** | `@WebMvcTest` + mocked services | ~100ms | `*ControllerSliceTest.java` |
| **Slice — Data** | `@DataJpaTest` + H2 | ~100ms | `*RepositoryTest.java` |
| **Slice — Contract** | Mockito unit tests | ~ms | `*QueryAdapterTest.java` |
| **ArchUnit** | Architecture rules | ~1s | `*ArchitectureRulesTest.java` |
| **Integration** | `@SpringBootTest` + Testcontainers | ~s | `*FlowIT.java` (app module only) |

| Test Quality Gate | Enforcement |
|-------------------|-------------|
| **≥80% coverage** | JaCoCo `jacocoTestCoverageVerification` per module |
| **ArchUnit rules** | Per-module `*ArchitectureRulesTest.java` + global rules |
| **Strict stubbing** | Mockito strict stubs (`UnnecessaryStubbingException`) |
| **Static analysis** | SpotBugs + PMD + Checkstyle on every build |

**Key**: Integration tests (`*FlowIT`, `*ServiceIT`, security tests) live **only in app module** — domain modules have zero integration tests, keeping them fast and isolated.

### 5.4 Maintainability

| Concern | How Addressed |
|---------|---------------|
| **Clear ownership** | Each domain module = single team ownership |
| **Explicit contracts** | Ports/events = explicit API; changes require consumer coordination |
| **ArchUnit guardrails** | Prevents accidental coupling; fails build on violation |
| **Explicit dependencies** | Gradle `implementation(project(":domain:xxx"))` = visible in build file |
| **Explicit contracts** | Ports are Java interfaces; IDE navigation works; refactoring safe |
| **No hidden coupling** | No shared databases; no cross-module repository access |

### 5.5 Deployment

| Aspect | How Supported |
|--------|---------------|
| **Monolith deployment** | Single `quietspace-app` Spring Boot JAR (all modules on classpath) |
| **Future microservice extraction** | Domain modules are `java-library` JARs; extract to separate service |
| **Database migration** | Flyway in `app` module; each domain owns its tables |
| **Configuration** | Profile-specific `application.yml` per environment |
| **Observability** | Per-domain `HealthIndicator`, Micrometer metrics, Zipkin tracing |

---

## 6. Dependency Graph (Visual)

```
                          ┌─────────────────┐
                          │  quietspace-app │  (composition root, wiring, ITs)
                          └────────┬────────┘
                                   │
          ┌────────────────────────┼────────────────────────┐
          ▼                        ▼                        ▼
    ┌─────────────┐         ┌─────────────┐         ┌─────────────┐
    │  core-shared │         │  core-data  │         │ core-security │
    └──────┬──────┘         └──────┬──────┘         └──────┬──────┘
           │                       │                       │
           ▼                       ▼                       ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                      DOMAIN LAYER                           │
    │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
    │  │ user    │ │ post    │ │ photo   │ │ comment │  ...     │
    │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘          │
    │       │           │           │           │                │
    │       ▼           ▼           ▼           ▼                │
    │  ┌─────────────────────────────────────────────────────┐   │
    │  │           CONSUMER-OWNED QUERY PORTS                 │   │
    │  │  UserQueryPort ◄── post, comment, reaction, chat,    │   │
    │  │                        message, notification          │   │
    │  │  PhotoQueryPort ◄── post, message, user              │   │
    │  │  PostQueryPort ◄── comment, reaction, notification  │   │
    │  │  CommentQueryPort ◄── post, reaction                │   │
    │  │  ReactionQueryPort ◄── comment, post                │   │
    │  └─────────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────────┘
```

---

## 7. Established Conventions Summary

| Convention | Location | Purpose |
|------------|----------|---------|
| **Domain Conventions Plugin** | `buildSrc/quietspace.domain-conventions.gradle.kts` | Mechanical deps, test gates, H2, ArchUnit |
| **Root Quality Gates** | `build.gradle.kts` (subprojects block) | SpotBugs, PMD, Checkstyle, JaCoCo, BootJar control |
| **Domain Module Structure** | `domain/*/src/main/java/dev/thural/quietspace/domain/xxx/` | Flat root + `api/`, `adapter/`, `controller/`, `dto/`, `health/`, `port/`, `service/`, `repository/` |
| **ArchUnit Rules** | `domain/*/archunit/DomainXxxArchitectureRulesTest.java` | No cross-domain internal access; no cross-module repo imports |
| **Query Port Pattern** | `domain/xxx/api/*QueryPort.java` + `dto/*SummaryDTO.java` | Cross-domain reads without repo coupling |
| **Consumer-Owned Ports** | `domain/xxx/port/*.java` + `adapter/*Adapter.java` | Explicit dependency direction |
| **Domain Events** | `core-shared/event/*Event.java` + `TransactionalEventPublisher` | Async, idempotent, outbox-backed |
| **Test Gates** | `domain-conventions` plugin + root `check` task | Unit ≥80%, ArchUnit, static analysis, JaCoCo |
| **BootJar Control** | Root `build.gradle.kts` | Only `quietspace-app` produces executable JAR |

---

## 8. Quick Reference: Module Responsibilities

| Module | Responsibility | Key Exports |
|--------|---------------|-------------|
| `core-shared` | Base entities, utils, events, SPI ports | `BaseEntity`, `DomainEvent`, `UserProfilePort`, `WebSocketUserPort` |
| `core-data` | JPA auditing, base repos | `BaseRepository` |
| `core-web` | Exception handling, OpenAPI | `GlobalExceptionHandler`, `OpenApiConfig` |
| `core-security` | JWT, filter chain, auth SPI | `JwtTokenService`, `AuthenticationProvider` |
| `core-messaging` | RabbitMQ, STOMP, email, projector | `WebSocketConfig`, `PostVisibilityProjector` |
| `domain-user` | User, Profile, Follow, Auth | `UserQueryPort`, `UserProfilePort` impl, `UserAuthenticationProvider` |
| `domain-post` | Post, Poll, Save, Repost | `PostQueryPort`, `CommentPostPort`, `NotificationPostPort` |
| `domain-photo` | Photo upload/storage | (leaf — no outbound ports) |
| `domain-comment` | Comment threads | `CommentCommandPort`, `CommentQueryPort`, `CommentPostPort` |
| `domain-reaction` | Reactions on content | `ReactionQueryPort` |
| `domain-chat` | Chat rooms, membership | `ChatMessagePort` |
| `domain-message` | Messages in chats | (consumes `ChatMessagePort`) |
| `domain-notification` | Notifications (sink) | `NotificationUserPort`, `NotificationPostPort`, `NotificationCommentPort` |

---

## 9. Conclusion

This modular architecture achieves:

- **Modularity**: 15 independent modules with explicit contracts, no cyclic dependencies
- **Scalability**: Event-driven async, denormalized read models, virtual threads, extraction-ready modules
- **Testability**: 5 test layers with quality gates; domain modules test fast (no integration tests)
- **Maintainability**: Explicit contracts, ArchUnit guardrails, visible dependencies, clear ownership
- **Deployment**: Single JAR today; microservice extraction path exists

The architecture is **enforced by tooling** (Gradle conventions, ArchUnit, static analysis) not just documentation — violations fail the build.
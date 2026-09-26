# Modular Architecture Alignment Plan

## Executive Summary

This plan addresses the gaps between the current quietspace-platform modular structure and the industry-standard modular architecture recommendations. The project has a solid 15-module foundation (5 core + 8 domain + 1 app) but needs alignment in **test organization**, **module contracts**, **ArchUnit enforcement**, and **package-private visibility**.

**Key Decisions Applied from Architecture Review:**

| Topic | Decision | Rationale |
|-------|----------|-----------|
| **Port Migration** | Keep `WebSocketUserPort` in `core-shared` (Outbound SPI); move `UserProfilePort` → `domain-user.api` (Inbound Query Port) | Core-driven SPI stays in core; domain-driven query ports move to domain |
| **Testcontainers** | Share base config via `app` module / test-fixtures library | Avoids 50MB/module overhead; prevents OOM in parallel execution |
| **SpringWolf/OpenAPI** | SpringWolf → `core-messaging`; OpenAPI → `core-web` | Infrastructure stays in owning core module |
| **Timeline Priority** | **Prioritize Phases 1-2** (Tests + ArchUnit) first | ArchUnit guardrails prevent regressions in later phases |
| **Breaking Changes** | No migration window needed if JSON/HTTP contracts intact | Package moves and port relocations are internal; only network contracts matter |

---

## Current State Assessment

| Aspect | Status | Gaps |
|--------|--------|------|
| **Module Structure** | ✅ 15 modules (5 core, 8 domain, 1 app) | — |
| **Test Co-location** | ✅ Mostly (domain/core modules have `src/test`) | ❌ App module contains 30 domain-specific tests |
| **Module Boundaries** | ✅ Reasonable DDD contexts | ⚠️ Some cross-domain deps need review |
| **Consumer-Owned Ports** | ✅ Correctly implemented | — |
| **ArchUnit Rules** | ⚠️ Single test in core-shared | ❌ No per-module ArchUnit suites |
| **Package-Private Repos** | ⚠️ Some public | ❌ Not enforced |
| **Public API Contracts** | ❌ No `api`/`ports` packages in domains | ❌ Need explicit contracts |

---

## Phase 1: Test Organization Cleanup (Week 1)

### 1.1 Move Domain-Specific Tests from App Module to Domain Modules

**Current Issue**: 30 tests in `app/quietspace-app/src/test` that belong in domain modules:
- **Repository tests** (9): `UserRepositoryTest`, `PostRepositoryTest`, `PhotoRepositoryTest`, `CommentRepositoryTest`, `ReactionRepositoryTest`, `ChatRepositoryTest`, `MessageRepositoryTest`, `NotificationRepositoryTest`, `TokenRepositoryTest`
- **Security tests** (4): `CorsSecurityTest`, `CsrfTest`, `SecurityHeadersTest`, `TokenRepositoryTest`
- **FlowITs** (14): All domain-specific FlowITs (`UserFlowIT`, `PostFlowIT`, `PhotoFlowIT`, `CommentFlowIT`, `ReactionFlowIT`, `ChatFlowIT`, `MessageFlowIT`, `NotificationFlowIT`, `AuthFlowIT`, `AdminFlowIT`, `UserServiceIT`, `SecurityFlowIT`, `WebSocketFlowIT`, `FlywayMigrationIT`)

**Action Plan**:
| Test File | Target Module | Test Type |
|-----------|--------------|-----------|
| `*RepositoryTest.java` (9 files) | Respective domain modules | `@DataJpaTest` slice tests |
| `*ControllerSecurityTest.java` (1) | `domain-post` | `@WebMvcTest` slice test |
| `CorsSecurityTest`, `CsrfTest`, `SecurityHeadersTest` | `app/quietspace-app` | Keep (cross-cutting) |
| `TokenRepositoryTest` | `domain-user` | `@DataJpaTest` slice test |
| `*FlowIT.java` (14 files) | Respective domain modules | `@SpringBootTest` integration tests |
| `FlywayMigrationIT` | `app/quietspace-app` | Keep (schema validation) |
| `SecurityFlowIT` | `app/quietspace-app` | Keep (cross-cutting) |

**Gradle Config**: Each domain module declares Testcontainers dependencies. **Shared base configuration** (MySQL, RabbitMQ containers) provided via `app` module test-fixtures or dedicated `test-support` module to avoid 50MB/module overhead.

### 1.2 Verify Test Dependencies in Domain Modules

Each domain module's `build.gradle.kts` should include:
```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
// Testcontainers base config provided by app module test-fixtures
// testImplementation("org.testcontainers:mysql:1.21.4")
// testImplementation("org.testcontainers:junit-jupiter:1.21.4")
```

---

## Phase 2: Per-Module ArchUnit Test Suites (Week 1-2)

**Priority**: Execute **Phases 1-2 first** to establish ArchUnit guardrails before later refactoring phases.

### 2.1 Create ArchUnit Test in Each Module

**Current**: Single `ArchitectureRulesTest` in `core-shared` scanning entire project.

**Target**: Each module owns its ArchUnit tests enforcing:
- Module-specific dependency rules
- Internal package structure
- Public API surface

### 2.2 Required ArchUnit Tests per Module

| Module | Rules to Enforce |
|--------|------------------|
| **core-shared** | No domain deps; no core-web/security/messaging deps; public ports only |
| **core-data** | No domain deps; only core-shared; JPA entities package-private |
| **core-web** | No domain deps; only core-shared; global exception handler |
| **core-security** | No domain deps; only core-shared, core-data; JWT/AuthenticationProvider |
| **core-messaging** | No domain deps; only core-shared/core-security; ports for WebSocket |
| **domain-user** | No other domain internal deps; public `api`/`ports` packages only |
| **domain-post** | No other domain internal deps; depends on domain-user; public `api` |
| **domain-photo** | No domain deps (leaf); public `api` |
| **domain-comment** | Depends on domain-user, domain-post; public `api` |
| **domain-reaction** | Depends on domain-user, domain-post, domain-comment; public `api` |
| **domain-chat** | Depends on domain-user; public `api` |
| **domain-message** | Depends on domain-user, domain-chat; public `api` |
| **domain-notification** | Sink module; no domain deps; owns ports for other domains |

### 2.3 Shared ArchUnit Rules (in core-shared test utilities)

Create `ArchUnitRules` utility class in `core-shared/src/test/java/dev/thural/quietspace/core/shared/archunit/`:

```java
public final class ArchUnitRules {
    public static ArchRule noDomainDependencies() { ... }
    public static ArchRule repositoriesPackagePrivate() { ... }
    public static ArchRule noControllerAccessesRepository() { ... }
    public static ArchRule entitiesPackagePrivate() { ... }
}
```

Each module's test imports and extends these with module-specific rules.

---

## Phase 3: Module Contract Standardization (Week 2-3)

### 3.1 Define Public API Packages in Each Domain Module

Per recommendations, each domain module exposes contracts under `dev.thural.quietspace.<domain>.api`:

```
domain-user/
  src/main/java/
    dev/thural/quietspace/user/
      api/
        UserQueryPort.java          // Inbound port for other domains
        dto/
          UserSummaryDTO.java       // Immutable record
          UserProfileDTO.java
      ports/                        // Outbound ports (if any)
      internal/                     // Package-private implementation
        adapter/
        service/
        repository/
```

### 3.2 Standardize Contract Types

| Contract Type | Location | Naming | Format |
|---------------|----------|--------|--------|
| **Inbound Port** | `<domain>.api` | `<Capability>Port` | Interface |
| **Outbound Port** | `<domain>.ports` | `<Capability>Port` | Interface |
| **DTO** | `<domain>.api.dto` | `<Entity>SummaryDTO` | `record` |
| **Event** | `core.shared.event` | `<Action>Event` | Class extending `DomainEvent` |

### 3.3 Migration: Move Core Ports to Domain Modules Where Appropriate

**Key Principle**: Core-driven SPI (Outbound Ports) stay in `core-shared`; Domain-driven Query Ports (Inbound) move to `domain.api`.

| Port | Current Location | Target Location | Rationale |
|------|------------------|-----------------|-----------|
| `WebSocketUserPort` | `core-shared.ports` | **STAY in `core-shared.ports`** | Core-driven Outbound SPI; WebSocket auth is infrastructure |
| `UserProfilePort` | `core-shared.ports` | **MOVE to `domain-user.api`** | Domain-driven Inbound Query Port; owned by user domain |
| `NotificationPostPort` | `domain-notification.port` | `domain-notification.ports` | ✅ Already correct (consumer-owned) |
| `NotificationCommentPort` | `domain-notification.port` | `domain-notification.ports` | ✅ Already correct |
| `NotificationUserPort` | `domain-notification.port` | `domain-notification.ports` | ✅ Already correct |

**Note**: Core-driven ports (WebSocket auth) stay in core as they are infrastructure SPI. Domain-driven query ports move to `domain.api` packages.

### 3.4 Create Immutable DTO Records

Example for `domain-user`:
```java
// domain-user/src/main/java/dev/thural/quietspace/user/api/dto/UserSummaryDTO.java
public record UserSummaryDTO(
    UUID id,
    String username,
    String displayName,
    String avatarUrl,
    StatusType status
) {}

// domain-user/src/main/java/dev/thural/quietspace/user/api/UserQueryPort.java
public interface UserQueryPort {
    UserSummaryDTO getUserSummary(UUID userId);
    Map<UUID, UserSummaryDTO> getUsersSummary(Set<UUID> userIds);
}
```

---

### 3.5: Infrastructure Documentation Location (Per Architecture Review)

**SpringWolf/OpenAPI Location**:
- **SpringWolf** (WebSocket API docs) → Move to `core-messaging` (owns WebSocket infrastructure)
- **OpenAPI/Swagger** (REST API docs) → Move to `core-web` (owns REST infrastructure)
- `app/quietspace-app` remains lean: main class + global config + cross-cutting ITs only

---

## Phase 4: Package-Private Enforcement (Week 3)

### 4.1 Make All Repository Interfaces Package-Private

Current pattern (public):
```java
public interface UserRepository extends JpaRepository<User, UUID> { }
```

Target pattern (package-private):
```java
interface UserRepository extends JpaRepository<User, UUID> { }
```

**Modules affected**: All 8 domain modules (User, Post, Photo, Comment, Reaction, Chat, Message, Notification repositories)

### 4.2 Make Internal Classes Package-Private

| Type | Current | Target |
|------|---------|--------|
| Service Impl (`*ServiceImpl`) | public | package-private |
| Mappers (`*Mapper`) | public | package-private |
| Adapters (`*Adapter`) | public | package-private |
| Entities (`@Entity`) | public | package-private (or keep public for JPA) |

### 4.3 Update Tests to Access Package-Private

Since tests are co-located in same module/package, they retain access. No test changes needed.

---

## Phase 5: Dependency Review & Cleanup (Week 3-4)

### 5.1 Review Cross-Domain Dependencies

Current dependencies needing review:

| Consumer | Provider | Type | Action |
|----------|----------|------|--------|
| domain-user → domain-photo | `implementation` | Query photo views | Keep (one-way) |
| domain-user → domain-notification | `implementation` | Implements notification ports | Keep (consumer-owned ports) |
| domain-post → domain-user | `implementation` | Author info | Replace with `UserQueryPort` |
| domain-post → domain-photo | `implementation` | Media attachment | Replace with `PhotoQueryPort` |
| domain-post → domain-reaction | `implementation` | Reaction counts | Replace with `ReactionQueryPort` |
| domain-post → domain-comment | `implementation` | Comment counts | Replace with `CommentQueryPort` |
| domain-post → domain-notification | `implementation` | Implements notification ports | Keep |
| domain-comment → domain-user | `implementation` | Author info | Replace with `UserQueryPort` |
| domain-comment → domain-reaction | `implementation` | Reaction counts | Replace with `ReactionQueryPort` |
| domain-comment → domain-notification | `implementation` | Implements notification ports | Keep |
| domain-reaction → domain-user | `implementation` | Actor info | Replace with `UserQueryPort` |
| domain-reaction → domain-notification | `implementation` | Implements notification ports | Keep |
| domain-chat → domain-user | `implementation` | Member info | Replace with `UserQueryPort` |
| domain-message → domain-user | `implementation` | Sender info | Replace with `UserQueryPort` |
| domain-message → domain-chat | `implementation` | Chat context | Keep (chat owns message) |
| domain-message → domain-photo | `implementation` | Media in messages | Replace with `PhotoQueryPort` |

### 5.2 Define Missing Query Ports

Each domain module needs `*QueryPort` interfaces for data needed by other domains:

| Domain | Query Ports Needed |
|--------|-------------------|
| domain-user | `UserQueryPort` (summary, profile, search) |
| domain-post | `PostQueryPort` (feed, search, counts) |
| domain-photo | `PhotoQueryPort` (URLs, metadata) |
| domain-comment | `CommentQueryPort` (thread, counts) |
| domain-reaction | `ReactionQueryPort` (counts, user reactions) |

---

## Phase 6: App Module Cleanup (Week 4)

### 6.1 App Module Responsibilities

After migration, `app/quietspace-app` should only contain:
- `QuietspaceApplication` (main class)
- Global configuration (`application.yml`)
- Cross-cutting integration tests:
  - `FlywayMigrationIT`
  - `SecurityFlowIT`
  - `CorsSecurityTest`
  - `CsrfTest`
  - `SecurityHeadersTest`
  - `WebSocketFlowIT` (if truly cross-domain)

**Moved out of app module**:
- SpringWolf WebSocket docs → `core-messaging`
- OpenAPI/Swagger config → `core-web`
- Testcontainers base config → shared test-fixtures (referenced by all domain modules)

### 6.2 Remove Duplicate Dependencies

App module currently declares many Spring Boot starters that domain modules already have. Keep only:
- `spring-boot-starter-web`
- `spring-boot-starter-actuator`
- `spring-boot-starter-flyway`
- `spring-boot-starter-data-jpa` (for Flyway)
- Observability (Micrometer, Zipkin)
- Testcontainers for cross-cutting ITs only

---

## Phase 6: CI/CD & Documentation (Week 4)

### 6.1 Gradle Build Optimization

- Enable configuration cache: `org.gradle.configuration-cache=true`
- Use `--parallel` for test execution
- Configure test task timeout and retry for flaky ITs

### 6.2 Documentation Updates

| Document | Location | Content |
|----------|----------|---------|
| Module Contracts | `docs/architecture/domains/` | Per-domain API contracts |
| Event Catalog | `docs/architecture/events.md` | All domain events with payloads |
| Migration Guide | `docs/guides/migration/` | How to add new domain module |
| Testing Guide | `docs/guides/testing.md` | Unit/Slice/IT patterns per module |

---

## Implementation Priority Matrix

| Phase | Task | Effort | Risk | Dependencies | Priority |
|-------|------|--------|------|--------------|----------|
| 1.1 | Move app tests to domain modules | Medium | Low | None | **P0** |
| 1.2 | Verify test dependencies (shared Testcontainers) | Low | Low | 1.1 | **P0** |
| 2.1 | Create per-module ArchUnit tests | Medium | Low | None | **P0** |
| 2.2 | Shared ArchUnit utilities | Low | Low | 2.1 | **P0** |
| 3.1 | Create `api` packages | Medium | Medium | None | P1 |
| 3.2 | Standardize contracts | Medium | Medium | 3.1 | P1 |
| 3.3 | Migrate core ports (UserProfilePort → domain-user.api) | Medium | Medium | 3.1 | P1 |
| 3.4 | Create immutable DTOs | Medium | Low | 3.2 | P1 |
| 3.5 | Move SpringWolf → core-messaging, OpenAPI → core-web | Low | Low | None | P1 |
| 4.1 | Package-private repos | Low | Low | None | P1 |
| 4.2 | Package-private internals | Low | Medium | 4.1 | P1 |
| 5.1 | Review dependencies | High | High | 3.3 | P2 |
| 5.2 | Define query ports | High | Medium | 5.1 | P2 |
| 6.1 | App module cleanup | Low | Low | 1.1 | P2 |
| 6.2 | Documentation | Low | Low | All | P2 |

**Priority Legend**: P0 = Execute first (guardrails), P1 = Execute next (contracts), P2 = Execute last (cleanup)

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking tests during migration | Run full test suite after each phase; use feature branches |
| Circular dependencies | ArchUnit tests will catch; resolve incrementally |
| Package-private breaking tests | Tests in same package retain access; verify before commit |
| Port migration breaking consumers | Keep old ports temporarily with `@Deprecated`; dual-support during transition |
| Immutable record DTOs breaking JSON serialization | Verify Jackson output matches exact same field names/order before deploying | Architecture review: ensure network contract compatibility |

---

## Success Criteria

- [x] All 15 modules have `src/test` with unit and slice tests (integration FlowITs live in `app` by definition — see Phase 6.1 amendment)
- [x] App module hosts only cross-cutting + FlowIT tests; **zero unit/slice tests** (amended from "< 10 tests": FlowITs/security tests require the composition-root context and cannot move to domain modules without circular Gradle deps)
- [x] Each module has `ArchitectureRulesTest` with module-specific rules
- [ ] All repositories package-private — **superseded**: enforce "no cross-module repository imports" via rewritten ArchUnit rules instead (Java package-private is package-exact and would break same-module `adapter/health/api/security` subpackages; see refactoring plan Phase B)
- [x] Query-port `api` packages with immutable DTO records in user/photo/post/comment/reaction (chat owns message context, notification is a sink, message needs no query port — no `api` required there)
- [ ] All cross-domain communication via ports — with documented exceptions: `UserProfilePort`/`WebSocketUserPort` stay in `core-shared` (outbound SPI; move would create a user↔photo cycle), message→chat (aggregate ownership), entity-graph navigation pending JPA decoupling (Phase E)
- [x] Full test suite passes: `./gradlew test`
- [x] All ArchUnit rules pass: `./gradlew test --tests "*ArchitectureRulesTest*"`
- [x] Static analysis clean: `./gradlew spotbugsMain pmdMain checkstyleMain`

---

## Questions for Clarification (RESOLVED)

The following decisions have been made based on architecture review:

| Question | Decision | Rationale |
|----------|----------|-----------|
| **1. Port Migration Strategy** | Keep `WebSocketUserPort` in `core-shared` (Outbound SPI); move `UserProfilePort` → `domain-user.api` (Inbound Query Port) | Core-driven SPI stays in core; domain-driven query ports move to domain |
| **2. Testcontainers** | Share base config via `app` module test-fixtures library | Avoids 50MB/module overhead; prevents OOM in parallel execution |
| **3. SpringWolf/OpenAPI** | SpringWolf → `core-messaging`; OpenAPI → `core-web` | Infrastructure stays in owning core module |
| **4. Timeline Priority** | **Prioritize Phases 1-2** (Tests + ArchUnit) first | ArchUnit guardrails prevent regressions in later phases |
| **5. Breaking Changes** | No migration window needed if JSON/HTTP contracts intact | Package moves and port relocations are internal; only network contracts matter |

---

## Appendix: Module Dependency Graph (Target)

```
core-shared ◄── core-data
core-shared ◄── core-web
core-shared ◄── core-security
core-shared ◄── core-messaging

core-shared ◄── domain-user
core-shared ◄── domain-post
core-shared ◄── domain-photo
core-shared ◄── domain-comment
core-shared ◄── domain-reaction
core-shared ◄── domain-chat
core-shared ◄── domain-message
core-shared ◄── domain-notification

core-data ◄── domain-user
core-data ◄── domain-post
core-data ◄── domain-photo
core-data ◄── domain-comment
core-data ◄── domain-reaction
core-data ◄── domain-chat
core-data ◄── domain-message
core-data ◄── domain-notification

core-security ◄── domain-user
core-security ◄── domain-post
core-security ◄── domain-chat
core-security ◄── domain-message

core-messaging ◄── domain-user
core-messaging ◄── domain-chat
core-messaging ◄── domain-message
core-messaging ◄── domain-notification

domain-user ◄── domain-post (via UserQueryPort)
domain-user ◄── domain-comment (via UserQueryPort)
domain-user ◄── domain-reaction (via UserQueryPort)
domain-user ◄── domain-chat (via UserQueryPort)
domain-user ◄── domain-message (via UserQueryPort)

domain-post ◄── domain-comment (via PostQueryPort)
domain-post ◄── domain-reaction (via PostQueryPort)
domain-post ◄── domain-notification (implements NotificationPostPort)

domain-comment ◄── domain-reaction (via CommentQueryPort)
domain-comment ◄── domain-notification (implements NotificationCommentPort)

domain-reaction ◄── domain-notification (implements NotificationReactionPort)

domain-chat ◄── domain-message
domain-message ◄── domain-notification (implements NotificationMessagePort)
```
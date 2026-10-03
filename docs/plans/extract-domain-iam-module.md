# Plan: Consolidate IAM Bounded Context within domain-user

## Overview
Keep `domain-user` as the single unified Identity Bounded Context (auth + profile + settings). No new `domain-iam` Gradle subproject. Partition responsibilities via internal packages, keep core domain-agnostic via SPIs in `core-security`, and decouple side effects via enriched domain events.

This supersedes the prior extraction strategy (obsolete). Single source of truth is this file.

### Locked decisions
1. **Module naming: keep `domain-user`.** No subproject rename (avoids churn in `settings.gradle.kts`, ArchUnit slices, downstream imports). Internal packages carry the boundary:
   - `dev.thural.quietspace.domain.user.auth` (authentication, login, token management, future `auth/strategy/` for OAuth2/WebAuthn/social)
   - `dev.thural.quietspace.domain.user.profile` (profiles, avatars, display names)
   - `dev.thural.quietspace.domain.user.settings` (preferences, privacy controls)
2. **`JwtTokenServiceImpl` stays in `core-security`.** Stateless JWT engine (parse/verify/claims) + SPIs (`core-security.port.JwtTokenService`, `core-security.port.CurrentUserPort`, `core-security.port.TokenBlacklistPort`) live in core. Stateful `Token` entity + `TokenRepository` + revocation/blacklist live in `domain-user` (`domain.user.token`). `CurrentUserPort` is **implemented in `domain-user.adapter`** (`CurrentUserAdapter`: SecurityContext username → `UserRepository` → id) because the runtime principal is `UserDetails`, never a UUID — a core-side implementation cannot resolve an id without user data. Same applies to `TokenBlacklistPort` (implemented by `TokenBlacklistAdapter` in `domain-user.token`).
3. **Activation mail via enriched event.** `UserRegisteredEvent` gains `activationCode`; `domain-notification` consumes it and dispatches mail via `EmailEventPublisher → EmailEvent`. No direct mail coupling from identity logic.

---

## Execution order principle

```
[Phase A: Core Ports & Contracts] --> [Phase B: Package Moves & Config] --> [Phase C: Service & Adapter Wiring] --> [Phase D: Consumer & ArchUnit Rules]
```

Core interfaces and event contracts land **before** any consumer code references them, so every step compiles and no file is re-opened for import churn in a later PR.

---

## PR 1: Core preparation & internal package isolation

**Goal:** Build-safe internal structure. All referenced ports/contracts exist before use.

| Step | Action | Files |
|------|--------|-------|
| 1.1 | Core SPIs & event contract first (moved forward from old PR 2) | `JwtTokenService` interface + `AuthenticationProvider` SPI → `core-security.port`; new `CurrentUserPort` + `TokenBlacklistPort` → `core-security.port` (`UnauthenticatedException` → 401 when anonymous — see §2.6); add `activationCode` to `core-shared/event/UserRegisteredEvent.java` (+ ctor/getters for Jackson/outbox roundtrip) + update `EventSerializerTest` |
| 1.2 | Package creation & class relocations (imports now resolve to final paths) | Create `domain.user.auth` (incl. `auth/strategy/` placeholder, `auth/controller/`), `domain.user.profile`, `domain.user.settings`, `domain.user.token`; relocate IAM classes into `auth/`; move `Token.java`, `TokenRepository.java` from `core-shared.security` → `domain-user.token` |
| 1.3 | Configuration consolidation | `security/UserSecurityBeans.java` → merge into internal `domain-user/UserIamConfig.java`, imported by `quietspace-app` (no legacy package scan) |
| 1.4 | Service wiring (no `UserCredentialsPort` — same-module collaboration) | `AuthService.register()` generates code, persists `Token`, publishes enriched `UserRegisteredEvent` via `TransactionalEventPublisher` in same TX (drop direct `EmailEventPublisher` call); new `TokenService.java` in `domain-user.token` (blacklist/refresh/revoke). Preserve `token`-column dual use: refresh records store the JTI, blacklist records store the full JWT — otherwise `existsByToken` in `refreshToken()` false-positives. Refresh expiry via `@Value` (never `System.getProperty`). |
| 1.5 | Adapter updates in single pass | `UserProfileAdapter`, `WebSocketUserAdapter`, `UserNotificationAdapter` → `core-security.port.CurrentUserPort` instead of `UserService.getSignedUser()`; new `CurrentUserAdapter` in `domain-user.adapter` implements the port (username → `UserRepository` → id); `JwtFilter` uses `TokenBlacklistPort` (implemented by `TokenBlacklistAdapter` in `domain-user.token`) instead of `TokenRepository` — preserves zero core→domain edge |
| 1.6 | Verification (compiles: ports + event contract already in place) | `./gradlew :domain:domain-user:test` |

Future auth methods (OCP): add `domain.user.auth.strategy.OAuth2AuthenticationProvider` / `PasskeyWebAuthnProvider` / `GoogleSocialAuthProvider` implementing the core `AuthenticationProvider` SPI. Zero edits to existing password flow or core.

---

## PR 2: Downstream integration, ArchUnit, docs & test verification

### 2.1 Notification consumer (enriched payload already exists from PR 1)
- `domain-notification.NotificationEventListener.onUserRegistered` builds activation mail from event (`userId`, `email`, `username`, `activationCode`) via `EmailEventPublisher → EmailEvent → email.queue`, idempotent via `processed_events.existsByEventId` (existing pattern). Preserves current welcome-notification behavior or replaces per product decision — state explicitly in PR.

### 2.2 Build & app config
- No `domain-iam/build.gradle.kts`, no `include("domain:domain-iam")`.
- `domain-user/build.gradle.kts`: unchanged semantic deps (`core-shared`, `core-data`, `core-messaging`, `domain-photo`, `domain-notification` + test-only `core-web`). No new domain dep.
- `core-security/build.gradle.kts`: no domain deps (unchanged).
- `quietspace-app`: unchanged module list; import consolidated `UserIamConfig` (not legacy package scan).

### 2.3 ArchUnit enforcement (code + interfaces already in place, rules validate a complete graph)
- Delete planned `DomainIamArchitectureRulesTest` for separate module.
- Extend `DomainUserArchitectureRulesTest`:
  - `auth` may access `token` + own repo; `controller` never injects repositories (existing global rule).
  - Forbid external modules importing `domain.user` internals / `UserRepository` (existing per-module + global `has_no_cross_module_repository_dependency` — verify).
  - Example guard:
```java
noClasses().that().resideInAPackage("..domain.user.controller..")
    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
```
- Global `ArchitectureRulesTest`: unchanged (no new slice; `no_core_depends_on_domain` still holds).

### 2.4 Full test & quality suite
- Keep auth tests in `domain-user` (no move). Add/extend: `AuthService` (enriched event published, token persisted), `TokenService` (blacklist/refresh/revoke), `UserIamConfig` wiring smoke if needed.
- Contract/slice rules per `04-testing-rules`: web slice (`@WebMvcTest`, `addFilters=false`, mock `JwtFilter` deps), repo slice for `TokenRepository` (`@DataJpaTest` + `Replace.NONE` + H2 `MODE=MYSQL;NON_KEYWORDS=user`), no `@SpringBootTest` in domain, IT (`login→refresh→logout`, activation-mail flow) in `app` as `*IT.java`.
- Coverage ≥80% for `domain-user` (`jacocoTestCoverageVerification`).
- Run `./gradlew check` + app integration tests.

### 2.5 Documentation
- Flyway stays centralized in `quietspace-app/.../db/migration` (`V{next}__*.sql`, idempotent). No module-level migrations.
- `Token.java` has no explicit `@Table` (defaults to `token`). Package move `core.shared.security` → `domain.user.token` does not change table name — no schema migration required. Do not add `@Table` unless renaming; verify H2 + MySQL `validate` both pass.
- Update `domain-module-contracts.md` (Token ownership, no new query port) + `events.md` (new payload) + `core-module-apis.md` (new port locations).
- Health/metrics: `domain-user` `*HealthIndicator` covers token counts if exposed; add `domain.user.*.total/duration` only for new ops; `AuthController` stays under `/api/v1` + OpenAPI (no new AsyncAPI unless new WS push).

### 2.6 Runtime safeguards (pre-`check` verification)
- **JPA/entity scanning:** `QuietspaceApplication` (`dev.thural.quietspace` package) relies on default Spring Boot scanning (`dev.thural.quietspace.*`), so moved `domain.user.token.Token` + `TokenRepository` are auto-registered with no explicit `@EntityScan`/`@EnableJpaRepositories`. Verify: no explicit scan annotations exist on the bootstrap class; if a future explicit scan is added, include `dev.thural.quietspace.domain.user.token` + `dev.thural.quietspace.core.shared`. `UserIamConfig` must be under `dev.thural.quietspace.domain.user` (auto-scanned) or explicitly `@Import`ed by app — never rely on legacy package scan.
- **`CurrentUserPort` unauthenticated contract:** `CurrentUserAdapter.currentUserId()` (`domain-user.adapter`) must define anonymous-path behavior explicitly since `SecurityContextHolder` is empty on public endpoints (e.g. `/api/v1/auth/login`, `/register`). Contract: throw standardized `UnauthenticatedException` (mapped to 401 via `core-web` handler), never return null; unknown usernames throw `UserNotFoundException`. Document in `core-module-apis.md`; cover all paths in tests (authenticated → UUID, anonymous → 401, unknown → 404). Rationale for domain-side impl: the runtime principal is `UserDetails`, never a UUID, so resolution requires `UserRepository`.
- **Flyway/table alignment:** covered in §2.5 — no table rename, no migration needed for the package move. Add a startup check: app IT boots against Testcontainers MySQL with `ddl-auto: validate` to catch any accidental naming drift.

---

## File movement summary (revised — no cross-module auth move)

| Source | Destination |
|--------|-------------|
| `domain-user/auth/*` (scattered) | `domain-user/auth/*` consolidated + `auth/strategy/` placeholder + `auth/controller/` |
| `domain-user/security/UserSecurityBeans.java` | → Merged into `domain-user/UserIamConfig.java` (imported by app) |
| `core-shared/security/JwtTokenService.java` (interface) | `core-security/port/JwtTokenService.java` |
| `core-shared/security/JwtTokenServiceImpl.java` | `core-security` (stateless impl, stays) |
| `core-shared/security/AuthenticationProvider.java` | `core-security.port.AuthenticationProvider` |
| `core-shared/security/Token.java` | `domain-user/token/Token.java` |
| `core-shared/security/TokenRepository.java` | `domain-user/token/TokenRepository.java` |
| (new) `core-security/port/CurrentUserPort.java` + `TokenBlacklistPort.java` (interfaces) | `core-security` |
| (new) `domain-user/adapter/CurrentUserAdapter.java` + `domain-user/token/TokenBlacklistAdapter.java` (implementations) | `domain-user` |
| (enriched) `core-shared/event/UserRegisteredEvent.java` | add `activationCode` |

Explicitly **not created**: `domain-iam/` subproject, `UserCredentialsPort`, `UserCredentialsAdapter`, `DomainIamArchitectureRulesTest`, `settings.gradle.kts` include.

---

## Verification checklist
- [ ] PR 1: `./gradlew :domain:domain-user:test` green; no new module deps
- [ ] PR 2: `./gradlew check` green (SpotBugs/PMD/Checkstyle/ArchUnit/JaCoCo ≥80%)
- [ ] PR 2: app ITs green incl. auth flow + activation-mail via notification
- [ ] ArchUnit: no core→domain, no cross-module repo/internal imports, controller→service→repo only
- [ ] `EventSerializerTest` roundtrip for enriched event; consumer idempotent
- [ ] Runtime safeguards (§2.6): `TokenRepository` bean registered (default scan, no explicit scan needed); `CurrentUserPort` anonymous → 401 covered; MySQL `validate` passes with no Flyway diff
- [ ] No committed secrets, Flyway centralized, OpenAPI prefix intact

---

## Why this complies (rules traceability)
- `01-dependency-rules §4` + `06 Part B` + ADR 004: IAM stays in `domain-user` — compliant as-written, no amendment.
- `02-module-structure`: same-module repo use, flat-root + `controller/dto/health` conventions retained; no cross-aggregate JPA additions.
- `03-communication` + `04-messaging-infra`: sync via existing `UserQueryPort`/adapters + `CurrentUserPort` SPI; async via outbox `UserRegisteredEvent`; mail only via `EmailEvent`.
- `system 01/03`: core defines `AuthenticationProvider`/`JwtTokenService`/`CurrentUserPort`, domain implements, app wires — zero core→domain compile edge; no JWT logic in domain (impl stays in `core-security`).

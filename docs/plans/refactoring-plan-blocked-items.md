# Refactoring Plan — Blocked-Item Resolution (IAM, ArchUnit, Ports, JPA Decoupling)

> **Source.** Recommendations from the Java Backend Development notebook review,
> resolved against the verified current state in
> `docs/architecture/system-architecture-overview.md` (§9 lists the same open
> questions). Parent plan: `docs/plans/modular-architecture-alignment-plan.md`.
> **Disposition: all recommendations ACCEPTED**, with scoping nuances noted per phase.
>
> **Workflow (mandatory).** Same established procedure: complete one step →
> run unit tests covering affected modules only → commit → next step.
> No full-suite runs per step.

---

## 0. Decision Log (recommendation → disposition)

| # | Recommendation | Disposition | Rationale / nuance |
|---|---------------|-------------|-------------------|
| 0 | Rewrite ArchUnit rules to match real packages; forbid cross-module repo imports | ✅ Accept, **do first** | Current rules guard non-existent `service/model/repository` packages and pass vacuously (`allowEmptyShould(true)`). Guardrails must precede decoupling |
| 1 | App = integration-test module; criterion → "zero unit/slice tests in app" | ✅ Accept | FlowITs/security tests need the composition-root context; verified a domain slice returns 200/404 instead of 401 |
| 2 | Keep `UserProfilePort` in `core-shared` (outbound SPI) | ✅ Accept | Move would force `photo→user` edge → **user↔photo Gradle cycle** (`user→photo` already exists) |
| 3 | Records only for new `api.dto`; freeze legacy response DTOs | ✅ Accept | `BaseResponse`+`@SuperBuilder` hierarchies + hand-written mappers + JSON shape; no MapStruct `@Mapper`s exist, so that sub-risk is moot |
| 4 | Abandon `package-private`; enforce via corrected ArchUnit | ✅ Accept | Package-private is package-exact: same-module `adapter/health/security/auth/service/api` subpackages import parent-package repos (~20 main-source files would break) |
| 5a | Keep `Message→ChatRepository` (aggregate ownership) | ✅ Accept | `Message.chat` `@ManyToOne`; plan §5.1 already says keep |
| 5b | Decouple cross-domain JPA associations → plain `UUID` FKs (long-term) | ✅ Accept as phased migration | `Comment.user`, `Message.sender/recipient`, `Post.user`, reaction actor lookups. Ports return snapshots and cannot populate associations — schema-level decoupling is the only clean fix |
| 5c | `PostSecurityService→UserRepository`: keep as documented exception or auth-aware port | ✅ Accept (keep + document now, port later) | SpEL authorization needs username→entity→id; snapshot ports lack roles |
| IAM | User/Auth/IAM unified in `domain-user`; zero User knowledge in core | ✅ Accept (already true — codify) | Verified: single `domain-user/.../auth/AuthService.java`, no `User` entity in core, no duplicate/shadowed `AuthService`. `core-messaging/.../model/UserRepresentation.java` is a validation DTO, not an entity — acceptable |

---

## Phase A — IAM Conformance Codification (effort: S, risk: low)

Goal: lock in the already-correct IAM placement so it can't regress.

| Step | Action | Gate |
|------|--------|------|
| A.1 | Add ArchUnit rule (core-shared + core-security suites): no class in `core..` may depend on `*.domain.user.User` entity or `*.domain.user.auth.*`. Negative test: assert rule file exists and passes | `:core:core-shared:test`, `:core:core-security:test` |
| A.2 | Record ADR (`docs/adr/`: IAM bounded context = `domain-user`; `UserProfilePort`/`WebSocketUserPort` stay in `core-shared` as outbound SPI; `UserRepresentation` messaging DTO grandfathered) | doc review only |
| A.3 | Amend alignment-plan success criteria: app criterion → "zero unit/slice tests in app"; package-private rows → "enforced via ArchUnit (see Phase B)" | doc review only |

## Phase B — ArchUnit Rewrite (effort: M, risk: low, **do before Phase E**)

Goal: rules that constrain real packages with no vacuous passes.

| Step | Action | Gate |
|------|--------|------|
| B.1 | Per domain module X, replace `service/model/repository`-style rules with: (1) `noClasses in domain.X.. dependOnClassesThat resideInAPackage domain.Y..` for each forbidden Y (keep currently-allowed edges: user→photo/notification, post→user/photo/reaction/comment/notification, comment→user/reaction/notification, reaction→user/notification, chat→user, message→user/chat/photo — encode the §2.2 table as allowlist, everything else denied); (2) `noClasses in domain.X.. dependOnClassesThat haveSimpleNameEndingWith("Repository") AND resideOutsideOfPackage domain.X..` — the explicit cross-module repo ban | affected module `:test` |
| B.2 | Remove `allowEmptyShould(true)` wherever the constrained package exists (keep only for genuinely optional cases); add a meta-test asserting each rule matches ≥1 class where applicable | affected module `:test` |
| B.3 | Core suites: add `core ↛ domain.*` rule (any core class depending on any `domain.` package fails) — today enforced only by Gradle edges | `:core:core-*:test` |

## Phase C — App Test Criterion Amendment (effort: S, risk: low)

| Step | Action | Gate |
|------|--------|------|
| C.1 | Verify zero unit/slice tests remain in app (only `*FlowIT`, `*ServiceIT`, security-posture, `FlywayMigrationIT`, `QuietspaceApplicationIT`, `TestcontainersConfig`, `IntegrationTestHelper`); delete or relocate any stragglers | `:app:quietspace-app:compileTestJava` |
| C.2 | Update alignment-plan §6.1/§Success Criteria: app hosts cross-cutting + FlowIT tests only | doc review only |

## Phase D — Ports Freeze Documentation (effort: S, risk: low)

| Step | Action | Gate |
|------|--------|------|
| D.1 | ADR: `UserProfilePort` + `WebSocketUserPort` permanently in `core-shared` (outbound SPI; cycle analysis user↔photo attached); `api` query ports are display-data-only, auth stays on `UserService` | doc review only |

## Phase E — Cross-Domain JPA Decoupling (effort: L, risk: high, one association per step)

Goal: replace cross-domain `@ManyToOne` entity associations with plain `UUID` FK columns;
consumers resolve display data via `*QueryPort`s. Each sub-step is independently committable.

**Precondition:** Phase B green (rewritten rules catch regressions).

| Step | Association | Change | Consumer fix | Gate |
|------|-------------|--------|--------------|------|
| E.1 | `Comment.user` → `userId UUID` | `Comment.java`: replace assoc with `@Column userId`; keep DB column (FK column already exists — no Flyway DDL needed, only mapping change; verify generated schema matches) | `CommentMapper.commentRequestToEntity` takes id directly; `commentEntityToResponse` username via `UserQueryPort`; `CommentServiceImpl`/`ReactionMapper` actor paths updated; update `CommentMapperTest`, `CommentServiceImplTest`, `CommentRepositoryTest` (drop `UserRepository` fixture where unused) | `:domain:domain-comment:test` + static analysis; `:domain:domain-post:test` (uses `Comment`) |
| E.2 | `Message.sender/recipient` → `senderId/recipientId UUID` | Same pattern as E.1 | `MessageMapper`, `MessageServiceImpl` via `UserQueryPort`; update message tests | `:domain:domain-message:test` + static analysis; `:domain:domain-chat:test` |
| E.3 | `Post.user` → `authorId UUID` + Phase F | **Done as transitional dual mapping**: `Post` keeps the `user` assoc as single writer plus read-only `authorId` FK view (same column, schema-identical); new code (mapper, service, adapters, `PostQueryAdapter`, `repostBy`) uses the id; `PostSecurityService` moved to `UserQueryPort.findUserIdByUsernameOrEmail` (Phase F done, with unit tests). Full removal blocked by `PostSpecifications.visibleToUser` (feed-privacy SQL join — app-level filtering would break pagination/security) and `containsText` username search | `:domain:domain-post:test` + static analysis; `:domain:domain-comment:test`, `:domain:domain-reaction:test`, app compile |
| E.4 | `ReactionMapper` actor lookup | Replace `UserRepository` with `UserQueryPort` for username snapshot | Update `ReactionMapperTest` | `:domain:domain-reaction:test` + static analysis |
| E.5 | Re-run rewritten ArchUnit + remove now-dead Gradle edges if any become unused (e.g. if a module no longer needs a domain dep even in main) | — | affected modules `:test` |

**Per-step test notes:** H2 slice tests need fixture rework (no cross-domain entity graphs);
`@ManyToOne`-dependent JPQL (`c.user.id`) must be rewritten to FK-column queries.
If a step's blast radius exceeds one module's suite, split it — never commit red.

## Phase F — `PostSecurityService` Auth-Aware Port (effort: M, risk: medium, optional)

| Step | Action | Gate |
|------|--------|------|
| F.1 | Either (a) keep as documented exception (default), or (b) extend `UserQueryPort` with `findUserIdByUsernameOrEmail(String): Optional<UUID>` (+ adapter + tests) and rewrite `canAccess` to compare ids without a managed entity | `:domain:domain-user:test`, `:domain:domain-post:test` + static analysis |

---

## Success-Criteria Amendments (to alignment-plan §Success Criteria)

- App module: "only cross-cutting + FlowIT tests; **zero unit/slice tests**" (replaces "<10 tests").
- Repositories: "**no cross-module repository imports** (ArchUnit, Phase B)" (replaces "all package-private").
- New: "no `core → domain.*` dependency (ArchUnit)"; "IAM classes exist only in `domain-user` (ArchUnit)".
- New: "cross-domain entity associations eliminated (E.1–E.4); associations allowed only intra-module and parent-owns-child (message→chat)".
- Unchanged: per-module ArchUnit suites, ≥80% coverage, static-analysis gates, full suite green at baseline.

## Risks

| Risk | Mitigation |
|------|-----------|
| E.x JPQL/H2 divergences (`c.user.id` rewrites) | Keep column names identical; H2 `MODE=MYSQL` slices already cover queries; app FlowITs exercise MySQL |
| E.x fixture churn in cross-module slice tests | Rewrite fixtures to UUID ids; drop unneeded `testImplementation` domain edges where possible |
| ArchUnit rewrite (B) briefly red | Land B.1 per module, smallest first (photo → notification sink → others) |
| `Post.user` removal (E.3) touches auth paths | `getSignedUser()` service stays; only the entity association goes; security tests in app verify behavior |

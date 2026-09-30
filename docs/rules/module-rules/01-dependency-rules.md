# Rule 01 — Dependency & Separation Rules

> Extracted from: ADR 001, ADR 004, `modular-architecture-overview.md` §2, `modular-architecture-review.md` §1, `system-architecture-overview.md` §2.

## 1. Tier model

- System has 3 tiers: **Core** (5 modules: `core-shared`, `core-data`, `core-web`, `core-security`, `core-messaging`), **Domain** (8 modules: `domain-user`, `domain-post`, `domain-photo`, `domain-comment`, `domain-reaction`, `domain-chat`, `domain-message`, `domain-notification`), **App** (`quietspace-app`, composition root).
- Core = reusable infrastructure, domain-agnostic. Must be extractable to a different system with zero modifications.
- Domain = one bounded context / aggregate owner per module. App = wiring, Flyway, config, integration tests only.

## 2. Allowed / forbidden edges

| Edge | Verdict | Enforcement |
|------|---------|-------------|
| Core → Domain | **FORBIDDEN** | ArchUnit (`no_core_user_entity_dependency`, `no_core_auth_dependency`, per-core rules) + no `domain:` deps in `core/*/build.gradle.kts` |
| Domain → Domain internal packages (`service`, `controller`, `model/entity`, `repository`, `adapter`) | **FORBIDDEN** | ArchUnit per-module rules |
| Domain → other module's `*Repository` | **FORBIDDEN** | ArchUnit `has_no_cross_module_repository_dependency` |
| Domain → Domain `api` query port / DTO | **ALLOWED** (explicit `implementation(project(":domain:..."))` allowlist) | Gradle declaration + ArchUnit allowlist |
| Domain → consumer-owned `port/` interface (to implement) | **ALLOWED** (provider depends on consumer's port) | Gradle declaration |
| Domain → Core | **ALLOWED** | Gradle declaration |
| App → All | **ALLOWED** | Composition root only |

## 3. Cycle rules

- The domain dependency graph must stay a **DAG**. Gradle fails on cycles.
- **Known load-bearing edges — do not invert:**
  - `user→photo` exists (`UserServiceImpl` → `PhotoService.deletePhotoByEntityId`). Therefore `UserProfilePort` **stays in `core-shared`**; moving it to `domain-user.api` would force `photo→user` and create a `user↔photo` cycle. (ADR 004, system-overview §2.2)
  - `message→chat` is **aggregate ownership** (`Message.chat` `@ManyToOne`), deliberately kept — not a violation to "fix".
  - `domain-notification` is a **sink**: zero domain `implementation` deps (only test-only fixtures). `domain-photo` is a **leaf**: zero domain deps.
- Test-only edges (`comment/reaction→post`, `notification→user`, `comment→post` fixtures, `core-web` in test scope) are allowed via `testImplementation` only. They do not affect production coupling; ArchUnit excludes tests from imports.

## 4. IAM bounded context (ADR 004)

- `domain-user` solely owns `User`, `ProfileSettings`, `AuthService`, `UserAuthenticationProvider`, `UserQueryPort`.
- No `User` entity or `AuthService` duplicate in core. `core-messaging`'s `UserRepresentation` is a validation DTO, not an entity.
- Other domains read user display data via `UserQueryPort` or domain events — never via direct DB lookups or new `@ManyToOne User` associations.
- Auth behavior changes stay in `domain-user`; `core-security` only sees `AuthenticationProvider`/`JwtTokenService` abstractions.

## 5. Refactor triggers (divergence smells)

- [ ] New import of `dev.thural.quietspace.domain.<other>..` outside `api/` or `port/` → reroute via query port or consumer-owned port.
- [ ] New `*Repository` import from another module → replace with query port method.
- [ ] New JPA association to another aggregate's entity → use plain `UUID` FK column + query port instead (see Rule 02 §5).
- [ ] New `core → domain` reference → invert: define SPI in `core-shared`, implement adapter in the domain module.
- [ ] Temptation to add `domain-X → domain-Y` `implementation` dep → check DAG + cycle rules above first; prefer event (async) or port (sync).

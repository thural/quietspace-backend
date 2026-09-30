# Module Architecture Rules — Index

> Source of truth extracted from `docs/architecture/`, `docs/adr/`, `docs/build/`, `docs/guides/`.
> Purpose: prevent architectural divergence when creating a new module or refactoring an existing one.
> Enforcement is by tooling (Gradle + ArchUnit + static analysis), not just documentation — violations fail the build.

## Rule files

| File | Category | When to use |
|------|----------|-------------|
| `01-dependency-rules.md` | Core/Domain split, allowed/forbidden edges, cycle avoidance | Every new dependency, every refactor touching imports |
| `02-module-structure.md` | Package layout, aggregate/entity/mapper/DTO conventions, JPA decoupling | Creating or restructuring a module's packages/classes |
| `03-communication-rules.md` | Consumer-owned ports, query ports, domain events + outbox, idempotency | Any cross-module read, write, or notification |
| `04-testing-rules.md` | Test pyramid placement, slice rules, naming, H2/Testcontainers | Adding or moving any test |
| `05-build-quality-gates.md` | Gradle conventions, BootJar control, SpotBugs/PMD/Checkstyle/JaCoCo/ArchUnit gates | Touching `*.gradle.kts`, `settings.gradle.kts`, `buildSrc/`, CI |
| `06-new-module-checklist.md` | Step-by-step checklist for new modules + refactor checklist | **Start here** for create/refactor work |

## The 10 non-negotiables (summary)

1. **Core has zero domain dependencies.** `core-*` never imports `domain.*`. (ADR 001, ADR 004)
2. **No cross-module repository imports.** Cross-domain reads go through `api` query ports. Enforced by ArchUnit `has_no_cross_module_repository_dependency`.
3. **No cross-domain internal access.** Never import another domain's `service`, `controller`, `model/entity`, `repository`, `adapter` internals — only `api` ports/DTOs and consumer-owned `port/` interfaces.
4. **Controllers never touch repositories.** Controller → Service → Repository only.
5. **New code uses FK-ID columns + query ports, not JPA associations across aggregates.** (`Post.user`, `Comment.user`, `Message.sender/recipient` patterns; transitional `authorId` dual mapping.)
6. **Consumer owns the port; provider implements it.** Port interface in consumer (`port/` or `api/`); `@Component` adapter in provider.
7. **State changes propagate via transactional outbox events.** Publish with `TransactionalEventPublisher` in the same TX; consumers are idempotent via `processed_events`.
8. **Events and shared SPI live in `core-shared`.** New event = class in `core-shared/event/` + serializer test. `UserProfilePort`/`WebSocketUserPort` stay in `core-shared` (moving `UserProfilePort` to `domain-user.api` creates a `user↔photo` cycle).
9. **Only `quietspace-app` is executable.** Library modules disable `bootJar`; integration (`*FlowIT`) tests live only in `app`.
10. **Every module passes all gates:** tests, ≥80% JaCoCo coverage, ArchUnit, SpotBugs, PMD, Checkstyle.

## Canonical references (if rules conflict, these win)

- `docs/architecture/modular-architecture-overview.md`
- `docs/architecture/modular-architecture-review.md`
- `docs/architecture/system-architecture-overview.md`
- `docs/architecture/domain-modules/domain-module-contracts.md`
- `docs/architecture/core-modules/core-module-apis.md`
- `docs/architecture/events.md`
- `docs/adr/001-core-domain-split.md`, `004-iam-bounded-context.md`, `003-transactional-outbox.md`, `005-feed-visibility-readmodel.md`
- `docs/guides/migration/adding-domain-module.md`
- `docs/build/gradle-modular-build-system.md`

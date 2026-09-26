# Testing Guide

How tests are organized in quietspace-platform, and the hard-won rules that keep
them green. Every rule below cost at least one broken build to learn.

## Test layers and where they live

| Layer | Location | Framework | Notes |
|-------|----------|-----------|-------|
| Unit (services, mappers, adapters, events, utils) | owning module `src/test` | JUnit 5 + Mockito (`MockitoExtension`), AssertJ | No Spring context. Mockito default is **strict stubs**: stub exactly what the exercised path calls — an unused stub fails the test (`UnnecessaryStubbingException`). Common trap: stubbing a port lookup in a test whose path returns early (e.g. repost with missing original never reaches username resolution). |
| Web slice | `*.controller.*SliceTest` | `@WebMvcTest`, `MockMvc`, `@MockitoBean` | Slice tests use `@AutoConfigureMockMvc(addFilters = false)` + a local `TestConfig` providing `ObjectMapper`. **Security-posture tests do NOT belong here**: without `core-security` on the slice, unauthenticated requests return 200/404 instead of 401 (verified experimentally). |
| Persistence slice | `*.repository.*RepositoryTest` | `@DataJpaTest` on **H2** | See "H2 rules" below — all three are mandatory together. |
| Contract | `*.api.*AdapterTest` | Mockito unit tests | One test class per `*QueryPort`/`*CommandPort` adapter: happy path, missing-entity path, empty-batch path (must not touch the repository). |
| Architecture | `*.archunit.*ArchitectureRulesTest` | ArchUnit, JUnit 5 | Rules must target **real packages** (flat roots like `domain.post..`, real subpackages `api`, `controller`, `dto`). Never write rules against assumed `service/model/repository` layer packages, and avoid `allowEmptyShould(true)` where the constrained package exists — it hides vacuous rules. The `has_no_cross_module_repository_dependency` pattern is the primary boundary guard. |
| Integration | `app/quietspace-app` **only** | `@SpringBootTest` + Testcontainers (MySQL, RabbitMQ) | `quietspace-app` is the sole composition root. Full-context, filter-chain, and multi-aggregate flow tests (`*FlowIT`, `*ServiceIT`, `SecurityFlowIT`, `FlywayMigrationIT`) live here and nowhere else: domain modules cannot host them without circular Gradle deps or duplicated wiring. |

## H2 rules for `@DataJpaTest` in domain modules (all three, always)

1. Each domain module has `src/test/resources/application.yml` pinning H2 with
   `url: jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=user`
   (`user` is an H2 reserved word; without `NON_KEYWORDS` every `insert into user` fails).
2. Tests use `@AutoConfigureTestDatabase(replace = Replace.NONE)` so the configured
   URL is honored. The `@DataJpaTest` default (replace with embedded DB) **ignores**
   the URL and reintroduces the reserved-word failure.
3. `com.h2database:h2` comes from the shared domain-conventions plugin
   (`testRuntimeOnly`) — do not redeclare per module.

## Fixture rules for repository slices

- Set FK ids in `@BeforeEach` **after** saves, not in field initializers
  (ids don't exist until `save()` flushes them back into the instance).
- Dual-mapped columns (`Post.user` assoc + read-only `authorId` view, Phase E.3):
  set **both** — the association is `@NotNull`-validated on persist.
- `User` rows: mirror `UserServiceIT` minimal shape (email/username/password/role/
  enabled/accountLocked/dates).

## Slice tests for JPA Specifications

`@DataJpaTest` + manually constructed specs + real repositories is the approved pattern
(see `PostRepositoryTest.testVisibleToUser_*`): autowire the repositories under test,
`Mockito.mock(...)` the services/ports the spec needs (`UserService.getSignedUser`),
and assert page contents. This exercises real SQL (catches bad JPQL paths like the
removed `c.user.id` traversal) without the app context.

## Workflow

Per-step procedure (no exceptions): implement → run the **affected modules'**
`:test` + `spotbugsMain/Test` + `pmdMain/Test` + `checkstyleMain/Test` →
commit → next step. Never run the full suite per step; never commit red.
Keep `git status` free of IDE output (`**/bin/` is gitignored — Gradle uses `build/`).

# Rule 04 — Testing Rules

> Extracted from: `docs/guides/tests/unit-test-guidelines.md`, `docs/guides/tests/integration-test-guidelines.md`, `system-architecture-overview.md` §8, `gradle-modular-build-system.md` §7.

## 1. Pyramid placement (where the test lives)

| Layer | Location | Tech | Rule |
|-------|----------|------|------|
| Unit (services, mappers, adapters, utils) | owning module `src/test` | Mockito only, no Spring context | Fast (ms); every dep `@Mock`, CUT `@InjectMocks`; value objects/DTOs/entities are real |
| Slice — web | owning module, `*ControllerSliceTest` | `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters=false)` | Mock all services with `@MockitoBean`; Spring Boot 4.x: `@WebMvcTest` is in `org.springframework.boot.webmvc.test.autoconfigure.*`; must also mock `JwtFilter` deps (`TokenRepository`, `JwtService`, `UserDetailsService`); provide `ObjectMapper` via static inner `@TestConfiguration` |
| Slice — persistence | owning module, `*RepositoryTest` | `@DataJpaTest` + H2 | Mandatory `@AutoConfigureTestDatabase(Replace.NONE)`; H2 URL with `MODE=MYSQL;NON_KEYWORDS=user`; no manual cleanup (TX rollback); never mock repos |
| Contract | owning module, `*QueryAdapterTest` | Mockito (`@ExtendWith(MockitoExtension.class)`) | One per `api` port |
| Architecture | owning module, `*.archunit.*ArchitectureRulesTest` (+ global) | ArchUnit | Real packages, no vacuous `allowEmptyShould(true)`; must include cross-module repository-import ban |
| Integration (`*FlowIT`, `*ServiceIT`, security, `FlywayMigrationIT`) | **`app` module only** | `@SpringBootTest(RANDOM_PORT)` + Testcontainers MySQL/RabbitMQ | Domain modules have zero full-context ITs. App has zero unit/slice tests. |

## 2. Mandatory test rules

- **Coverage ≥80% per module** (JaCoCo verification). New module merges only with ≥80%.
- **Method naming:** `methodName_givenScenario_shouldExpectedBehavior` (both unit and IT).
- **AAA pattern** with blank-line sections; AssertJ assertions; `verify()` only for critical external side-effects (save, send, publish) — never for getters/setters or private helpers.
- **FIRST principles:** fast, independent (fresh mocks per test), repeatable, self-validating, thorough (happy + error paths: 400/401/404/409/500 for endpoints).
- **No logic in tests:** no `if`/`for` to build assertions — use `@ParameterizedTest`. Never mock `List`/`Map`/`String`/`UUID`/entities/DTOs or types you don't own.
- **Anti-patterns:** no `@SpringBootTest`/`@MockBean`/`@Autowired` in unit tests; no `@DirtiesContext`; no mocked repositories in ITs (only external services like `EmailService`/`PhotoService` via `@MockitoBean`); no live external services (use WireMock); `@SpringBootTest` only when a slice is insufficient; full-context ITs must be named `*IT.java` so the `integrationTest` task picks them up.
- **IT template (Spring Boot 4.x):** `@AutoConfigureMockMvc` is required with `RANDOM_PORT`; add `@Transactional` when `@BeforeEach` does direct repository/`EntityManager` work; `@Import(TestcontainersConfig.class)` + `@ActiveProfiles("testcontainers")`; clean with `deleteAll()` in `@BeforeEach`; auth via `IntegrationTestHelper.registerAndLogin()`; security-gating tests (no token → 401) must NOT use `addFilters=false`; WebSocket ITs use `@LocalServerPort` + JWT in STOMP headers with a `Future.get(timeout)`.

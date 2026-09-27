# Integration Testing Guidelines — quietspace-backend

## Table of Contents
1. [What Is an Integration Test?](#1-what-is-an-integration-test)
2. [Test Pyramid Placement](#2-test-pyramid-placement)
3. [Test Slices](#3-test-slices)
4. [Full-Context Integration Tests](#4-full-context-integration-tests)
5. [Build Tool Configuration](#5-build-tool-configuration)
6. [Naming Conventions](#6-naming-conventions)
7. [Testcontainers Setup](#7-testcontainers-setup)
8. [WireMock for External Services](#8-wiremock-for-external-services)
9. [Data Cleanup Strategy](#9-data-cleanup-strategy)
10. [IntegrationTestHelper Utility](#10-integrationtesthelper-utility)
11. [Anti-Patterns](#11-anti-patterns)

---

## 1. What Is an Integration Test?

An integration test verifies that multiple layers of the application work together correctly. In this project, an integration test must touch **at least two of these layers**:

- **Web layer** — HTTP request/response, serialization, validation, security filters
- **Service layer** — business logic, transactions, authorization decisions
- **Data layer** — JPA repositories, Hibernate, actual SQL generation, database constraints

If every dependency is mocked, it is a **unit test**, not an integration test. See `docs/tests/unit-test-guidelines.md`.

---

## 2. Test Pyramid Placement

```
         ╱╲
        ╱  ╲
       ╱ UI ╲          ← E2E (Selenium, Playwright) — not in this project
      ╱──────╲
     ╱  INT   ╲        ← Integration (Spring context, real DB, WireMock) — ~20–30 tests
    ╱──────────╲
   ╱   UNIT     ╲      ← Unit (pure Mockito) — ~100+ tests
  ╱──────────────╲
```

**Rule of thumb:** Integration tests are **authoritative** for correctness but **slow**. Unit tests are **fast** but verify isolation. Each bug fix should start with an integration test that reproduces the bug, then a unit test that covers the specific logic.

---

## 3. Test Slices

Do not use `@SpringBootTest` for every integration test. Use Spring's slice annotations to load only the required context.

### 3.1 Controller Slice — `@WebMvcTest`

Use for testing HTTP endpoints, request validation, response serialization, and security gating. Mock all service-layer dependencies with `@MockitoBean`.

> **Spring Boot 4.x package change:** `@WebMvcTest` and `@AutoConfigureMockMvc` are now in `org.springframework.boot.webmvc.test.autoconfigure.*` (the `spring-boot-webmvc-test` module), **not** `org.springframework.boot.test.autoconfigure.web.servlet.*`.

```java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    void getAllPosts_shouldReturn200() throws Exception {
        when(postService.getAllPosts(anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk());
    }
}
```

**Key rules:**
- Use `@AutoConfigureMockMvc(addFilters = false)` to disable security filters for standard controller tests.
- In Spring Boot 4.x, `@WebMvcTest` does **not** auto-exclude `JwtFilter`. You **must** add `@MockitoBean` for all three of its dependencies (`TokenRepository`, `JwtService`, `UserDetailsService`) in every slice test.
- Provide an `ObjectMapper` bean in a **static inner `@TestConfiguration`**:
  ```java
  @TestConfiguration
  static class TestConfig {
      @Bean
      ObjectMapper objectMapper() {
          return new ObjectMapper();
      }
  }
  ```
- Mock **all** service-level beans with `@MockitoBean`.
- Package: `dev.thural.quietspace.controller.slice`

### 3.2 Repository Slice — `@DataJpaTest`

Use for testing custom repository queries, projections, and database constraints.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;
}
```

**Key rules:**
- Always include `@AutoConfigureTestDatabase(replace = NONE)`.
- Each test runs in a transaction that auto-rolls back — no manual cleanup needed.
- Use `@BeforeEach` to persist seed data and `@AfterEach` to delete it if cleanup beyond rollback is needed.
- Do **not** mock repositories. The purpose is to test real SQL generation.

### 3.3 When to Use Each Slice vs `@SpringBootTest`

| Test Focus | Annotation | Why |
|---|---|---|
| Single controller, mock services | `@WebMvcTest` | Fast, no DB needed |
| Single repository, custom query | `@DataJpaTest` | Fast, H2 only |
| Multi-layer flow (controller → service → DB) | `@SpringBootTest` + Testcontainers | Authoritative end-to-end behavior |
| WebSocket STOMP messaging | `@SpringBootTest` + Testcontainers | WebSocket requires full server |
| Service with external HTTP dependency | `@SpringBootTest` + WireMock | Real service context |

---

## 4. Full-Context Integration Tests

Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` for tests that exercise multiple layers end-to-end. Every `@SpringBootTest` integration test must follow this template (note: `@AutoConfigureMockMvc` is **required** in Spring Boot 4.x for `@Autowired MockMvc` to work with `RANDOM_PORT`):

```java
@AutoConfigureMockMvc
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
class PostFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PhotoService photoService;

    private IntegrationTestHelper helper;
    private String jwtToken;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        postRepository.deleteAll();
        userRepository.deleteAll();
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        jwtToken = helper.registerAndLogin("testuser@test.com", "password123");
        userId = userRepository.findUserEntityByEmail("testuser@test.com").orElseThrow().getId();
    }
}
```

**Key rules:**
- Always add `@AutoConfigureMockMvc` with `@SpringBootTest(webEnvironment = RANDOM_PORT)` — Spring Boot 4.x does **not** auto-configure MockMvc for `RANDOM_PORT` contexts.
- Add `@Transactional` to the test class when `@BeforeEach` performs **direct** `repository`/`EntityManager` operations (e.g., `deleteAll()`, native cleanup queries via `IntegrationTestHelper.cleanDatabase`). Without an active transaction, Spring Boot 4.x throws `TransactionRequiredException` because no web request is in flight. The transaction rolls back after each test, keeping tests isolated.
- Use `RANDOM_PORT` to avoid port conflicts.
- Import `TestcontainersConfig` and activate `testcontainers` profile for a real MySQL container.
- Mock all **external** services (`EmailService`, `PhotoService`) with `@MockitoBean`.
- Never mock repositories — the test must hit the real database.
- Use `IntegrationTestHelper.registerAndLogin()` to obtain a JWT token for authenticated requests.
- Include both **happy-path** and **error-path** tests (400, 401, 404, 409, 500).

### 4.1 Security Gating Tests

For a focused security test (no token → 401), do **not** use `@AutoConfigureMockMvc(addFilters = false)`:

```java
@WebMvcTest(controllers = PostController.class)
class PostControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void getPostById_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
```

### 4.2 WebSocket Integration Tests

Full-context WebSocket tests follow the same `@SpringBootTest` + Testcontainers pattern but add a STOMP client:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
class WebSocketFlowIT {

    @LocalServerPort
    private int port;

    @MockitoBean
    private PhotoService photoService;

    @Test
    void connectAndReceivePublicMessage_shouldWork() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.set("Authorization", "Bearer " + jwtToken);

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        stompClient.connectAsync("ws://localhost:" + port + "/ws",
                        handshakeHeaders, connectHeaders,
                        new StompSessionHandlerAdapter() { ... })
                .get(10, TimeUnit.SECONDS);
    }
}
```

**Key rules:**
- Use `@LocalServerPort` to inject the dynamic port.
- Pass JWT in `StompHeaders` for authentication.
- Use `WebSocketHttpHeaders` for the handshake (separate from STOMP connect headers).
- Add a timeout with `Future.get(seconds, TimeUnit)` to avoid hanging.

---

## 5. Build Tool Configuration

Integration tests are separated from unit tests in `../../../build.gradle.kts`:

```kotlin
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests only."
    group = "verification"
    useJUnitPlatform()
    filter {
        includeTestsMatching("*IT")
        includeTestsMatching("*ITCase")
    }
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(integrationTest)
}
```

- Run unit tests: `./gradlew test`
- Run integration tests: `./gradlew integrationTest`
- Run both: `./gradlew check`

---

## 6. Naming Conventions

| Test Type | Suffix | Location | Example |
|---|---|---|---|
| Unit test | `*Test.java` | `controller/unit/`, `service/`, `mapper/`, `security/` | `PostServiceImplTest.java` |
| Controller slice test | `*Test.java` | `controller/slice/` | `PostControllerTest.java` |
| Security slice test | `*Test.java` (no `addFilters = false`) | `controller/slice/` | `PostControllerSecurityTest.java` |
| Repository slice test | `*Test.java` | `repository/` | `PostRepositoryTest.java` |
| Full-context IT | `*IT.java` | `controller/`, `service/`, `websocket/` | `PostFlowIT.java` |
| Service-layer IT | `*IT.java` | `service/` | `UserServiceIT.java` |

**File name** for test methods: `methodName_givenScenario_shouldExpectedBehavior`

```
createPost_givenValidRequest_shouldReturn200()
getPostById_givenNonExistentId_shouldReturn404()
```

---

## 7. Testcontainers Setup

Configuration in `src/test/java/.../config/TestcontainersConfig.java`:

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
    }
}
```

Profile configuration in `src/test/resources/application-testcontainers.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    database-platform: org.hibernate.dialect.MySQLDialect
  flyway:
    enabled: false
```

**Key rules:**
- `@ServiceConnection` auto-configures the Spring `DataSource` from container metadata.
- `ddl-auto: create-drop` ensures a clean schema before each test class.
- Flyway is disabled since Hibernate generates the schema.

---

## 8. WireMock for External Services

Use WireMock to stub external HTTP services (e.g., SMTP, REST APIs) instead of connecting to live staging services.

```java
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@WireMockTest(httpPort = 1025)
class EmailServiceIT {

    @Autowired
    private EmailService emailService;

    @Test
    void sendEmail_shouldDeliverSuccessfully() throws Exception {
        stubFor(post("/").willReturn(aResponse().withStatus(200)));

        emailService.sendEmail(
                "recipient@test.com", "Test User",
                EmailTemplateName.ACTIVATE_ACCOUNT,
                "http://localhost:3000/activate",
                "123456", "Account Activation"
        );

        verify(postRequestedFor(urlEqualTo("/")));
    }
}
```

**Key rules:**
- Use `@WireMockTest(httpPort = 1025)` at the class level.
- Declare WireMock dependency as `testImplementation("org.wiremock:wiremock-standalone:3.9.1")`.
- Stub the external endpoint before calling the service method.
- Verify the expected HTTP request was made.

---

## 9. Data Cleanup Strategy

| Test Type | Cleanup Method | Mechanism |
|---|---|---|
| `@DataJpaTest` | Automatic | Transactional rollback — no manual cleanup |
| `@SpringBootTest` IT | `@BeforeEach` | Call `repository.deleteAll()` on all repositories touched |
| `@WebMvcTest` slice | Automatic | No real database — all beans mocked |

**Good:**
```java
@BeforeEach
void setUp() throws Exception {
    postRepository.deleteAll();
    userRepository.deleteAll();
    // ... seed data
}
```

**Bad:** Using `@DirtiesContext` to force a context reload after every test class.

---

## 10. IntegrationTestHelper Utility

`IntegrationTestHelper` (`src/test/java/.../utils/IntegrationTestHelper.java`) provides two reusable methods:

```java
// Creates a USER-role user directly in DB with encoded password + ProfileSettings,
// then authenticates via POST /api/v1/auth/authenticate and returns the JWT.
String registerAndLogin(String email, String password) throws Exception;

// Same but with Role.ADMIN.
String registerAndLoginAdmin(String email, String password) throws Exception;
```

This avoids the activation-token dance required by the real registration flow.

**Instantiation pattern:**
```java
helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
```

---

## 11. Anti-Patterns

### 11.1 `@DirtiesContext` Overuse

**Don't:**
```java
@DirtiesContext  // Bad: destroys context caching, minutes of extra boot time
```

**Why:** Spring caches the `ApplicationContext` between test classes with identical configuration. `@DirtiesContext` forces a full restart, adding 10–30 seconds per boot.

**Fix:** Clean up data in `@BeforeEach` with `deleteAll()`, or rely on `@DataJpaTest` transactional rollback.

### 11.2 Mocking Database Repositories in Integration Tests

**Don't:**
```java
@SpringBootTest
class PostFlowIT {
    @MockitoBean  // Bad: if the repository is mocked, this is not an integration test
    private PostRepository postRepository;
}
```

**Why:** A repository mock means no SQL is generated, no constraints are validated, and no actual database interaction occurs. This hides bugs in queries, schema mappings, and transaction boundaries.

**Fix:** Only mock **external** services (`EmailService`, `PhotoService`). Let integration tests hit the real database (MySQL via Testcontainers).

### 11.3 Relying on Live External Services

**Don't:**
```java
// Bad: depends on a real SMTP server
emailService.sendEmail("real@example.com", ...);
```

**Why:** Live services introduce flakiness due to network issues, rate limits, and authentication changes. CI builds become unreliable.

**Fix:** Use WireMock (or an equivalent in-memory stub) for every external HTTP dependency.

### 11.4 Data Contamination Between Tests

**Don't:**
```java
@BeforeEach
void setUp() {
    // Missing cleanup — data from Test A leaks into Test B
    userRepository.save(user);
}
```

**Why:** Violates the "Independent" principle. Tests become order-dependent and flaky.

**Fix:** Always start with a clean slate. Call `deleteAll()` in `@BeforeEach`, or use `@Transactional` rollback.

### 11.5 Using `@SpringBootTest` When a Slice Suffices

**Don't:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest { ... }  // Bad: boots full context for controller-only test
```

**Why:** Increases test runtime by 10–30× (full context boot + MySQL container vs web slice only).

**Fix:** Use `@WebMvcTest(controllers = PostController.class)` for controller tests, `@DataJpaTest` for repository tests.

### 11.6 Inconsistent Naming or Task Placement

**Don't:**
```java
@SpringBootTest
class PostControllerIntegrationTest { ... }  // Bad: won't be picked up by integrationTest task
```

**Why:** A full-context test named `*Test` runs during `./gradlew test`, slowing the fast unit test cycle.

**Fix:** Name full-context integration tests `*IT.java` so they are exclusively run by the `integrationTest` task.

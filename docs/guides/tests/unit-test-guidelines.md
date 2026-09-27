# Unit Testing Guidelines — quietspace-backend

## Table of Contents
1. [FIRST Principles](#1-first-principles)
2. [Strict Isolation](#2-strict-isolation)
3. [AAA Pattern](#3-aaa-pattern)
4. [Test Naming](#4-test-naming)
5. [Anti-Patterns](#5-anti-patterns)
6. [Behavior-Based vs Implementation-Based](#6-behavior-based-vs-implementation-based)

---

## 1. FIRST Principles

Every unit test **must** satisfy all five pillars:

| Pillar | Requirement |
|--------|------------|
| **Fast** | Complete in milliseconds. Hundreds of tests should run in under a second. |
| **Independent** | Never depend on execution order or state left by another test. Each test gets a fresh set of mocks. |
| **Repeatable** | Same result on any machine, any JDK, any number of runs. No flaky timeouts, thread races, or environment assumptions. |
| **Self-Validating** | A clear pass/fail via assertions. Never rely on manual log inspection. |
| **Thorough / Timely** | Cover happy paths, error paths, edge cases, and null guards. Write tests *with* or *before* the code. |

---

## 2. Strict Isolation

- Use **only** `@ExtendWith(MockitoExtension.class)` — never `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, `@MockBean`, or `@Autowired`.
- Every dependency of the class under test **must** be mocked with `@Mock`.
- The class under test gets its mocks injected via `@InjectMocks`.
- Value objects (POJOs, DTOs, entities) are **real instances** built with `Builder` or constructors — never mocked.
- Java standard library types (`List`, `Map`, `String`, etc.) are never mocked.

### Example

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();
    }
}
```

---

## 3. AAA Pattern

Structure every test method into three clear sections separated by blank lines:

```
[Arrange]   — Create test data, stub mock behavior
[Act]       — Call the single method under test
[Assert]    — AssertJ assertions + Mockito verify() for critical side-effects
```

### Example

```java
@Test
void getUserById_givenExistingUser_shouldReturnResponse() {
    // Arrange
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(userResponse);

    // Act
    UserResponse result = userService.getUserById(user.getId());

    // Assert
    assertThat(result).isEqualTo(userResponse);
}
```

---

## 4. Test Naming

Use the convention: `methodName_givenScenario_shouldExpectedBehavior`

Alternatively, you may use `@DisplayName`.

### Good

```java
void registerUser_givenExistingEmail_shouldThrowUserAlreadyExistsException()
void getPostById_givenNonExistentId_shouldReturnEmpty()
void toggleFollow_givenNotCurrentlyFollowing_shouldAdd()
```

### Bad

```java
void testSaveUser()      // Does not say what scenario or expected outcome
void testDeletePost()    // What is being tested? Success? Not found? Unauthorized?
```

---

## 5. Anti-Patterns

### 5.1 The "Spring Boot for Everything" Anti-Pattern

**Don't:** Use `@SpringBootTest`, `@MockBean`, `@Autowired`, or any Spring context annotation in a unit test.

**Why:** It boots the entire ApplicationContext, turning a 5 ms test into 5–30 seconds. This kills the "Fast" pillar.

**Fix:** Pure `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`.

### 5.2 Over-Mocking / Mocking Types You Don't Own

**Don't:** Mock `List`, `Map`, `User` entities, `String`, `UUID`, or third-party library internals.

**Why:** Mocking data containers makes tests verbose and brittle. Mocking external libraries hides breaking changes.

**Fix:** Create real instances of POJOs, entities, and DTOs. Only mock complex behavioral dependencies (services, repositories, mappers).

### 5.3 Testing Implementation Details (Brittle Tests)

**Don't:** Write tests that verify *how* a method works — e.g., `verify(mock, times(3)).someHelper()` on five internal private methods.

**Why:** Refactoring internals breaks the test even when the external behavior is identical. This causes "test fatigue."

**Fix:** Assert on the **return value** or the **exception thrown**. Only use `verify()` for critical external side-effects (saving to a database, sending an email, publishing a WebSocket event).

### 5.4 Logic in Tests (Smart Tests)

**Don't:** Put `if`/`else`, `for` loops, or complex string building inside test methods to dynamically generate assertions.

**Why:** Test code with logic can itself have bugs. You'd need tests for your tests.

**Fix:** Keep tests linear and predictable. For multiple input variations, use JUnit 5 `@ParameterizedTest`.

### 5.5 "Success-Only" Testing

**Don't:** Only test the happy path.

**Why:** Production failures come from unexpected inputs, missing data, null values, and infrastructure errors.

**Fix:** Every test for a happy path should have a corresponding error-path test:

```java
@Test
void getUserById_givenNonExistentId_shouldThrow() {
    when(userRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(UUID.randomUUID()))
            .isInstanceOf(EntityNotFoundException.class);
}
```

---

## 6. Behavior-Based vs Implementation-Based

The goal of unit tests is to act as a **refactoring safety net**. A test should pass **as long as the external behavior of the method is unchanged**, regardless of internal implementation changes.

### Behavior-Based (Good) ✓

```java
// Tests what the method DOES, not how it does it
@Test
void registerUser_givenValidRequest_shouldReturnAuthResponse() {
    when(userRepository.save(any())).thenReturn(savedUser);
    when(jwtService.generateToken(savedUser)).thenReturn("jwt");

    AuthResponse result = authService.register(request);

    assertThat(result.getAccessToken()).isEqualTo("jwt");
}
```

After refactoring (extracting helper methods, renaming variables, changing internal data structures), this test **still passes** because the output is unchanged.

### Implementation-Based (Bad) ✗

```java
// Tests HOW the method achieves its result — fragile
@Test
void registerUser_givenValidRequest_shouldCallPrivateHelper() {
    // BAD: verifying internal method calls that may change during refactoring
    verify(userService, times(1)).validateEmail(request);
    verify(userService, times(1)).hashPassword(request);
    verify(userService, times(1)).buildUserEntity(request);
}
```

After refactoring (e.g., inlining `validateEmail` into the main method), this test **breaks** even though the registration still works correctly. Over time, this makes developers hate writing tests.

### When `verify()` Is Acceptable

Use `verify()` **only** for critical external side-effects whose absence would represent a real bug:

```java
// ACCEPTABLE: side-effect is a core part of the contract
verify(notificationService).send(any());
verify(repository).save(any());
verify(template).convertAndSendToUser(anyString(), anyString(), any());
```

```java
// UNNECESSARY: verifying a getter/setter is noise
verify(user).getUsername();
verify(user).setOnlineStatus(any());
```

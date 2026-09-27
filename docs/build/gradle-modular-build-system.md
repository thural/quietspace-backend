# Gradle Multi-Module Build System for Modular Architecture

## Overview

This document provides a comprehensive guide to how the Gradle multi-module build system is implemented to achieve and enforce the modular architecture of the QuietSpace platform. The build system is not just a compilation tool—it is the **primary enforcement mechanism** for architectural boundaries.

---

## 1. High-Level Build Structure

```
quietspace-platform/
├── settings.gradle.kts              # Module declaration (15 modules)
├── build.gradle.kts                 # Root build: conventions, quality gates, bootJar control
├── gradle.properties                # JVM args, parallel execution, caching
├── buildSrc/                        # Build logic as code (convention plugins)
│   └── src/main/kotlin/
│       └── quietspace.domain-conventions.gradle.kts
├── core/
│   ├── core-shared/build.gradle.kts
│   ├── core-data/build.gradle.kts
│   ├── core-web/build.gradle.kts
│   ├── core-security/build.gradle.kts
│   └── core-messaging/build.gradle.kts
├── domain/
│   ├── domain-user/build.gradle.kts
│   ├── domain-post/build.gradle.kts
│   ├── domain-photo/build.gradle.kts
│   ├── domain-comment/build.gradle.kts
│   ├── domain-reaction/build.gradle.kts
│   ├── domain-chat/build.gradle.kts
│   ├── domain-message/build.gradle.kts
│   └── domain-notification/build.gradle.kts
└── app/
    └── quietspace-app/build.gradle.kts
```

---

## 2. settings.gradle.kts — Module Declaration

```kotlin
// settings.gradle.kts
rootProject.name = "quietspace-platform"

include("core:core-shared")
include("core:core-data")
include("core:core-web")
include("core:core-security")
include("core:core-messaging")
include("domain:domain-user")
include("domain:domain-post")
include("domain:domain-photo")
include("domain:domain-comment")
include("domain:domain-reaction")
include("domain:domain-chat")
include("domain:domain-message")
include("domain:domain-notification")
include("app:quietspace-app")
```

**Purpose**: Explicitly declares all 15 modules. The `core:` and `domain:` prefixes create logical grouping in IDEs and build reports.

---

## 3. Root build.gradle.kts — Centralized Conventions

The root `build.gradle.kts` is the **single source of truth** for build conventions applied to all 15 modules.

### 3.1 Plugin Application (Applied to All Subprojects)

```kotlin
subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "pmd")
    apply(plugin = "checkstyle")
    // ...
}
```

**Why**: Every module gets the same plugins without duplication.

### 3.2 Java Toolchain & Compilation

```kotlin
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Amapstruct.defaultComponentModel=spring")
    options.release = 21  // Compile with Java 21, target Java 25 runtime
}
```

- **`-Amapstruct.defaultComponentModel=spring`**: Ensures MapStruct generates Spring-managed components
- **`release = 21`**: Compiles with Java 21 class file format, compatible with Java 25 runtime

### 3.3 Quality Gates (Enforced on Every Module)

```kotlin
configure<com.github.spotbugs.snom.SpotBugsExtension> {
    toolVersion = "4.9.0"
    ignoreFailures = false          // FAIL BUILD on violations
    showStackTraces = true
    excludeFilter = file("$rootDir/config/spotbugs/exclude.xml")
}

configure<PmdExtension> {
    toolVersion = "6.55.0"
}

configure<CheckstyleExtension> {
    toolVersion = "10.17.0"
    configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
}

tasks.named("check") {
    dependsOn("spotbugsMain", "pmdMain", "checkstyleMain")
}
```

**Result**: `./gradlew check` fails the build on ANY static analysis violation across ALL 15 modules.

### 3.4 BootJar Control (Critical for Modular Architecture)

```kotlin
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = project.name == "quietspace-app"
}
```

**Why this matters**: 
- Only `quietspace-app` produces an executable JAR
- Core and Domain modules produce **library JARs** (`java-library` plugin)
- Prevents accidental executable JAR creation from library modules
- Enables future microservice extraction (each domain module is already a proper library JAR)

### 3.5 Common Configuration

```kotlin
allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    group = "dev.thural.quietspace"
    version = "0.0.1-SNAPSHOT"
    
    repositories {
        mavenCentral()
    }
}
```

---

## 4. buildSrc — Build Logic as Code

The `buildSrc` directory contains **build logic as maintainable Kotlin code**, not Groovy scripts.

### 4.1 Convention Plugin: `quietspace.domain-conventions.gradle.kts`

```kotlin
// buildSrc/src/main/kotlin/quietspace.domain-conventions.gradle.kts
plugins {
    java
    `java-library`
}

dependencies {
    // Mechanical deps every domain module needs
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-actuator")  // exported
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("org.projectlombok:lombok:1.18.46")

    annotationProcessor("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Mandatory test gates for ALL phases
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Testcontainers baseline (MySQL)
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:mysql:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testRuntimeOnly("com.h2database:h2")  // H2 for @DataJpaTest slices
}
```

**What this achieves**:
1. **Single source of truth** for all domain module dependencies
2. **Enforced test gates**: ArchUnit, JUnit 5, Spring Test, Testcontainers on EVERY domain module
3. **Consistent dependency versions** managed centrally
4. **H2 for slice tests**: `@DataJpaTest` runs on H2 with MySQL compatibility mode
4. **`api` vs `implementation`**: Actuator exported via `api` so consumers get it transitively

### 4.2 Applying the Convention

Each domain module's `build.gradle.kts` is minimal:

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

    testImplementation(project(":core:core-web"))  // slice test support
}
```

**Module declares WHAT it depends on; convention plugin handles HOW to build.**

---

## 5. Gradle Properties — Build Performance & JVM Tuning

```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.jvmargs=-XX:MaxMetaspaceSize=384m -XX:+HeapDumpOnOutOfMemoryError -Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en
```

| Property | Purpose |
|----------|---------|
| `org.gradle.daemon=true` | Reuses Gradle daemon across builds |
| `org.gradle.parallel=true` | Parallel project execution (15 modules benefit greatly) |
| `org.gradle.caching=true` | Build output caching (reuses task outputs) |
| `org.gradle.configuration-cache=true` | Caches configuration phase (massive speedup) |
| `org.gradle.jvmargs` | Tuned heap for parallel Testcontainers execution |

---

## 6. Architectural Enforcement via Gradle + ArchUnit

### 6.1 Dependency Declaration as Architecture Documentation

```kotlin
// domain-post/build.gradle.kts
dependencies {
    // Core dependencies (ALLOWED)
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    
    // Domain dependencies (EXPLICIT ALLOWLIST)
    implementation(project(":domain:domain-user"))
    implementation(project(":domain:domain-photo"))
    implementation(project(":domain:domain-reaction"))
    implementation(project(":domain:domain-comment"))
    implementation(project(":domain:domain-notification"))
    
    // Test-only cross-module deps (for test fixtures)
    testImplementation(project(":core:core-web"))
}
```

**ArchUnit reads these dependencies** to enforce the same rules at runtime.

### 6.2 ArchUnit Rules (Enforced at Test Time)

Each domain module has `DomainXxxArchitectureRulesTest.java`:

```java
@Test
void has_no_cross_module_repository_dependency() {
    ArchRule rule = noClasses()
            .that().resideInAPackage("dev.thural.quietspace.domain.post..")
            .should().dependOnClassesThat(
                JavaClass.Predicates.simpleNameEndingWith("Repository")
                    .and(JavaClass.Predicates.resideInAPackage("dev.thural.quietspace.domain.."))
                    .and(JavaClass.Predicates.resideOutsideOfPackage("dev.thural.quietspace.domain.post..")))
            .because("DomainPost repositories are module-internal; cross-domain reads go through api query ports");
    rule.check(CLASSES);
}
```

**Gradle + ArchUnit synergy**:
- Gradle declares dependencies → ArchUnit verifies they match architectural intent
- Any undeclared transitive dependency or forbidden import fails the build
- No "accidental" coupling possible

### 6.3 Cross-Module Repository Import Ban

The key ArchUnit rule that replaces package-private repositories:

```java
// In each domain's ArchitectureRulesTest
void has_no_cross_module_repository_dependency() {
    noClasses()
        .that().resideInAPackage("dev.thural.quietspace.domain.post..")
        .should().dependOnClassesThat(
            JavaClass.Predicates.simpleNameEndingWith("Repository")
                .and(resideInAPackage("dev.thural.quietspace.domain.."))
                .and(resideOutsideOfPackage("dev.thural.quietspace.domain.post..")))
        .because("DomainPost repositories are module-internal; cross-domain reads go through api query ports");
```

**Replaces package-private**: Repositories stay `public` (for same-module `adapter/`, `service/`, `api/` access) but ArchUnit forbids cross-module imports.

---

## 7. Test Configuration & Slice Testing

### 7.1 Domain Conventions Provide Test Infrastructure

```kotlin
// In domain-conventions.gradle.kts
testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
testRuntimeOnly("com.h2database:h2")  // H2 for @DataJpaTest
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:mysql:1.21.4")
```

### 7.2 Per-Module Test Resources (H2 Config)

Each domain module has `src/test/resources/application.yml`:

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=user
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

**Critical**: `@AutoConfigureTestDatabase(replace = Replace.NONE)` in tests to use this config instead of Spring Boot's default embedded DB replacement.

### 7.3 Slice Test Annotations

```java
// Repository slice test
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest { ... }

// Web slice test
@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerSliceTest { ... }

// Contract test (pure Mockito, no Spring context)
@ExtendWith(MockitoExtension.class)
class PostQueryAdapterTest { ... }
```

---

## 8. CI/CD Integration

### 8.1 GitHub Actions Workflow (`.github/workflows/ci-backend.yml`)

```yaml
jobs:
  test-core:
    name: Test Core Modules
    runs-on: ubuntu-24.04
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '25', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew :core:core-shared:test :core:core-data:test :core:core-web:test :core:core-security:test :core:core-messaging:test

  test-domain:
    name: Test Domain Modules
    runs-on: ubuntu-24.04
    timeout-minutes: 20
    steps:
      - ...
      - run: ./gradlew :domain:domain-user:test :domain:domain-post:test ...

  test-app:
    name: Test App Module
    runs-on: ubuntu-24.04
    timeout-minutes: 30
    steps:
      - ...
      - run: ./gradlew :app:quietspace-app:test

  build-and-push:
    needs: [test-core, test-domain, test-app]
    if: github.event_name != 'pull_request'
    # Build & push Docker image
```

**Key features**:
- **Sharded test execution**: Core, Domain, App run in parallel
- **Configuration cache**: Enabled via `org.gradle.configuration-cache=true`
- **Build cache**: Enabled via `org.gradle.caching=true`
- **Timeouts**: Prevents hung builds

### 8.2 Path Filtering

```yaml
on:
  push:
    branches: ["staging", "prod"]
    paths:
      - 'core/**'
      - 'domain/**'
      - 'app/**'
      - 'buildSrc/**'
      - 'gradle/**'
      - 'gradle.properties'
      - 'settings.gradle.kts'
      - '*.gradle.kts'
      - 'Dockerfile'
```

Only triggers CI when relevant files change.

---

## 8. H2 Configuration for Slice Tests

Each domain module requires H2 with MySQL compatibility mode:

```yaml
# src/test/resources/application.yml (per domain module)
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=user
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```

**Critical annotations on test classes**:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest { ... }
```

- `NON_KEYWORDS=user`: `user` is reserved in H2
- `MODE=MYSQL`: MySQL-compatible SQL dialect
- `Replace.NONE`: Prevents Spring Boot from replacing the configured DataSource

---

## 9. Test Fixtures & Shared Test Infrastructure

### 8.1 Core-Shared Test Fixtures

```kotlin
// core/core-shared/build.gradle.kts
java {
    testFixtures { enabled = true }
}

dependencies {
    testFixturesImplementation(project(":domain:domain-user"))
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testFixturesImplementation("com.fasterxml.jackson.core:jackson-databind")
    // ...
}
```

### 8.2 Consuming Test Fixtures

```kotlin
// app/quietspace-app/build.gradle.kts
dependencies {
    testImplementation(testFixtures(project(":core:core-shared")))
    // ...
}
```

**Usage in integration tests**:
```java
@Autowired
private IntegrationTestHelper helper;  // From core-shared testFixtures

@BeforeEach
void setUp() {
    helper.cleanDatabase(entityManager);
    jwtToken = helper.registerAndLogin("user@test.com", "password");
}
```

---

## 8. Quality Gates Summary

| Gate | Tool | Command | Failure = Build Failure |
|------|------|---------|------------------------|
| Unit Tests | JUnit 5 | `./gradlew test` | ✅ |
| Coverage ≥80% | JaCoCo | `./gradlew jacocoTestCoverageVerification` | ✅ |
| ArchUnit Rules | ArchUnit | `./gradlew test --tests "*ArchitectureRulesTest*"` | ✅ |
| SpotBugs | SpotBugs | `./gradlew spotbugsMain spotbugsTest` | ✅ |
| PMD | PMD | `./gradlew pmdMain pmdTest` | ✅ |
| Checkstyle | Checkstyle | `./gradlew checkstyleMain checkstyleTest` | ✅ |
| Compile | Java | `./gradlew compileJava compileTestJava` | ✅ |
| **All Gates** | **All** | `./gradlew check test` | ✅ |

---

## 9. Future-Proofing: Microservice Extraction Readiness

The Gradle setup ensures each domain module is **extraction-ready**:

| Requirement | How Achieved |
|-------------|--------------|
| **Library JAR** | `java-library` plugin + `api`/`implementation` separation |
| **No main class** | Only `app/quietspace-app` has `BootJar` enabled |
| **Own dependencies** | Each module declares its own `implementation(project(...))` |
| **Own tests** | Unit + slice tests in module; integration tests in app |
| **Own Flyway** | Migrations in `core:core-data` or per-domain (future) |
| **Own config** | Profile-specific `application.yml` per module (future) |

**Extraction path**: Copy module directory → add `@SpringBootApplication` → deploy as separate service.

---

## 10. Quick Reference: Common Commands

| Task | Command |
|------|---------|
| Build all | `./gradlew build` |
| Run all tests | `./gradlew test` |
| Run specific module tests | `./gradlew :domain:domain-post:test` |
| Run specific test class | `./gradlew :domain:domain-post:test --tests PostServiceImplTest` |
| Run all quality gates | `./gradlew check` |
| Generate coverage report | `./gradlew jacocoTestReport` |
| Verify coverage ≥80% | `./gradlew jacocoTestCoverageVerification` |
| Run ArchUnit only | `./gradlew test --tests "*ArchitectureRulesTest*"` |
| Build Docker image | `./gradlew :app:quietspace-app:bootJar` |
| Clean build | `./gradlew clean build` |
| Dependency insight | `./gradlew :domain:domain-post:dependencyInsight --dependency spring-boot-starter-web` |
| Dependency tree | `./gradlew :domain:domain-post:dependencies` |

---

## 11. Summary: How Gradle Enables Modular Architecture

| Architectural Goal | Gradle Mechanism |
|-------------------|------------------|
| **Explicit module boundaries** | `settings.gradle.kts` + `implementation(project(...))` |
| **Core/Domain separation** | `core:*` modules have no `domain:` deps; enforced by ArchUnit |
| **Consumer-owned ports** | Gradle deps + ArchUnit `has_no_cross_module_repository_dependency` |
| **Quality gates** | `check` task = SpotBugs + PMD + Checkstyle + Tests + Coverage |
| **BootJar isolation** | `bootJar.enabled = project.name == "quietspace-app"` |
| **Shared conventions** | `buildSrc` convention plugin (`quietspace.domain-conventions`) |
| **Test isolation** | Domain = unit/slice only; App = integration only |
| **ArchUnit enforcement** | Per-module test suites + Gradle test execution |
| **Build performance** | Configuration cache + build cache + parallel execution |
| **Extraction readiness** | `java-library` plugin, no main class, explicit deps |

The Gradle build system is not just a build tool—it **is** the architectural enforcement layer. Every architectural rule has a corresponding Gradle or ArchUnit check that fails the build on violation.
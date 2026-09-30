# Rule 05 — Build & Quality-Gate Rules

> Extracted from: `docs/build/gradle-modular-build-system.md`, `modular-architecture-overview.md` §3, `modular-architecture-review.md` §4.

## 1. Module declaration

- All 15 modules explicitly listed in `settings.gradle.kts` (`core:*`, `domain:*`, `app:quietspace-app`, `buildSrc`). New module = new `include("domain:domain-xxx")` (or `core:...`).
- Root project `quietspace-platform`; group `dev.thural.quietspace`; Java 21 release (JDK 25 runtime); MapStruct `defaultComponentModel=spring`.

## 2. Convention plugins (declare WHAT, not HOW)

- Domain modules apply `quietspace.domain-conventions` and declare **only semantic deps**: `implementation(project(":core:..."))` + explicit `implementation(project(":domain:..."))` allowlist (+ `testImplementation` for fixtures/slice support). Mechanical deps (Spring starters, MapStruct, Lombok, ArchUnit, Testcontainers, H2 runtime) come from the plugin — do not redeclare versions.
- Core changes (new util/interface) must keep zero domain deps and export via `api` (not `implementation`) when domains need it.

## 3. BootJar control (extraction readiness)

- **Only `quietspace-app` produces an executable JAR** (`bootJar.enabled = project.name == "quietspace-app"`). Core/domain modules are `java-library` JARs with no main class. Never enable `bootJar` elsewhere.

## 4. Gates — all must pass (`./gradlew check test`)

| Gate | Command scope |
|------|---------------|
| Unit/slice/contract/ArchUnit tests | `./gradlew test` (per module or all) |
| Integration tests | `./gradlew integrationTest` / `./gradlew check` (`*IT` naming required) |
| Coverage ≥80% | `jacocoTestCoverageVerification` per module |
| ArchUnit | `test --tests "*ArchitectureRulesTest*"` per module |
| SpotBugs / PMD / Checkstyle | `spotbugsMain`, `pmdMain`, `checkstyleMain` (fail on violation; `check` depends on them) |

## 5. CI expectations

- Sharded jobs `test-core` / `test-domain` / `test-app` then `build-and-push`; path filter on `core/**`, `domain/**`, `app/**`, `buildSrc/**`, `gradle/**`; build + configuration cache enabled; per-job timeouts. Keep builds cache-friendly (no absolute paths, no undeclared inputs).
- Concurrency baseline (ADR 002): `spring.threads.virtual.enabled=true`; HikariCP `maxPoolSize` ≈ CPU × 2; replace `synchronized` with `ReentrantLock` in hot paths (pinning); monitor JFR `jdk.VirtualThreadPinned`.
- Migrations ship via Flyway in `app` (`V{next}__description.sql`, idempotent SQL); Flyway disabled in slice/TC profiles (`ddl-auto: create-drop`).

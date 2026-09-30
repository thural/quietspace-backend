# Rule 02 — Runtime & Persistence Rules

> Extracted from: `system-architecture-overview.md` §1/§6, `configuration.md`, `usage-guide.md`, ADR 002, `testing.md` (H2 rules), migration guide (Flyway).

## 1. Stack (do not drift without an ADR)

- Java 21 target / JDK 25 runtime (Gradle toolchain), Spring Boot 4.1.0, Spring Data JPA/Hibernate, Spring Security, Spring WebSocket (STOMP), Spring AMQP, MySQL 8 prod, RabbitMQ, OpenAPI/springdoc (`core-web`), SpringWolf STOMP docs (`core-messaging`, docket YAML in app config).
- Build: Gradle 8.14+ wrapper, Kotlin DSL, `buildSrc` conventions; CI JDK 25 (Temurin/Corretto per workflow); Docker runtime `eclipse-temurin:25-jre-alpine`, `linux/amd64`.

## 2. Virtual threads (ADR 002)

- `spring.threads.virtual.enabled=true` stays on (Tomcat + `@Async` `VirtualThreadPerTaskExecutor` + STOMP executors).
- HikariCP `maximum-pool-size ≈ CPU cores × 2` (prod `5` baseline in `application-prod.yml` — retune deliberately, not casually).
- **No `synchronized` in hot paths** — use `ReentrantLock`. Monitor JFR `jdk.VirtualThreadPinned`; alert on pinning (see Rule 06).

## 3. Persistence & migrations

- Prod: `jpa.hibernate.ddl-auto: validate` + **Flyway enabled** (`baseline-on-migrate: true`). Dev: `ddl-auto: update` + Flyway disabled. Test slices: H2 `create-drop`; app ITs: Testcontainers MySQL with `create-drop`, Flyway disabled.
- Migrations live **centralized in `app/.../db/migration`** as `V{next}__description.sql`, idempotent SQL (`IF NOT EXISTS`, `ON CONFLICT`). No module-level migration ownership (IAM-plan decision — avoids version conflicts).
- Schema ownership: each domain owns its tables conceptually (separate schemas per domain on extraction path); cross-domain queries via ports/events/local read models, never cross-schema joins. Entities extend `BaseEntity` with UUID ids + audit fields; JPA auditing enabled in `core-data`.
- H2 slice rules (all three, always): (1) per-module `src/test/resources/application.yml` with `jdbc:h2:mem:testdb;MODE=MYSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=user`; (2) `@AutoConfigureTestDatabase(Replace.NONE)`; (3) H2 via domain-conventions plugin only. Fixtures: set FK ids after `save()`; dual-mapped columns set both sides; `User` rows mirror `UserServiceIT` minimal shape.

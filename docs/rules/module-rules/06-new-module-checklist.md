# Checklist 06 — New Module & Refactor Checklist

> **Start here.** Work through top to bottom for a new module; use Part B for refactors.
> Each item maps to a rule file: [01](01-dependency-rules.md) [02](02-module-structure.md) [03](03-communication-rules.md) [04](04-testing-rules.md) [05](05-build-quality-gates.md).

## Part A — New module (copy/paste, check off)

### A1. Scope & dependencies [01]
- [ ] Single aggregate/bounded context defined; module role (leaf/sink/standard) decided.
- [ ] Dependency edges listed; DAG verified (no cycles, esp. `user↔photo` trap).
- [ ] `settings.gradle.kts`: `include("domain:domain-xxx")` added.
- [ ] `build.gradle.kts`: applies `quietspace.domain-conventions`, declares only semantic `implementation(project(...))` deps + `testImplementation` fixtures.

### A2. Structure [02]
- [ ] Flat-root layout with `api/`, `adapter/` or `port/`, `controller/`, `dto/`, `health/` as needed.
- [ ] Aggregate: `extends BaseEntity`, `@SuperBuilder`, factory + behavior methods, no public setters.
- [ ] Repository extends `BaseRepository`; same-module use only.
- [ ] Controller thin (no repository injection); service owns `@Transactional`.
- [ ] Cross-aggregate refs are `UUID` FK columns, not JPA associations. Response DTOs extend `BaseResponse` via hand-written mappers.

### A3. Communication [03]
- [ ] Sync reads: new or reused `api` query port + record DTO + `*QueryAdapter` (+ contract test).
- [ ] Sync commands: consumer-owned `port/` interface (in consumer) + provider `adapter/`.
- [ ] Async: event class in `core-shared/event/` + `EventSerializerTest` roundtrip; publish via `TransactionalEventPublisher` in-TX; consumer `@EventListener` idempotent via `processed_events`.
- [ ] `domain-module-contracts.md` + `events.md` registry entries updated.

### A4. Tests [04]
- [ ] Unit (Mockito-only), web slice (`@WebMvcTest`, filters off, `JwtFilter` deps mocked), repo slice (`@DataJpaTest` + `Replace.NONE` + H2 config), contract, ArchUnit (real packages + repo-import ban) tests added.
- [ ] Integration flows added in `app` (`*IT.java`), not in the domain module.
- [ ] Coverage ≥80%; FIRST/AAA/naming rules followed.

### A5. Gates & wiring [05]
- [ ] `bootJar` stays disabled; app wires `@EntityScan`/`@EnableJpaRepositories`/component scan.
- [ ] `./gradlew :domain:domain-xxx:check :domain:domain-xxx:jacocoTestCoverageVerification` green; full `./gradlew check` green.

## Part B — Refactor existing module (divergence sweep)

- [ ] **Imports:** no new cross-module internals or `*Repository` imports outside own module [01]. Violations rerouted to query/consumer ports.
- [ ] **Associations:** no new cross-aggregate JPA associations; legacy ones migrated to FK-ID + port where touched [02 §5].
- [ ] **Controllers:** no repository injection; logic pushed to service/domain [02 §4].
- [ ] **Ports/events:** sync needs use existing ports first; async needs use existing events first; new contracts registered in docs [03].
- [ ] **Core:** no domain references introduced; IAM stays in `domain-user`; shared ports stay in `core-shared` [01 §4].
- [ ] **Tests:** moved to correct layer/module; app holds no unit/slice tests, domains hold no `*FlowIT` [04].
- [ ] **Build:** no undeclared deps, no `bootJar` change, all gates green [05].

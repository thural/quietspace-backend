# Rule 01 — System Design Principles

> Extracted from: `system-architecture-review.md`, `system-architecture-overview.md` §1–2/§9, `plans/extract-domain-iam-module.md`.

## 1. Composition root

- `app/quietspace-app` is the **sole composition root**: the only `@SpringBootApplication`, the only executable `bootJar`, the only host of full-context ITs, Flyway migrations, and `application*.yml`.
- Domain/core modules are `java-library` JARs: no `main` class, no server port, no datasource URL, no broker wiring. They expose ports/events; the app wires them.
- New module wiring = add `implementation(project(":domain:..."))` in `app/build.gradle.kts` + entity/repo scan coverage. Never component-scan legacy packages after a move — import the new module config (cf. `IamModuleConfig` precedent).

## 2. SOLID at macro level

- **SRP:** one bounded context per domain module; one technical concern per core module. New feature goes in exactly one owner module; shared behavior goes behind a port, not a util dump.
- **OCP:** extend by implementing a port or subscribing to an event — e.g. adding reactions = implement port + subscribe to `PostCreatedEvent`, zero edits to `domain-post`. If your feature requires editing 3+ modules' internals, the contract is wrong — define a port/event instead.
- **LSP:** depend on port abstractions so adapters are swappable (repo backend, `EventBus`, WS transport). Test doubles substitute via the same interface.
- **ISP:** small ports with only the methods the consumer needs — no god interfaces. New port method must be justified by an actual consumer call site.
- **DIP:** consumer owns the interface; provider adapts. Core exposes SPIs (`AuthenticationProvider`, `JwtTokenService`, `WebSocketUserPort`, `CurrentUserPort`); domains implement. Zero compile-time core→domain edge.

## 3. Hexagonal discipline

- Domain logic never imports adapter/transport packages (JPA impl details, AMQP, STOMP, servlet). Ports sit at the boundary; adapters live outside the domain core.
- `@Component`/`@Service` only on port implementations, mappers, listeners — never on domain entities/value objects.

## 4. Extraction & transport-swap readiness

- Every feature must keep these swaps configuration/wiring-only: RabbitMQ→Kafka (bootstrap wiring), WebSocket→SSE/long-poll (port impl), read replicas (core-data config), new frontend (CORS/URL config).
- Signals of divergence: transport class imported in a domain service; broker/queue name hardcoded outside `core-messaging`/config; SQL join reaching into another domain's tables (use ports/events/visibility tables instead — ADR 005).
- IAM precedent (`extract-domain-iam` plan): splitting a module = package-isolate → port-isolate (`UserCredentialsPort`) → extract module → wire in app → move tests. Follow that sequence; keep Flyway centralized in `app` during splits.

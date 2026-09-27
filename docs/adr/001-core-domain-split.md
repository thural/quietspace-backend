# ADR 001: Core/Domain Split Architecture

## Status
Accepted

## Context
The QuietSpace application was initially built as a monolith with all code in a single module. As the codebase grew, we identified architectural issues:
- Controllers directly accessing repositories (bypassing service layer)
- Cross-feature repository access (e.g., NotificationService accessing PostRepository, CommentRepository)
- Domain-specific enums in shared package creating unnecessary coupling
- No clear boundaries between features

We need a clear architectural structure that enforces separation of concerns and enables future modularization.

## Decision
We adopt a **Core/Domain split architecture** with the following structure:

```
src/main/java/dev/thural/quietspace/
├── core/           (future: core-* modules)
│   ├── shared/     (domain-agnostic utilities, exceptions, base classes)
│   ├── data/       (JPA auditing, base repositories)
│   ├── web/        (exception handlers, base controllers, OpenAPI)
│   ├── security/   (JWT, filters, authentication interfaces)
│   └── messaging/  (RabbitMQ, email, WebSocket config)
├── domain/         (future: domain-* modules)
│   ├── auth/
│   ├── user/
│   ├── post/
│   ├── chat/
│   ├── message/
│   ├── notification/
│   ├── reaction/
│   ├── comment/
│   └── photo/
└── shared/         (current: domain-agnostic code, to be moved to core/shared)
```

### Rules enforced by ArchUnit:
1. **No core → domain dependencies**: Core modules must not depend on domain modules
2. **No domain → domain internal access**: Domain modules must not access other domain modules' internal classes directly (only via published interfaces/events)
3. **No cross-module repository imports**: Repository interfaces are module-internal; cross-domain reads go through query ports (ArchUnit `has_no_cross_module_repository_dependency`)
4. **Controllers must not access repositories directly**: Must go through service layer

## Consequences
### Positive
- Clear architectural boundaries
- Enables future multi-module extraction (Phase 6)
- Prevents architectural decay
- ArchUnit tests provide automated governance

### Negative
- Requires refactoring existing code (Phase 1-3)
- Some cross-feature queries need Domain Events + Outbox pattern (Phase 4)
- Package-private repositories superseded by ArchUnit cross-module import ban (rule 3)

### Neutral
- Incremental adoption: rules can be enabled as code is fixed

## Alternatives Considered
- **Layered architecture (controller/service/repository)**: Too flat, doesn't prevent cross-feature access
- **Modular monolith with Spring Modulith**: Good alternative, but Core/Domain split gives more explicit control
- **Full microservices**: Too much operational complexity for current scale

## Related ADRs
- ADR 002: Virtual Threads for Concurrency
- ADR 003: Transactional Outbox for Domain Events

## References
- Architecture Improvement Plan (docs/plans/architecture-improvement-plan.md)
- ArchUnit test suite (src/test/java/dev/thural/quietspace/shared/test/archunit/)
# ADR 004: IAM Bounded Context in domain-user; Infrastructure Ports in core-shared

## Status
Accepted

## Context
Identity and Access Management (authentication, user lifecycle, profile state) was
at risk of leaking into core modules: `core-security` owns the JWT filter chain and
`core-messaging` authenticates WebSocket sessions, both of which need user data.
A God Entity or `AuthService` duplicate in core would violate the core/domain split
(ADR 001) and create cyclic module dependencies.

Verified current state (no migration needed):
- Single `User` entity in `domain-user`; single `AuthService` in
  `domain-user/.../auth/`; no `User` entity and no `AuthService` duplicate in core.
- `core-messaging/.../model/UserRepresentation.java` is a validation DTO, not an entity.
- `domain-photo` (declared leaf) reaches user state via `core-shared/ports/UserProfilePort`,
  implemented by `domain-user/.../adapter/UserProfileAdapter`.

## Decision
1. **IAM is a single bounded context owned by `domain-user`**: `User`, `ProfileSettings`,
   `AuthService`, `UserAuthenticationProvider`, and `UserQueryPort` (display-data queries).
2. **Core stays domain-agnostic**: zero references to the `User` entity or
   `domain.user.auth` from any `core..` package — enforced by ArchUnit rules
   `no_core_user_entity_dependency` and `no_core_auth_dependency` in `ArchitectureRulesTest`.
3. **Outbound SPI ports stay in `core-shared`**: `UserProfilePort` and `WebSocketUserPort`
   are infrastructure contracts consumed by leaf/infra modules. Moving `UserProfilePort`
   to `domain-user.api` would force a `photo→user` edge; with the existing `user→photo`
   edge that is a Gradle dependency cycle, so the move is rejected.
4. Other domains read user display data via `UserQueryPort` or domain events — never via
   direct database lookups or the `User` entity association for *new* code (legacy
   `@ManyToOne User` associations are tracked for removal in the JPA-decoupling plan).

## Consequences
- `domain-photo` keeps zero domain dependencies while retaining upload-flow user access.
- Auth behavior changes are contained in `domain-user`; core-security only sees the
  `AuthenticationProvider`/`JwtTokenService` abstractions.

# Rule 03 — Security Rules

> Extracted from: `core-module-apis.md` (core-security), `system-architecture-overview.md` §7, integration-test guidelines §4.1, `configuration.md` (secrets), ADR 004.

## 1. Separation of auth concerns

- `core-security`: JWT filter chain, `JwtTokenService(Impl)` (HS256 + issuer/audience, refresh tokens), `AuthenticationProvider` SPI, `SecurityConfig` (stateless API, CSRF disabled), `SecurityBeans` (`PasswordEncoder`, `AuthenticationManager`).
- `domain-user` (IAM): `User`, `AuthService`, `UserAuthenticationProvider` (backed by `UserRepository`), credential checks. No JWT/filter code in domains; no `User`/credential concepts in core.
- Authorization decisions live in domain services (e.g. `PostSecurityService.canAccess` via `UserQueryPort`), enforced at endpoints with `@PreAuthorize("@postSecurity.canAccess(#postId, authentication.name)")` — never direct `UserRepository` lookups from security expressions.

## 2. Endpoint & WebSocket hardening

- New endpoints default to authenticated; explicitly permit only public ones (auth, health/docs per policy). Method-security over URL-matcher sprawl for object-level checks.
- WebSocket: `WebSocketSecurityConfig` (CSRF disabled for handshake, CONNECT permit, `/user` prefix); STOMP auth via JWT in headers resolved through `WebSocketUserPort`. Frontend origins via `application.urls.frontend` (`FRONTEND_HOST`/`FRONTEND_PORT`) — keep CORS config and `.env` in sync.
- Global errors via `core-web` `GlobalExceptionHandler` (`ProblemDetail` + `ErrorResponse`); never leak stack traces or credential/DB details to clients.

## 3. Secrets & JWT

- `JWT_SECRET_KEY` (base64 HMAC), `ADMIN_PASSWORD`, DB creds: required env/secrets, **never committed** (`.env` gitignored; K8s secrets out of repo; rotate JWT key periodically). Prod `.env` generated from GitHub Secrets on deploy.
- JWT validation includes issuer/audience; tokens issued only via auth flow (`registerAndLogin` pattern in tests mirrors prod).

## 4. Security testing (mandatory placement)

- Security-posture tests live in **`app` with the wired filter chain** (`PostControllerSecurityTest`, `CorsSecurityTest`, `CsrfTest`, `SecurityHeadersTest`, `SecurityFlowIT`): unauthenticated → 401. A `@WebMvcTest` slice without `core-security` returns 200/404 instead of 401 — so slice tests must use `addFilters=false` + mocked services, and must NOT be mistaken for security coverage.

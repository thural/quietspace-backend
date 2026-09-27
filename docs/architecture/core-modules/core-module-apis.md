# Core Module APIs

## Overview
Core modules provide shared infrastructure and cross-cutting concerns. They have **zero dependencies on domain modules** and are designed for reuse across multiple applications.

---

## core-shared
**Package**: `dev.thural.quietspace.core.shared`

### Public Interfaces
| Interface | Purpose |
|-----------|---------|
| `EmailService` | Abstract email sending (SMTP/Resend implementations) |
| `CommonService<T, ID>` | Generic CRUD operations |

### Utilities
| Class | Purpose |
|-------|---------|
| `BaseEntity` | JPA mapped superclass with UUID id, audit fields, optimistic locking |
| `BaseResponse` | Standardized API response envelope |
| `ErrorResponse` | Error detail record (timestamp, status, error, message, path) |
| `PagingProvider` | Pagination helper with sensible defaults (page=0, size=25, sort=createdAt desc) |
| `PageUtils` | Static utility for list-to-page conversion |
| `ImageCompressionUtil` | Thumbnailator-based image compression |
| `OffsetDateTimeProvider` | Spring Data auditing DateTimeProvider implementation |

### Enums (Truly Shared)
| Enum | Values |
|------|--------|
| `Role` | USER, ADMIN |
| `Permission` | ADMIN_*, USER_* (8 permissions) |
| `StatusType` | ONLINE, OFFLINE |
| `EmailTemplateName` | ACTIVATE_ACCOUNT |

### Exceptions (Infrastructure)
| Exception | Extends |
|-----------|---------|
| `CustomErrorException` | RuntimeException (with HttpStatus + data) |
| `CustomDataNotFoundException` | RuntimeException |
| `CustomMessagingException` | MessagingException |
| `CustomParameterConstraintException` | RuntimeException |
| `CustomSocketException` | SocketException |
| `ImageUploadException` | RuntimeException |
| `OperationNotPermittedException` | RuntimeException |
| `UnauthorizedException` | RuntimeException |
| `UnsupportedImageTypeException` | RuntimeException |
| `UserNotFoundException` | RuntimeException |
| `ActivationTokenException` | RuntimeException |

### Domain Events (Event Contracts)
| Event | Aggregate | Payload |
|-------|-----------|---------|
| `UserRegisteredEvent` | User | userId, username, email |
| `PostCreatedEvent` | Post | postId, authorId, title, text |
| `CommentCreatedEvent` | Comment | commentId, postId, authorId, text |
| `ReactionAddedEvent` | Reaction | reactionId, contentId, contentType, reactionType, actorId |
| `MessageSentEvent` | Message | messageId, chatId, senderId, text |
| `UserFollowedEvent` | User | followerId, followedId |
| `UserUnfollowedEvent` | User | followerId, followedId |
| `UserPrivacyChangedEvent` | User | userId, isPrivate |
| `EmailEvent` | Email | to, subject, templateName, variables |

### Event Infrastructure
| Class | Purpose |
|-------|---------|
| `DomainEvent` | Abstract base (eventId, timestamp, aggregateType, aggregateId, eventType) |
| `EventSerializer` | Jackson-based JSON serialization |
| `TransactionalEventPublisher` | Persists events to outbox within same transaction |
| `OutboxEvent` | JPA entity for outbox table |
| `OutboxEventRepository` | Spring Data repository for outbox polling |
| `ProcessedEvent` | Idempotency tracking entity |
| `ProcessedEventRepository` | Spring Data repository for deduplication |

### Shared Ports (SPI - Outbound)
| Port | Provider | Consumers | Rationale |
|------|----------|-----------|-----------|
| `UserProfilePort` | `domain-user/.../adapter/UserProfileAdapter` | `domain-photo/PhotoServiceImpl` | Photo is a leaf; moving it would force `photo→user` cycle |
| `WebSocketUserPort` | `domain-user/.../adapter/WebSocketUserAdapter` | `core-messaging` WebSocket auth | Core-driven outbound SPI |

---

## core-data
**Package**: `dev.thural.quietspace.core.data`

### Public Interfaces
| Interface | Purpose |
|-----------|---------|
| `BaseRepository<T, ID>` | Extended JpaRepository with common queries |

### Configuration
- JPA auditing enabled via `@EnableJpaAuditing`
- Hibernate UUID generator
- HikariCP connection pool (tuned for virtual threads)

---

## core-web
**Package**: `dev.thural.quietspace.core.web`

### Public Interfaces
| Interface | Purpose |
|-----------|---------|
| `GlobalExceptionHandler` | Standardized error responses (ProblemDetail + ErrorResponse) |
| `HealthController` | `/actuator/health` endpoint |

### OpenAPI/Swagger
- SpringDoc OpenAPI 3 configuration
- Bearer token security scheme
- Actuator endpoints documented

### Base Controllers
| Class | Purpose |
|-------|---------|
| `BaseController` | Common CRUD endpoint patterns |

---

## core-security
**Package**: `dev.thural.quietspace.core.security`

### Public Interfaces
| Interface | Purpose |
|-----------|---------|
| `AuthenticationProvider` | `UserDetails loadUserByUsername(String)` — implemented by domain-user |
| `JwtTokenService` | `generateToken(UserDetails)`, `validateToken(String)` |

### Implementations
| Class | Purpose |
|-------|---------|
| `JwtTokenServiceImpl` | HS256 JWT with issuer/audience validation, refresh tokens |
| `JwtFilter` | Servlet filter for JWT authentication |
| `SecurityConfig` | Spring Security filter chain, CSRF disabled for stateless API |
| `SecurityBeans` | PasswordEncoder, AuthenticationManager beans |

---

## core-messaging
**Package**: `dev.thural.quietspace.core.messaging`

### Public Interfaces
| Interface | Purpose |
|-----------|---------|
| `WebSocketUserPort` | `UserRepresentation getUser(UUID)` — implemented by domain-user |
| `NotificationEventPublisher` | `void publish(NotificationEvent)` — STOMP via RabbitMQ relay |

### Configuration
| Class | Purpose |
|-------|---------|
| `WebSocketConfig` | STOMP over WebSocket, RabbitMQ broker relay (`/topic`, `/queue`) |
| `WebSocketSecurityConfig` | CSRF disabled, CONNECT permit, user destination prefix `/user` |
| `RabbitMQConfig` | Topic exchange `domain.events`, email exchange/queue |
| `PostVisibilityProjector` | Maintains feed-visibility read model from user lifecycle events (see ADR 005) |
| `RabbitMQConfig` | Topic exchange `domain.events`, email exchange/queue |
| `EmailEventPublisher` | Publishes `EmailEvent` to RabbitMQ |
| `EmailEventConsumer` | Consumes from `email.queue`, delegates to `EmailService` |

### Models
| Class | Purpose |
|-------|---------|
| `UserRepresentation` | Minimal user DTO for cross-module (id, username, email, status) |

---

## Extension Points

### Adding a New Core Module
1. Create module under `core/` with `build.gradle.kts`
2. Add `testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")`
3. Create `*ArchitectureRulesTest.java` enforcing no domain dependencies
4. Add to `settings.gradle.kts`
5. Wire in `app/quietspace-app` if needed

### Adding a New Domain Event
1. Create event class in `core-shared` extending `DomainEvent`
2. Add serialization test in `core-shared:EventSerializerTest`
3. Publisher: inject `TransactionalEventPublisher` and call `publish(event)`
4. Consumer: implement `@EventListener` in domain module with idempotency check
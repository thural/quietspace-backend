# QuietSpace Backend

## Project Overview

QuietSpace is a privacy-focused social media platform built as a **Modular Monolith** using Spring Boot 4, Java 25, and Domain-Driven Design (DDD) with Hexagonal Architecture principles. The architecture enforces strict module boundaries, consumer-owned ports, and event-driven communication — enabling both operational simplicity and future microservice extraction readiness.

## Architecture

### Modular Monolith (Modulith)

The system is organized into **two primary tiers** with strictly enforced boundaries:

```
quietspace-backend/
├── core/                           # Infrastructure tier (zero domain deps)
│   ├── core-shared/                # Base entities, events, utilities, outbox
│   ├── core-data/                  # JPA config, auditing, repositories, Flyway
│   ├── core-web/                   # Exception handling, OpenAPI, pagination
│   ├── core-security/              # JWT filter chain, auth manager, passwords
│   └── core-messaging/             # Event bus, RabbitMQ, STOMP/WebSocket
│
├── domain/                         # Business logic tier (vertical slices)
│   ├── domain-user/                # IAM, authentication, user lifecycle
│   ├── domain-post/                # Posts, polls, saves, reposts, feeds
│   ├── domain-comment/             # Comments, replies, comment reactions
│   ├── domain-reaction/            # Generic reactions on content
│   ├── domain-chat/                # Chat rooms, membership
│   ├── domain-message/             # Messages, read receipts
│   ├── domain-notification/        # Sink: consumes 8+ event types
│   └── domain-photo/               # Leaf: upload, storage, metadata
│
├── app/
│   └── quietspace-app/             # Only executable module (bootJar)
│
├── buildSrc/                       # Build logic as Kotlin code
│   └── conventions/                # Convention plugins (domain, core, test)
│
└── docs/architecture/              # Architecture documentation
    ├── system-architecture-review.md
    ├── modular-architecture-review.md
    ├── modular-architecture-overview.md
    ├── core-modules/core-module-apis.md
    └── domain-modules/domain-module-contracts.md
```

### Dependency Rules (Enforced)

| Direction | Allowed | Enforcement |
|-----------|---------|-------------|
| Domain → Core | ✅ Yes | Gradle `implementation` |
| Domain → Domain | ❌ No | ArchUnit + Gradle |
| Core → Domain | ❌ No | Gradle (no deps declared) |

## Features

### User Management
- Secure authentication/authorization with Spring Security 7
- Email account activation with token-based verification
- Stateless JWT authentication (HS256, issuer/audience validation)
- Refresh token rotation with blacklist on logout
- Role-based permissions (USER, ADMIN)

### Social Interactions
- Posts with text, polls, images, saves, reposts
- Nested comments and replies
- Generic reactions (like, love, haha, wow, sad, angry) on posts/comments
- Follow/unfollow with privacy controls
- Algorithmic feed with visibility projection

### Real-Time Communication
- STOMP over WebSocket with RabbitMQ broker relay
- JWT-authenticated WebSocket connections
- One-to-one and group chat
- Message delivery/read receipts
- Real-time notifications via STOMP topics

### Media Management
- Profile picture and attachment uploads
- Thumbnailator-based compression and resizing
- Database-backed storage with efficient DTO representation

### Observability & Quality
- Structured logging, metrics (Micrometer), health endpoints
- Comprehensive test pyramid: unit, slice, integration, ArchUnit
- Static analysis: SpotBugs, PMD, Checkstyle (fail build on violation)
- API docs: OpenAPI 3 (springdoc), AsyncAPI (Springwolf)

## Technology Stack

| Category | Technology |
|----------|------------|
| **Framework** | Spring Boot 4.1.0 |
| **Language** | Java 25 |
| **Build** | Gradle 9.6.1 (Kotlin DSL, convention plugins) |
| **Security** | Spring Security 7, JJWT 0.13.0 |
| **Database** | MySQL 8, JPA/Hibernate 7.4, Flyway |
| **Messaging** | RabbitMQ, STOMP/WebSocket |
| **Serialization** | Jackson 3.x (records, modules) |
| **Mapping** | MapStruct 1.6.3 |
| **Testing** | JUnit 5, Mockito, Testcontainers, ArchUnit |
| **Docs** | springdoc OpenAPI 3, Springwolf AsyncAPI |
| **Containerization** | Docker, Docker Compose |

## Inter-Module Communication

### Consumer-Owned Ports (Synchronous)
Consumer defines the interface; provider implements it.

```
domain-notification (consumer)          domain-post (provider)
├── NotificationPostPort  ◄────── implements ────►  PostNotificationAdapter
```

### Domain Events (Asynchronous)
All events in `core-shared`, published via **Transactional Outbox** → RabbitMQ → idempotent consumers.

| Event | Publisher | Consumers |
|-------|-----------|-----------|
| `UserRegisteredEvent` | domain-user | domain-notification |
| `PostCreatedEvent` | domain-post | domain-notification |
| `CommentCreatedEvent` | domain-comment | domain-notification |
| `ReactionAddedEvent` | domain-reaction | domain-notification |
| `MessageSentEvent` | domain-message | domain-notification, domain-chat |
| `UserFollowedEvent` | domain-user | domain-notification, domain-post (projector) |

## Getting Started

### Prerequisites
- Java 25+
- Gradle 9.x (wrapper included)
- MySQL 8+
- Docker (for RabbitMQ, MySQL)

### Quick Setup

```bash
# 1. Clone
git clone https://github.com/thural/quietspace-backend.git
cd quietspace-backend

# 2. Configure environment
cp .env.example .env  # Edit with your values

# 3. Start infrastructure
docker compose -f infrastructure/docker/docker-compose.yml up -d

# 4. Build and run
./gradlew build
./gradlew :app:quietspace-app:bootRun
```

### Environment Variables (.env)

```bash
ACTIVE_PROFILE=dev
DB_PORT_NUMBER=3306
SERVER_PORT_NUMBER=8080
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION=86400000
JWT_EXPIRATION_REFRESH=604800000
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
```

## API Documentation

| Interface | UI | Spec |
|-----------|-----|------|
| **REST (OpenAPI 3)** | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| **WebSocket/STOMP (AsyncAPI)** | `http://localhost:8080/springwolf/asyncapi-ui.html` | `http://localhost:8080/springwolf/docs` |

## Development

### Running Tests

```bash
# All tests (unit + integration + ArchUnit)
./gradlew test

# Specific module
./gradlew :domain:domain-user:test

# Integration tests only (requires Testcontainers)
./gradlew :app:quietspace-app:integrationTest
```

### Code Quality

```bash
# Static analysis (SpotBugs, PMD, Checkstyle)
./gradlew check

# Format check
./gradlew spotlessCheck

# Architecture rules (ArchUnit)
./gradlew :domain:domain-user:test --tests "*ArchitectureRulesTest"
```

### Adding a New Domain Module

See [`docs/guides/migration/adding-domain-module.md`](docs/guides/migration/adding-domain-module.md) for the complete checklist.

## Documentation

| Document | Description |
|----------|-------------|
| `docs/architecture/system-architecture-review.md` | SOLID adherence, modularity, encapsulation, industry alignment |
| `docs/architecture/modular-architecture-review.md` | Module structure, communication patterns, Gradle enforcement |
| `docs/architecture/modular-architecture-overview.md` | High-level architecture overview |
| `docs/architecture/core-modules/core-module-apis.md` | Core module public APIs and SPIs |
| `docs/architecture/domain-modules/domain-module-contracts.md` | Domain module contracts, ports, events, migrations |

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/xyz`)
3. Ensure all quality gates pass: `./gradlew check test`
4. Commit changes with conventional messages
5. Push and create a pull request

## License

This project is licensed under a **Proprietary License**. All rights reserved. See [LICENSE.md](./LICENSE.md).
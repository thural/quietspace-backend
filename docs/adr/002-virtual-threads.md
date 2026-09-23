# ADR 002: Virtual Threads (Project Loom) for Concurrency

## Status
Accepted

## Context
The application handles WebSocket connections (STOMP over SockJS) and HTTP requests. Traditional platform threads (thread-per-request) limit scalability:
- Each WebSocket connection holds a platform thread
- Thread pool exhaustion under high concurrency
- Blocking I/O operations waste thread resources
- Tomcat's default thread pool (200 threads) limits concurrent connections

Java 25 (Project Loom) introduces **Virtual Threads** - lightweight threads managed by the JVM that enable massive concurrency without WebFlux/reactive complexity.

## Decision
Enable Virtual Threads across the application:

1. **Tomcat HTTP/WebSocket**: `spring.threads.virtual.enabled=true` in application.yml
2. **@Async processing**: Configure `VirtualThreadPerTaskExecutor` in AsyncConfig
3. **WebSocket STOMP inbound/outbound**: Use virtual thread executors (Phase 5.3)
4. **Connection pool tuning**: HikariCP `maxPoolSize` ≈ CPU cores × 2 for virtual thread compatibility

## Consequences
### Positive
- Massive concurrency: millions of WebSocket/HTTP connections possible
- Simpler code: no reactive programming required
- Better resource utilization: virtual threads park during blocking I/O
- Scales elastically: no thread pool sizing needed

### Negative
- **Thread pinning risk**: Virtual threads pinned when entering `synchronized` blocks or native calls
- Requires monitoring: JFR `jdk.VirtualThreadPinned` events
- Some libraries may not be virtual-thread-friendly (older JDBC drivers, etc.)
- HikariCP tuning required

### Neutral
- Migration is configuration-only for most Spring Boot components
- Existing `@Async` and `@Transactional` code works unchanged

## Alternatives Considered
- **WebFlux/Reactive**: Requires rewriting all blocking code, steep learning curve
- **Traditional thread pools**: Limited scalability, thread pool tuning complexity
- **Loom + reactive hybrid**: Unnecessary complexity

## Related ADRs
- ADR 001: Core/Domain Split Architecture
- ADR 003: Transactional Outbox for Domain Events

## References
- Spring Boot 3.2+ Virtual Threads documentation
- Java 25 Project Loom JEP 444
- Architecture Improvement Plan Phase 0.3
- Pinning detection setup (JFR monitoring)
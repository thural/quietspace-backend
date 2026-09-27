# Observability Enhancements

## Distributed Tracing

### Configuration
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (adjust for production)
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}
```

### Trace Context Propagation
- **Outbox → RabbitMQ**: Trace headers injected via `MessageConverter`
- **RabbitMQ → Consumer**: Trace headers extracted, new span created as child
- **STOMP/WebSocket**: Trace context propagated via STOMP headers

### Dependencies
```kotlin
implementation("io.micrometer:micrometer-tracing-bridge-brave")
implementation("io.zipkin.reporter2:zipkin-reporter-brave")
```

---

## Metrics

### Domain-Level Metrics (Per Module)

| Metric | Type | Description | Labels |
|--------|------|-------------|--------|
| `domain.user.follow.total` | Counter | Total follow operations | result=success\|error |
| `domain.user.unfollow.total` | Counter | Total unfollow operations | result=success\|error |
| `domain.user.follow.duration` | Timer | Follow operation latency | - |
| `domain.user.privacy.change.total` | Counter | Total privacy setting changes | result=success\|error |
| `domain.post.create.duration` | Timer | Post creation latency | - |
| `domain.message.send.duration` | Timer | Message send latency | - |
| `domain.notification.created` | Counter | Notifications created | type=FOLLOW_REQUEST\|POST_REACTION\|... |
| `domain.outbox.publish.duration` | Timer | Outbox poller publish latency | - |
| `domain.event.processing.duration` | Timer | Event consumer processing time | eventType=UserRegistered\|UserFollowed\|UserUnfollowed\|UserPrivacyChanged\|PostCreated\|CommentCreated\|ReactionAdded\|MessageSent |
| `domain.post.visibility.projector.duration` | Timer | PostVisibilityProjector event handling latency | eventType=UserRegistered\|UserPrivacyChanged\|UserFollowed\|UserUnfollowed |
| `domain.post.visibility.table.size` | Gauge | Rows in post_author_visibility table | - |
| `domain.post.viewer_author_access.size` | Gauge | Rows in viewer_author_access table | - |

### Standard JVM/HTTP Metrics (Auto-configured)
| Metric | Description |
|--------|-------------|
| `jvm.memory.used` | Heap/non-heap memory |
| `jvm.threads.live` | Live thread count (virtual + platform) |
| `http.server.requests` | HTTP request latency, count, errors |
| `hikaricp.connections.active` | Active DB connections |
| `rabbitmq.connections` | RabbitMQ connection count |

### Custom Metrics Registration Pattern
```java
@Service
@RequiredArgsConstructor
public class XxxServiceImpl {
    private final Counter createCounter;
    private final Timer createTimer;
    
    public XxxServiceImpl(MeterRegistry registry) {
        this.createCounter = Counter.builder("domain.xxx.create.total").register(registry);
        this.createTimer = Timer.builder("domain.xxx.create.duration").register(registry);
    }
    
    public Xxx create(...) {
        return createTimer.record(() -> {
            // ... business logic
            createCounter.increment();
        });
    }
}
```

### Prometheus Endpoint
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
```

Scrape at `/actuator/prometheus`

---

## Health Checks

### Per-Domain Health Indicators
Each domain module provides a `HealthIndicator`:

| Module | Indicator | Details |
|--------|-----------|---------|
| domain-user | `UserHealthIndicator` | totalUsers |
| domain-post | `PostHealthIndicator` | totalPosts |
| domain-photo | `PhotoHealthIndicator` | totalPhotos |
| domain-comment | `CommentHealthIndicator` | totalComments |
| domain-reaction | `ReactionHealthIndicator` | totalReactions |
| domain-chat | `ChatHealthIndicator` | totalChats |
| domain-message | `MessageHealthIndicator` | totalMessages |
| domain-notification | `NotificationHealthIndicator` | totalNotifications |

### Health Endpoint
```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  endpoints:
    web:
      exposure:
        include: health
```

Access at `/actuator/health` (requires authentication)

---

## Testing Observability

### Tracing Integration Test
```java
@SpringBootTest
@Testcontainers
class TracingIntegrationTest {
    
    @Autowired Tracer tracer;
    @Autowired TransactionalEventPublisher publisher;
    
    @Test
    void eventFlow_propagatesTraceContext() {
        Span span = tracer.nextSpan().name("test-root").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            var event = new UserRegisteredEvent(UUID.randomUUID(), "user", "u@x.com");
            publisher.publish(event);
        } finally {
            span.end();
        }
        
        // Verify span exported to Zipkin
        await().untilAsserted(() -> {
            List<Span> spans = zipkinClient.getSpans("test-root");
            assertThat(spans).hasSizeGreaterThan(1); // root + child spans
        });
    }
}
```

### Metrics Test
```java
@SpringBootTest
class MetricsTest {
    
    @Autowired MeterRegistry registry;
    @Autowired UserService userService;
    
    @Test
    void followOperation_recordsMetrics() {
        userService.followUser(targetId);
        
        assertThat(registry.get("domain.user.follow.total").counter().count()).isEqualTo(1);
        assertThat(registry.get("domain.user.follow.duration").timer().count()).isEqualTo(1);
    }
}
```

### Health Indicator Test
```java
@ExtendWith(MockitoExtension.class)
class UserHealthIndicatorTest {
    
    @Mock UserRepository userRepository;
    @InjectMocks UserHealthIndicator indicator;
    
    @Test
    void health_givenRepositoryWorks_shouldBeUpWithCount() {
        when(userRepository.count()).thenReturn(42L);
        
        Health health = indicator.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalUsers", 42L);
    }
}
```

---

## Dashboards

### Grafana Dashboard (Recommended Panels)
| Panel | Query |
|-------|-------|
| Request Rate | `rate(http_server_requests_seconds_count[5m])` |
| Error Rate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` |
| Latency p99 | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))` |
| Follow Rate | `rate(domain_user_follow_total[5m])` |
| Event Processing Latency | `histogram_quantile(0.99, rate(domain_event_processing_duration_seconds_bucket[5m]))` |
| Outbox Lag | `time() - max(outbox_event_published_at_timestamp)` |
| Virtual Threads | `jvm_threads_live_threads{virtual="true"}` |
| DB Connections | `hikaricp_connections_active` |
| Post Visibility Lag | `time() - max(post_author_visibility.updated_at)` |

---

## Alerting Rules (Prometheus)
```yaml
groups:
  - name: quietspace-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP 5xx error rate"
       
      - alert: OutboxLag
        expr: time() - max(outbox_event_published_at_timestamp) > 300
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Outbox events not published for 5+ minutes"
       
      - alert: VirtualThreadPinning
        expr: increase(jdk_VirtualThreadPinned_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Virtual thread pinning detected"
       
      - alert: PostVisibilityProjectorLag
        expr: time() - max(post_author_visibility.updated_at_timestamp) > 60
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Post visibility projector lagging - events not processed"
```

---

## Observability Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-09-26 | Event-driven feed visibility (ADR 005) | Accepted eventual consistency on privacy changes to enable pagination-correct SQL filtering without cross-module joins |
| 2026-09-26 | Projector metrics `domain.post.visibility.projector.duration` | Track outbox processing latency; alert on lag > 60s |
| 2026-09-26 | Event processing metrics by type | Distinguish latency by event type (privacy vs follow vs registration) |
| 2026-09-26 | Visibility table size gauges | Monitor denormalized table growth; detect projector backlog |
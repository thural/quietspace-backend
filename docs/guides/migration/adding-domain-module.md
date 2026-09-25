# Migration Guides

## Adding a New Domain Module

### 1. Create Module Structure
```bash
mkdir -p domain/domain-xxx/src/{main,test}/java/dev/thural/quietspace/domain/xxx
```

### 2. build.gradle.kts
```kotlin
plugins {
    id("quietspace.domain-conventions")
}

dependencies {
    implementation(project(":core:core-shared"))
    implementation(project(":core:core-data"))
    // Add other core modules as needed
    // implementation(project(":domain:domain-user")) // if depends on user
}
```

### 3. Aggregate (Rich Domain Model)
```java
// domain/xxx/XXX.java
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Xxx extends BaseEntity {
    // Business fields (NO public setters)
    
    // Factory method
    public static Xxx create(...) { ... }
    
    // Domain methods (encapsulate behavior)
    public void doBusinessAction(...) { ... }
}
```

### 4. Package-Private Repository
```java
// domain/xxx/repository/XXXRepository.java
interface XXXRepository extends JpaRepository<XXX, UUID> {
    // Custom queries
}
```

### 5. Consumer-Owned Ports (if needed)
```java
// domain/xxx/port/XXXPort.java
interface XXXPort {
    ReturnType doSomething(UUID id);
}
```

### 6. Service Implementation
```java
// domain/xxx/service/XXXServiceImpl.java
@Service @Transactional @RequiredArgsConstructor
public class XXXServiceImpl implements XXXService {
    private final XXXRepository repository;
    private final TransactionalEventPublisher eventPublisher;
    private final OtherDomainPort otherPort; // if needed
    
    public XXX create(...) {
        XXX entity = XXX.create(...);
        repository.save(entity);
        eventPublisher.publish(new XxxCreatedEvent(...));
        return entity;
    }
}
```

### 7. Controller (Thin)
```java
// domain/xxx/controller/XXXController.java
@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/xxx")
public class XXXController {
    private final XXXService service;
    
    @PostMapping
    ResponseEntity<XXXResponse> create(@RequestBody CreateRequest req) {
        return ResponseEntity.ok(mapper.toResponse(service.create(...)));
    }
}
```

### 8. Per-Module ArchUnit Test
```java
// domain/xxx/archunit/DomainXxxArchitectureRulesTest.java
class DomainXxxArchitectureRulesTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("dev.thural.quietspace.domain.xxx");
    
    @Test
    void domain_xxx_has_no_other_domain_dependencies() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("dev.thural.quietspace.domain.xxx..")
            .should().dependOnClassesThat()
            .resideInAPackage("dev.thural.quietspace.domain.yyy..")
            .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
```

### 9. Tests (≥80% Coverage)
| Test Type | Example | Coverage Target |
|-----------|---------|-----------------|
| Unit | `XxxTest.java` | Domain methods |
| Unit | `XXXServiceImplTest.java` | Service logic (mock repo/ports) |
| Slice | `XXXControllerSliceTest.java` | Endpoints (mock service) |
| Integration | `XXXFlowIT.java` | Full stack (Testcontainers) |

Run: `./gradlew :domain:domain-xxx:test :domain:domain-xxx:jacocoTestReport`

### 10. Wire in App
```java
// app/quietspace-app/src/main/java/.../QuietspaceApplication.java
@SpringBootApplication
@EnableJpaRepositories(basePackages = "dev.thural.quietspace.domain.xxx.repository")
@EntityScan(basePackages = "dev.thural.quietspace.domain.xxx")
public class QuietspaceApplication { ... }
```

Add to `settings.gradle.kts`: `include("domain:domain-xxx")`

### 11. Verify All Gates
```bash
./gradlew :domain:domain-xxx:test
./gradlew :domain:domain-xxx:spotbugsMain :domain:domain-xxx:pmdMain :domain:domain-xxx:checkstyleMain
./gradlew :domain:domain-xxx:jacocoTestReport  # verify ≥80%
./gradlew test  # full regression
```

---

## Adding a New Domain Event

### 1. Create Event in core-shared
```java
// core-shared/event/XxxCreatedEvent.java
@Getter @Setter
public class XxxCreatedEvent extends DomainEvent {
    private UUID xxxId;
    private String someField;
    
    public XxxCreatedEvent() { setEventType("XxxCreated"); }
    
    public XxxCreatedEvent(UUID xxxId, String someField) {
        this();
        setAggregateType("Xxx");
        setAggregateId(xxxId);
        this.xxxId = xxxId;
        this.someField = someField;
    }
}
```

### 2. Add Serialization Test
```java
// core-shared/EventSerializerTest.java
@Test
void serialize_thenDeserialize_preservesXxxCreatedEvent() {
    var original = new XxxCreatedEvent(UUID.randomUUID(), "value");
    String json = serializer.serialize(original);
    var deserialized = serializer.deserialize(json, XxxCreatedEvent.class);
    assertThat(deserialized.getXxxId()).isEqualTo(original.getXxxId());
}
```

### 3. Publish from Domain Service
```java
// domain-xxx/XXXServiceImpl.java
public XXX create(...) {
    XXX entity = XXX.create(...);
    repository.save(entity);
    eventPublisher.publish(new XxxCreatedEvent(entity.getId(), "value"));
    return entity;
}
```

### 4. Consume in Target Domain
```java
// domain-notification/NotificationEventListener.java
@Component @RequiredArgsConstructor
public class NotificationEventListener {
    @EventListener
    public void onXxxCreated(XxxCreatedEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) return;
        // Create notification
        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
    }
}
```

### 5. Add Integration Test
```java
// app/.../XxxEventFlowIT.java
@SpringBootTest @Testcontainers
class XxxEventFlowIT {
    @Test
    void xxxCreation_publishesEvent_consumedByNotification() {
        // Create aggregate
        // Verify notification created via event
    }
}
```

---

## Upgrading Core Module

### Adding a New Core Utility
1. Add class to appropriate core module (`core-shared`, `core-web`, etc.)
2. Add unit test in same module
3. Ensure **no domain dependencies** (ArchUnit will enforce)
4. Export via `api` (not `implementation`) if needed by domain modules

### Changing Core Interface
1. Update interface in core module
2. Update all implementations in domain modules
3. Run full test suite: `./gradlew test`
4. ArchUnit will catch missing implementations

---

## Virtual Thread Migration (Java 25)

### Already Enabled
- `spring.threads.virtual.enabled=true` in `application.yml`
- Tomcat uses virtual threads for HTTP
- `@Async` uses `VirtualThreadPerTaskExecutor`

### For Blocking Libraries
| Library | Status | Action |
|---------|--------|--------|
| HikariCP | Compatible | Pool size = CPU cores × 2 |
| JDBC (MySQL) | Compatible | Use async driver if available |
| Redis (Lettuce) | Compatible | Non-blocking by default |
| RabbitMQ | Compatible | Use async client |

### Pinning Detection
```bash
# Enable JFR recording
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr ...

# Analyze for pinning
jfr print --events jdk.VirtualThreadPinned recording.jfr
```

**Fix pinning**: Replace `synchronized` with `ReentrantLock` in hot paths.

---

## Database Migration (Flyway)

### Adding a Migration
1. Create `V{next}__description.sql` in `core/core-data/src/main/resources/db/migration/`
2. Write idempotent SQL (use `IF NOT EXISTS`, `ON CONFLICT`)
3. Test: `./gradlew :core:core-data:test` + `./gradlew :app:quietspace-app:test`

### Example
```sql
-- V5__add_processed_events_table.sql
CREATE TABLE IF NOT EXISTS processed_events (
    event_id CHAR(36) PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_events_processed_at 
ON processed_events (processed_at);
```

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Domain module depends on another's internal class | Use consumer-owned port (interface in consumer, adapter in provider) |
| Controller injects repository | Inject service instead (ArchUnit enforces) |
| Repository made public | Keep package-private (ArchUnit enforces) |
| Event consumer not idempotent | Check `processedEventRepository.existsByEventId()` |
| New module <80% coverage | Add unit/slice/IT tests before merging |
| Virtual thread pinning | Monitor JFR, replace `synchronized` with `ReentrantLock` |
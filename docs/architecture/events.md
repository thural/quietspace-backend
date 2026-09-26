# Domain Event Catalog

## Overview
All domain events are defined in `core-shared` and published via **Transactional Outbox** pattern for guaranteed at-least-once delivery.

---

## Event Base Class
```java
public abstract class DomainEvent {
    private UUID eventId = UUID.randomUUID();
    private OffsetDateTime timestamp = OffsetDateTime.now();
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
}
```

---

## Events

### 1. UserRegisteredEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "User" |
| `aggregateId` | UUID | User ID |
| `eventType` | String | "UserRegistered" |
| `username` | String | Registered username |
| `email` | String | Registered email |

**Publisher**: `domain-user` → `AuthService.registerUser()`
**Consumers**: `domain-notification` → welcome notification
**Routing Key**: `domain.user.registered`

---

### 2. PostCreatedEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "Post" |
| `aggregateId` | UUID | Post ID |
| `eventType` | String | "PostCreated" |
| `authorId` | UUID | Post author ID |
| `title` | String | Post title |
| `text` | String | Post content |

**Publisher**: `domain-post` → `PostService.createPost()`
**Consumers**: `domain-notification` → mention notifications
**Routing Key**: `domain.post.created`

---

### 3. CommentCreatedEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "Comment" |
| `aggregateId` | UUID | Comment ID |
| `eventType` | String | "CommentCreated" |
| `postId` | UUID | Parent post ID |
| `authorId` | UUID | Comment author ID |
| `text` | String | Comment content |

**Publisher**: `domain-comment` → `CommentService.createComment()`
**Consumers**: `domain-notification` → comment notification to post owner
**Routing Key**: `domain.comment.created`

---

### 4. ReactionAddedEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "Reaction" |
| `aggregateId` | UUID | Reaction ID |
| `eventType` | String | "ReactionAdded" |
| `contentId` | UUID | Target content ID (post/comment/message) |
| `contentType` | EntityType | POST, COMMENT, MESSAGE |
| `reactionType` | ReactionType | LIKE, DISLIKE |
| `actorId` | UUID | User who added reaction |

**Publisher**: `domain-reaction` → `ReactionService.addReaction()`
**Consumers**: `domain-notification` → reaction notification to content owner
**Routing Key**: `domain.reaction.added`

---

### 5. MessageSentEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "Message" |
| `aggregateId` | UUID | Message ID |
| `eventType` | String | "MessageSent" |
| `chatId` | UUID | Chat ID |
| `senderId` | UUID | Message sender ID |
| `text` | String | Message content |

**Publisher**: `domain-message` → `MessageService.sendMessage()`
**Consumers**: `domain-notification` → message notification; `domain-chat` → update last message
**Routing Key**: `domain.message.sent`

---

### 6. UserFollowedEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "User" |
| `aggregateId` | UUID | Followed user ID |
| `eventType` | String | "UserFollowed" |
| `followerId` | UUID | User who followed |
| `followedId` | UUID | User who was followed |

**Publisher**: `domain-user` → `UserService.followUser()`
**Consumers**: `domain-notification` → follow notification
**Routing Key**: `domain.user.followed`

---

### 7. UserUnfollowedEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "User" |
| `aggregateId` | UUID | Unfollowed user ID |
| `eventType` | String | "UserUnfollowed" |
| `followerId` | UUID | User who unfollowed |
| `followedId` | UUID | User who was unfollowed |

**Publisher**: `domain-user` → `UserService.unfollowUser()`, `removeFollower()`
**Consumers**: `domain-post` → `PostVisibilityProjector` (removes viewer→author access edge)
**Routing Key**: `domain.user.unfollowed`

---

### 8. UserPrivacyChangedEvent
| Property | Type | Description |
|----------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `timestamp` | OffsetDateTime | Event creation time |
| `aggregateType` | String | "User" |
| `aggregateId` | UUID | User ID |
| `eventType` | String | "UserPrivacyChanged" |
| `userId` | UUID | User ID |
| `isPrivate` | Boolean | New privacy state |

**Publisher**: `domain-user` → `UserService.saveProfileSettings()` (only on change)
**Consumers**: `domain-post` → `PostVisibilityProjector` (upserts author visibility row)
**Routing Key**: `domain.user.privacy-changed`
**Consistency note**: see ADR 005 — sub-second propagation window accepted by product sign-off.

---

### 9. EmailEvent
| Property | Type | Description |
|----------|------|-------------|
| `to` | String | Recipient email |
| `subject` | String | Email subject |
| `templateName` | String | Thymeleaf template name |
| `variables` | Map<String, Object> | Template variables |

**Publisher**: Any domain via `EmailEventPublisher`
**Consumer**: `core-messaging` → `EmailEventConsumer` → `EmailService`
**Queue**: `email.queue` (bound to `email.exchange`)

---

## Infrastructure

### Transactional Outbox
| Component | Purpose |
|-----------|---------|
| `OutboxEvent` | JPA entity: id, aggregateType, aggregateId, eventType, payload, createdAt, publishedAt |
| `OutboxEventRepository` | Spring Data: `findUnpublishedEvents()`, `markAsPublished()` |
| `TransactionalEventPublisher` | Saves event to outbox within same transaction as aggregate |
| `OutboxEventPoller` | `@Scheduled` (5s) polls unpublished, publishes to RabbitMQ, marks published |

### RabbitMQ Topology
```
Exchange: domain.events (topic)
  └── Queues (per consumer domain):
      ├── notification.events (domain-notification)
      └── websocket.events (core-messaging STOMP relay)

Exchange: email.exchange (topic)
  └── Queue: email.queue (core-messaging EmailEventConsumer)
```

### STOMP over WebSocket
- **Broker Relay**: RabbitMQ (`/topic`, `/queue`)
- **User Destination Prefix**: `/user`
- **Heartbeat**: 30s
- **Message Flow**: Domain event → RabbitMQ → STOMP relay → user's connected node → WebSocket session

---

## Idempotency Contract
**All consumers MUST be idempotent** (at-least-once delivery).

```java
@EventListener
public void onUserRegistered(UserRegisteredEvent event) {
    if (processedEventRepository.existsByEventId(event.getEventId())) {
        return; // Already processed
    }
    // ... process event
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

**ProcessedEvent Table**: `eventId` (PK), `processedAt`, TTL-based cleanup.

---

## Testing Events
| Test | Module | Purpose |
|------|--------|---------|
| `EventSerializerTest` | core-shared | Serialize/deserialize roundtrip for all events |
| `TransactionalEventPublisherTest` | core-shared | Event saved in same TX, rollback on failure |
| `NotificationEventListenerTest` | domain-notification | Each `@EventListener` creates correct notification |
| `OutboxEventPollerIntegrationTest` | app | End-to-end: aggregate change → outbox → RabbitMQ → consumer |
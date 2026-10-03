# Domain Module Contracts

## Overview
Each domain module is a **self-contained vertical slice** owning a single aggregate and its behavior. Modules communicate **only via consumer-owned ports** (interfaces in consumer module, adapters in provider module) and **domain events** (published to RabbitMQ, consumed asynchronously).

---

## Dependency Rules (Enforced by ArchUnit)
- Domain modules **may depend on core modules** (core-shared, core-data, core-security, core-messaging, core-web)
- Domain modules **must NOT depend on other domain modules' internal packages** (adapter, service, controller, model, repository)
- Domain modules **may implement ports** owned by other domains (consumer-owned ports pattern)

---

## Module Contracts

### domain-user
**Aggregate**: `User`, `ProfileSettings`, `Follow` (value object), `Token` (refresh/blacklist records)

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| `UserProfilePort` | `UserRepresentation getUser(UUID userId)` | Minimal user data for cross-module |
| `WebSocketUserPort` | `UserRepresentation getUser(UUID userId)` | WebSocket auth resolution |

| Service | Key Methods |
|---------|-------------|
| `UserService` | `followUser`, `unfollowUser`, `blockUser`, `unblockUser`, `updateProfile`, `getSignedUser` |
| `AuthService` | `register` (publishes `UserRegisteredEvent` with `activationCode`), `authenticate`, `activateAccount`, `refreshToken`, `signout` |
| `TokenService` | `saveRefreshToken`, `blacklistToken`, `revokeTokenByJti`, `cleanExpiredTokens` |
| `UserAuthenticationProvider` | `loadUserByUsername` (implements `core-security.port.AuthenticationProvider`) |
| `CurrentUserAdapter` | `currentUserId` (implements `core-security.port.CurrentUserPort`) |
| `TokenBlacklistAdapter` | `isBlacklisted`, `isRevokedByJti` (implements `core-security.port.TokenBlacklistPort`) |

| Events Published | Events Consumed |
|------------------|-----------------|
| `UserRegisteredEvent` (incl. `activationCode`) | — |
| `UserFollowedEvent` | — |
| `UserUnfollowedEvent` | — |
| `UserPrivacyChangedEvent` | — |

| Repository | Package-Private |
|------------|-----------------|
| `UserRepository` | ✅ |
| `TokenRepository` | ✅ |

---

### domain-post
**Aggregate**: `Post`, `Poll`, `PollOption`, `PollVote`, `Save`, `Repost`

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| `CommentPostPort` | `UUID findPostOwnerId(UUID postId)` | Notification: post owner for comment |
| `NotificationPostPort` | `UUID findPostOwnerId(UUID postId)` | Notification: post owner for reaction/mention |

| Service | Key Methods |
|---------|-------------|
| `PostService` | `createPost`, `votePoll`, `savePost`, `unsavePost`, `repost`, `getFeed` |
| `PollService` | `createPoll`, `votePoll`, `getPollResults` |
| `PostSecurityService` | `canAccess` (authorization via `UserQueryPort`) |

| Events Published | Events Consumed |
|------------------|-----------------|
| `PostCreatedEvent` | — |

| Repository | Package-Private |
|------------|-----------------|
| `PostRepository` | ✅ |
| `PollRepository` | ✅ |

---

### domain-photo
**Aggregate**: `Photo`, `PhotoMetadata`

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| *(none — leaf module)* | — | — |

| Service | Key Methods |
|---------|-------------|
| `PhotoService` | `uploadPhoto`, `deletePhoto`, `getPhotoUrl` |

| Events Published | Events Consumed |
|------------------|-----------------|
| — | — |

| Repository | Package-Private |
|------------|-----------------|
| `PhotoRepository` | ✅ |

---

### domain-comment
**Aggregate**: `Comment`, `CommentReaction`

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| `NotificationCommentPort` | `UUID findCommentOwnerId(UUID commentId)` | Notification: comment owner for reply/reaction |
| `CommentPostPort` | `boolean postExists(UUID postId)` | Verify post exists for comment creation |

| Service | Key Methods |
|---------|-------------|
| `CommentService` | `createComment`, `updateComment`, `deleteComment`, `getCommentsByPost` |

| Events Published | Events Consumed |
|------------------|-----------------|
| `CommentCreatedEvent` | — |

| Repository | Package-Private |
|------------|-----------------|
| `CommentRepository` | ✅ |

---

### domain-reaction
**Aggregate**: `Reaction`

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| *(none)* | — | — |

| Service | Key Methods |
|---------|-------------|
| `ReactionService` | `addReaction`, `removeReaction`, `getReactionsByContent` |

| Events Published | Events Consumed |
|------------------|-----------------|
| `ReactionAddedEvent` | — |

| Repository | Package-Private |
|------------|-----------------|
| `ReactionRepository` | ✅ |

---

### domain-chat
**Aggregate**: `Chat`, `ChatMember`

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| `ChatMessagePort` | `void sendMessage(UUID chatId, UUID senderId, String text)` | Called by domain-message adapter |

| Service | Key Methods |
|---------|-------------|
| `ChatService` | `createChat`, `addMember`, `removeMember`, `getChatsForUser` |

| Events Published | Events Consumed |
|------------------|-----------------|
| — | `MessageSentEvent` (updates last message) |

| Repository | Package-Private |
|------------|-----------------|
| `ChatRepository` | ✅ |

---

### domain-message
**Aggregate**: `Message`, `ReadReceipt`

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| *(none)* | — | — |

| Service | Key Methods |
|---------|-------------|
| `MessageService` | `sendMessage`, `getMessagesByChat`, `markAsRead`, `getUnreadCount` |

| Events Published | Events Consumed |
|------------------|-----------------|
| `MessageSentEvent` | — |

| Repository | Package-Private |
|------------|-----------------|
| `MessageRepository` | ✅ |

---

### domain-notification
**Aggregate**: `Notification` (sink module — no outbound dependencies)

| Port (Owned) | Interface | Purpose |
|--------------|-----------|---------|
| `NotificationUserPort` | `UserRepresentation getUser(UUID userId)` | Resolve notification recipient |
| `NotificationPostPort` | `UUID findPostOwnerId(UUID postId)` | Implemented by domain-post adapter |
| `NotificationCommentPort` | `UUID findCommentOwnerId(UUID commentId)` | Implemented by domain-comment adapter |

| Service | Key Methods |
|---------|-------------|
| `NotificationService` | `getAllNotifications`, `getNotificationsByType`, `markAsRead`, `getCountOfPendingNotifications` |
| `NotificationEventListener` | `@EventListener` for all 6+ domain events |

| Events Published | Events Consumed |
|------------------|-----------------|
| — | `UserRegisteredEvent`, `PostCreatedEvent`, `CommentCreatedEvent`, `ReactionAddedEvent`, `MessageSentEvent`, `UserFollowedEvent`, `UserUnfollowedEvent`, `UserPrivacyChangedEvent` |

| Repository | Package-Private |
|------------|-----------------|
| `NotificationRepository` | ✅ |

---

## Cross-Domain Query Ports (`api` packages, Phase 3.1/3.4)

Each domain exposes lightweight immutable read-models for cross-domain display
needs. Ports live in `<domain>.api`, DTOs in `<domain>.api.dto` (Java records),
adapters delegate to the owning repository. Authentication (`getSignedUser`)
stays on `UserService`; these ports are display data only.

| Port | Owner Module | Methods | DTO |
|------|--------------|---------|-----|
| `UserQueryPort` | domain-user | `getUserSummary(UUID)`, `getUsersSummary(Set<UUID>)`, `findUserIdByUsernameOrEmail(String)`, `searchUserIds(String)` | `UserSummaryDTO(id, username, displayName, photoId, statusType)` |
| `PhotoQueryPort` | domain-photo | `getPhotoSummary(UUID)`, `getPhotoSummaryByEntityId(UUID)`, `getPhotosSummary(Set<UUID>)` | `PhotoSummaryDTO(id, name, type, userId, entityId, entityType)` |
| `PostQueryPort` | domain-post | `getPostSummary(UUID)`, `getPostsSummary(Set<UUID>)` | `PostSummaryDTO(id, authorId, title, text, photoId)` |
| `CommentQueryPort` | domain-comment | `getCommentSummary(UUID)`, `getCommentsSummary(Set<UUID>)`, `countCommentsByPostId(UUID)`, `findPostIdsByUserId(UUID)` | `CommentSummaryDTO(id, postId, authorId, parentId, text)` |
| `ReactionQueryPort` | domain-reaction | `getReactionSummary(UUID)`, `getReactionsSummary(Set<UUID>)`, `countReactions(UUID, ReactionType)` | `ReactionSummaryDTO(id, userId, username, contentId, contentType, reactionType)` |

First consumer migrations (Phase 5.1):
- `CommentMapper` uses `ReactionQueryPort.countReactions` instead of `ReactionRepository`
- `CommentMapper` uses `UserQueryPort.getUserSummary` instead of `UserRepository`
- `MessageMapper` uses `UserQueryPort.getUserSummary` for sender/recipient names
- `PostMapper` uses `UserQueryPort.getUserSummary` for author names
- `PostMapper` uses `CommentQueryPort.countCommentsByPostId` for comment counts
- `PostSecurityService` uses `UserQueryPort.findUserIdByUsernameOrEmail` instead of `UserRepository`
- `PostSpecifications.containsText` uses `UserQueryPort.searchUserIds` for author search
- `PostSpecifications.commentedByUser` uses `CommentQueryPort.findPostIdsByUserId`
- `PostSpecifications.visibleToUser` uses `PostAuthorVisibility` + `ViewerAuthorAccess` local tables
- `PostNotificationAdapter` uses `Post.authorId` (read-only FK view)
- `PostCommentAdapter` uses `PostRepository.existsById`
- `CommentNotificationAdapter` uses `Comment.userId` (FK column)

Intentionally retained direct access: message→chat (chat owns message), entity-graph navigation (`Comment.user`, `Message.sender`, `Message.recipient`, `PostSecurityService` authorization lookups), and shared-kernel `UserProfilePort` (photo must not depend on user — see `core-shared.ports`).

---

## Consumer-Owned Ports Pattern

```
┌─────────────────────────────────────────────────────────────┐
│  CONSUMER MODULE (e.g., domain-notification)                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ interface NotificationPostPort {                    │   │
│  │     UUID findPostOwnerId(UUID postId);              │   │
│  │ }                                                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                               ▲
                               │ implements
                               │
┌─────────────────────────────────────────────────────────────┐
│  PROVIDER MODULE (e.g., domain-post)                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ class PostNotificationAdapter implements            │   │
│  │     NotificationPostPort {                          │   │
│  │     UUID findPostOwnerId(UUID postId) { ... }       │   │
│  │ }                                                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Rule**: Port interface lives in consumer module. Adapter implementation lives in provider module. This keeps dependency direction: consumer → port (own), provider → port (depends on consumer).

---

## Event Catalog

| Event | Publisher | Consumers | Payload |
|-------|-----------|-----------|---------|
| `UserRegisteredEvent` | domain-user (AuthService) | domain-notification (welcome notification + activation email) | userId, username, email, activationCode |
| `PostCreatedEvent` | domain-post (PostService) | domain-notification | postId, authorId, title, text |
| `CommentCreatedEvent` | domain-comment (CommentService) | domain-notification | commentId, postId, authorId, text |
| `ReactionAddedEvent` | domain-reaction (ReactionService) | domain-notification | reactionId, contentId, contentType, reactionType, actorId |
| `MessageSentEvent` | domain-message (MessageService) | domain-notification, domain-chat | messageId, chatId, senderId, text |
| `UserFollowedEvent` | domain-user (UserService) | domain-notification, domain-post (via projector) | followerId, followedId |
| `UserUnfollowedEvent` | domain-user (UserService) | domain-post (via projector) | followerId, followedId |
| `UserPrivacyChangedEvent` | domain-user (UserService) | domain-post (via projector) | userId, isPrivate |

**Delivery**: Transactional Outbox → RabbitMQ (`domain.events` topic exchange) → STOMP/WebSocket + `@EventListener` consumers
**Idempotency**: All consumers check `processed_events` table by `eventId` before processing.

---

## Consumer Migrations (Phase 5.1)

| Consumer | Before | After | Port Used |
|----------|--------|-------|-----------|
| `CommentMapper.getLikeCount` | `ReactionRepository.countByContentIdAndReactionType` | `ReactionQueryPort.countReactions` | `ReactionQueryPort` |
| `CommentMapper.resolveUsername` | `UserRepository.findById` | `UserQueryPort.getUserSummary` | `UserQueryPort` |
| `MessageMapper` (sender/recipient names) | `UserRepository` | `UserQueryPort.getUserSummary` | `UserQueryPort` |
| `PostMapper` (author name, repost author) | `UserRepository` | `UserQueryPort.getUserSummary` | `UserQueryPort` |
| `PostMapper.commentCount` | `post.getComments().size()` | `CommentQueryPort.countCommentsByPostId` | `CommentQueryPort` |
| `PostSecurityService.canAccess` | `UserRepository.findUserEntityByEmail/findUserByUsername` | `UserQueryPort.findUserIdByUsernameOrEmail` | `UserQueryPort` |
| `PostSpecifications.containsText` | `root.get("user").get("username")` join | `UserQueryPort.searchUserIds` + `IN` clause | `UserQueryPort` |
| `PostSpecifications.commentedByUser` | `JOIN p.comments c WHERE c.user.id = :userId` | `CommentQueryPort.findPostIdsByUserId` + `IN` clause | `CommentQueryPort` |
| `PostSpecifications.visibleToUser` | JOINs to user/profile_settings/followers | Local tables `post_author_visibility` + `viewer_author_access` | — |
| `PostNotificationAdapter.findPostOwnerId` | `post.getUser().getId()` | `post.getAuthorId()` (FK view) | — |
| `CommentNotificationAdapter.findCommentOwnerId` | `comment.getUser().getId()` | `comment.getUserId()` (FK column) | — |

---

## Adding a New Domain Module

1. **Create module structure** under `domain/domain-xxx/`
2. **Define aggregate** with rich domain methods (no public setters)
3. **Create package-private repository** interface
4. **Define owned ports** (interfaces in `port/` package)
5. **Implement service** using domain methods + ports
6. **Publish domain events** via `TransactionalEventPublisher`
7. **Create controller** (thin, delegates to service)
8. **Add ArchUnit test** forbidding internal access to other domains
9. **Achieve ≥80% coverage** (unit + slice + integration tests)
10. **Wire in app/quietspace-app**
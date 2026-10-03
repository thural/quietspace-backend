package dev.thural.quietspace.domain.notification;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.event.*;
import dev.thural.quietspace.core.shared.service.impl.EmailEventPublisher;
import dev.thural.quietspace.domain.notification.port.NotificationCommentPort;
import dev.thural.quietspace.domain.notification.port.NotificationPostPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.thural.quietspace.core.messaging.constant.WebSocketPaths.NOTIFICATION_SUBJECT;
import static dev.thural.quietspace.core.messaging.constant.WebSocketPaths.UNREAD_COUNT;
import static dev.thural.quietspace.domain.notification.NotificationType.COMMENT_REACTION;
import static dev.thural.quietspace.domain.notification.NotificationType.POST_REACTION;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationCommentPort commentPort;
    private final NotificationPostPort postPort;
    private final SimpMessagingTemplate template;
    private final ProcessedEventRepository processedEventRepository;
    private final EmailEventPublisher emailEventPublisher;

    @Value("${spring.application.mailing.frontend.activation-url}")
    private String activationUrl;

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }
        log.info("Processing UserRegisteredEvent for user {}", event.getAggregateId());
        // Send welcome notification
        var notification = notificationRepository.save(
                Notification.builder()
                        .notificationType(NotificationType.FOLLOW_REQUEST)
                        .contentId(event.getAggregateId())
                        .actorId(event.getAggregateId())
                        .userId(event.getAggregateId())
                        .build()
        );
        sendNotification(event.getAggregateId(), notification);
        sendActivationEmail(event);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(java.time.OffsetDateTime.now())
                .build());
    }

    private void sendActivationEmail(UserRegisteredEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", event.getUsername());
        variables.put("confirmationUrl", activationUrl);
        variables.put("activationCode", event.getActivationCode());

        emailEventPublisher.publish(new EmailEvent(
                event.getEmail(),
                "account activation",
                "activate_account",
                variables
        ));
    }

    @EventListener
    @Transactional
    public void onPostCreated(PostCreatedEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }
        log.info("Processing PostCreatedEvent for post {}", event.getAggregateId());
        // Handle mentions, etc.
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(java.time.OffsetDateTime.now())
                .build());
    }

    @EventListener
    @Transactional
    public void onCommentCreated(CommentCreatedEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }
        log.info("Processing CommentCreatedEvent for comment {}", event.getAggregateId());
        var notification = notificationRepository.save(
                Notification.builder()
                        .notificationType(NotificationType.COMMENT)
                        .contentId(event.getAggregateId())
                        .actorId(event.getAuthorId())
                        .userId(getUserIdByPostId(event.getPostId()))
                        .build()
        );
        sendNotification(getUserIdByPostId(event.getPostId()), notification);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(java.time.OffsetDateTime.now())
                .build());
    }

    @EventListener
    @Transactional
    public void onReactionAdded(ReactionAddedEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }
        log.info("Processing ReactionAddedEvent for reaction {}", event.getAggregateId());
        NotificationType type = switch (event.getContentType()) {
            case COMMENT -> COMMENT_REACTION;
            case POST -> POST_REACTION;
            default -> throw new IllegalArgumentException("Unknown content type: " + event.getContentType());
        };
        var notification = notificationRepository.save(
                Notification.builder()
                        .notificationType(type)
                        .contentId(event.getContentId())
                        .actorId(event.getActorId())
                        .userId(getRecipientId(type, event.getContentId()))
                        .build()
        );
        sendNotification(getRecipientId(toNotificationType(event.getContentType()), event.getContentId()), notification);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(java.time.OffsetDateTime.now())
                .build());
    }

    @EventListener
    @Transactional
    public void onMessageSent(MessageSentEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }
        log.info("Processing MessageSentEvent for message {}", event.getAggregateId());
        // Note: MessageSentEvent doesn't have recipientId, notification would need to be sent to chat participants
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(java.time.OffsetDateTime.now())
                .build());
    }

    @EventListener
    @Transactional
    public void onUserFollowed(UserFollowedEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }
        log.info("Processing UserFollowedEvent for user {}", event.getAggregateId());
        var notification = notificationRepository.save(
                Notification.builder()
                        .notificationType(NotificationType.FOLLOW_REQUEST)
                        .contentId(event.getFollowedId())
                        .actorId(event.getFollowerId())
                        .userId(event.getFollowedId())
                        .build()
        );
        sendNotification(event.getFollowedId(), notification);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .processedAt(java.time.OffsetDateTime.now())
                .build());
    }

    private void sendNotification(UUID recipientId, Notification notification) {
        var response = notificationMapper.toResponse(notification);
        try {
            log.info("Notified {} user {}", response.getType(), response.getActorId());
            template.convertAndSendToUser(recipientId.toString(), NOTIFICATION_SUBJECT, response);
            int unreadCount = notificationRepository.countByUserIdAndIsSeen(recipientId, false);
            template.convertAndSendToUser(recipientId.toString(), UNREAD_COUNT, unreadCount);
        } catch (Exception e) {
            log.info("Failed to notify {} user {}", response.getType(), response.getActorId(), e);
        }
    }

    private NotificationType toNotificationType(EntityType entityType) {
        return switch (entityType) {
            case POST -> NotificationType.POST_REACTION;
            case COMMENT -> NotificationType.COMMENT_REACTION;
            case MESSAGE -> NotificationType.COMMENT;
            case USER -> NotificationType.FOLLOW_REQUEST;
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };
    }

    private UUID getRecipientId(NotificationType type, UUID contentId) {
        return switch (type) {
            case COMMENT, POST_REACTION -> getUserIdByPostId(contentId);
            case COMMENT_REPLY, COMMENT_REACTION -> getUserIdByCommentId(contentId);
            case FOLLOW_REQUEST -> contentId;
            default -> throw new RuntimeException("(!) implement mention feature");
        };
    }

    private UUID getUserIdByPostId(UUID postId) {
        return postPort.findPostOwnerId(postId);
    }

    private UUID getUserIdByCommentId(UUID commentId) {
        return commentPort.findCommentOwnerId(commentId);
    }
}
package dev.thural.quietspace.domain.notification;

import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.domain.notification.NotificationType;
import dev.thural.quietspace.domain.notification.port.NotificationCommentPort;
import dev.thural.quietspace.domain.notification.port.NotificationPostPort;
import dev.thural.quietspace.domain.notification.port.NotificationUserPort;
import dev.thural.quietspace.core.messaging.event.message.NotificationEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

import static dev.thural.quietspace.core.messaging.event.EventType.SEEN_NOTIFICATION;
import static dev.thural.quietspace.domain.notification.NotificationType.COMMENT_REACTION;
import static dev.thural.quietspace.domain.notification.NotificationType.POST_REACTION;
import static dev.thural.quietspace.core.shared.util.PagingProvider.DEFAULT_SORT_OPTION;
import static dev.thural.quietspace.core.shared.util.PagingProvider.buildPageRequest;
import static dev.thural.quietspace.core.messaging.constant.WebSocketPaths.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationUserPort userPort;
    private final NotificationCommentPort commentPort;
    private final NotificationPostPort postPort;
    private final SimpMessagingTemplate template;

    @Override
    @Transactional
    public void handleSeen(UUID notificationId) {
        log.info("setting notification with id {} as seen ...", notificationId);
        UUID userId = userPort.currentUserId();
        var notification = notificationRepository.findById(notificationId).orElseThrow(EntityNotFoundException::new);
        if (!notification.getUserId().equals(userId))
            throw new ResourceAccessException("denied access for requested resource");
        if (!notification.getIsSeen()) notification.setIsSeen(true);
        var event = NotificationEvent.builder()
                .notificationId(notificationId)
                .actorId(notification.getActorId())
                .recipientId(notification.getUserId())
                .type(SEEN_NOTIFICATION)
                .build();
        template.convertAndSendToUser(userId.toString(), NOTIFICATION_EVENT, event);
        int unreadCount = notificationRepository.countByUserIdAndIsSeen(userId, false);
        template.convertAndSendToUser(userId.toString(), UNREAD_COUNT, unreadCount);
    }

    @Override
    public Page<NotificationResponse> getAllNotifications(Integer pageNumber, Integer pageSize) {
        UUID signedUserId = userPort.currentUserId();
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return notificationRepository.findAllByUserId(signedUserId, pageRequest)
                .map(notificationMapper::toResponse);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByType(Integer pageNumber, Integer pageSize, String notificationType) {
        NotificationType type = NotificationType.valueOf(notificationType);
        UUID signedUserId = userPort.currentUserId();
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, DEFAULT_SORT_OPTION);
        return notificationRepository
                .findAllByUserIdAndNotificationType(signedUserId, type, pageRequest)
                .map(notificationMapper::toResponse);
    }

    @Override
    public Integer getCountOfPendingNotifications() {
        UUID signedUserId = userPort.currentUserId();
        return notificationRepository.countByUserIdAndIsSeen(signedUserId, false);
    }

    public void processNotification(NotificationType type, UUID contentId) {
        UUID signedUserId = userPort.currentUserId();
        UUID recipientId = getRecipientId(type, contentId);
        userPort.findUsernameById(recipientId);
        var notification = notificationRepository.save(
                Notification
                        .builder()
                        .notificationType(type)
                        .contentId(contentId)
                        .actorId(signedUserId)
                        .userId(recipientId)
                        .build()
        );
        var response = notificationMapper.toResponse(notification);
        try {
            log.info("notified {} user {}", response.getType(), response.getActorId());
            template.convertAndSendToUser(recipientId.toString(), NOTIFICATION_SUBJECT, response);
            int unreadCount = notificationRepository.countByUserIdAndIsSeen(recipientId, false);
            template.convertAndSendToUser(recipientId.toString(), UNREAD_COUNT, unreadCount);
        } catch (MessagingException exception) {
            log.info("failed to notify {} user {}", response.getType(), response.getActorId());
        }
    }

    public void processNotificationByReaction(EntityType type, UUID contentId) {
        switch (type) {
            case COMMENT -> processNotification(COMMENT_REACTION, contentId);
            case POST -> processNotification(POST_REACTION, contentId);
            default -> throw new IllegalArgumentException("Unknown entity type: " + type);
        }
    }

    private UUID getRecipientId(NotificationType type, UUID contentId) {
        return switch (type) {
            case COMMENT, REPOST, POST_REACTION -> getUserIdByPostId(contentId);
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

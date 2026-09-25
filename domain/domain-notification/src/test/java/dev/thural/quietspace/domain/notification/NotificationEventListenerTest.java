package dev.thural.quietspace.domain.notification;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.core.shared.event.CommentCreatedEvent;
import dev.thural.quietspace.core.shared.event.MessageSentEvent;
import dev.thural.quietspace.core.shared.event.PostCreatedEvent;
import dev.thural.quietspace.core.shared.event.ProcessedEventRepository;
import dev.thural.quietspace.core.shared.event.ReactionAddedEvent;
import dev.thural.quietspace.core.shared.event.UserFollowedEvent;
import dev.thural.quietspace.core.shared.event.UserRegisteredEvent;
import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import dev.thural.quietspace.domain.notification.port.NotificationCommentPort;
import dev.thural.quietspace.domain.notification.port.NotificationPostPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationCommentPort commentPort;
    @Mock
    private NotificationPostPort postPort;
    @Mock
    private SimpMessagingTemplate template;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private NotificationEventListener listener;

    private Notification saved(String type) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .notificationType(NotificationType.valueOf(type))
                .build();
    }

    private void stubCommon(Notification notification) {
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class)))
                .thenReturn(new NotificationResponse());
    }

    @Test
    void onUserRegistered_shouldSaveWelcomeNotification() {
        var event = new UserRegisteredEvent(UUID.randomUUID(), "user", "u@x.com");
        stubCommon(saved("FOLLOW_REQUEST"));

        listener.onUserRegistered(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(processedEventRepository).save(any());
    }

    @Test
    void onUserRegistered_givenAlreadyProcessed_shouldSkip() {
        var event = new UserRegisteredEvent(UUID.randomUUID(), "user", "u@x.com");
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        listener.onUserRegistered(event);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void onPostCreated_shouldMarkProcessed() {
        var event = new PostCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "t", "b");
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);

        listener.onPostCreated(event);

        verify(processedEventRepository).save(any());
    }

    @Test
    void onCommentCreated_shouldNotifyPostOwner() {
        UUID postId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var event = new CommentCreatedEvent(UUID.randomUUID(), postId, UUID.randomUUID(), "nice");
        stubCommon(saved("COMMENT"));
        when(postPort.findPostOwnerId(postId)).thenReturn(ownerId);

        listener.onCommentCreated(event);

        verify(postPort, times(2)).findPostOwnerId(postId);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void onReactionAdded_givenPostReaction_shouldNotifyPostOwner() {
        UUID contentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var event = new ReactionAddedEvent(UUID.randomUUID(), contentId,
                EntityType.POST, ReactionType.LIKE, UUID.randomUUID());
        stubCommon(saved("POST_REACTION"));
        when(postPort.findPostOwnerId(contentId)).thenReturn(ownerId);

        listener.onReactionAdded(event);

        verify(postPort, times(2)).findPostOwnerId(contentId);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void onReactionAdded_givenCommentReaction_shouldNotifyCommentOwner() {
        UUID contentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        var event = new ReactionAddedEvent(UUID.randomUUID(), contentId,
                EntityType.COMMENT, ReactionType.LIKE, UUID.randomUUID());
        stubCommon(saved("COMMENT_REACTION"));
        when(commentPort.findCommentOwnerId(contentId)).thenReturn(ownerId);

        listener.onReactionAdded(event);

        verify(commentPort, times(2)).findCommentOwnerId(contentId);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void onMessageSent_shouldMarkProcessed() {
        var event = new MessageSentEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hi");
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);

        listener.onMessageSent(event);

        verify(processedEventRepository).save(any());
    }

    @Test
    void onUserFollowed_shouldSaveFollowNotification() {
        var event = new UserFollowedEvent(UUID.randomUUID(), UUID.randomUUID());
        stubCommon(saved("FOLLOW_REQUEST"));

        listener.onUserFollowed(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(processedEventRepository).save(any());
    }
}

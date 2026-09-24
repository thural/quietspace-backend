package dev.thural.quietspace.domain.notification;

import dev.thural.quietspace.domain.notification.Notification;
import dev.thural.quietspace.domain.notification.NotificationMapper;
import dev.thural.quietspace.domain.notification.NotificationRepository;
import dev.thural.quietspace.domain.notification.NotificationServiceImpl;
import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.domain.notification.port.NotificationCommentPort;
import dev.thural.quietspace.domain.notification.port.NotificationPostPort;
import dev.thural.quietspace.domain.notification.port.NotificationUserPort;
import dev.thural.quietspace.domain.notification.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.core.messaging.constant.WebSocketPaths.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationUserPort userPort;
    @Mock
    private NotificationCommentPort commentPort;
    @Mock
    private NotificationPostPort postPort;
    @Mock
    private SimpMessagingTemplate template;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID signedUserId;
    private Notification notification;
    private UUID notificationId;
    private NotificationResponse notificationResponse;
    private UUID postId;
    private UUID commentId;
    private UUID postOwnerId;
    private UUID commentOwnerId;

    @BeforeEach
    void setUp() {
        signedUserId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        postOwnerId = UUID.randomUUID();
        commentOwnerId = UUID.randomUUID();

        notification = Notification.builder()
                .id(notificationId)
                .userId(signedUserId)
                .actorId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .isSeen(false)
                .notificationType(NotificationType.FOLLOW_REQUEST)
                .build();

        notificationResponse = NotificationResponse.builder()
                .actorId(notification.getActorId())
                .contentId(notification.getContentId())
                .type(NotificationType.FOLLOW_REQUEST)
                .isSeen(false)
                .build();
    }

    @Test
    void handleSeen_givenOwnNotification_shouldMarkSeenAndSendEvent() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.countByUserIdAndIsSeen(signedUserId, false)).thenReturn(0);

        notificationService.handleSeen(notificationId);

        assertThat(notification.getIsSeen()).isTrue();
        verify(template).convertAndSendToUser(
                eq(signedUserId.toString()),
                eq(NOTIFICATION_EVENT),
                any()
        );
        verify(template).convertAndSendToUser(
                eq(signedUserId.toString()),
                eq(UNREAD_COUNT),
                eq(0)
        );
    }

    @Test
    void handleSeen_givenAlreadySeen_shouldNotDoubleSetAndStillSendEvent() {
        notification.setIsSeen(true);
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.countByUserIdAndIsSeen(signedUserId, false)).thenReturn(0);

        notificationService.handleSeen(notificationId);

        verify(template).convertAndSendToUser(
                eq(signedUserId.toString()),
                eq(NOTIFICATION_EVENT),
                any()
        );
        verify(template).convertAndSendToUser(
                eq(signedUserId.toString()),
                eq(UNREAD_COUNT),
                eq(0)
        );
    }

    @Test
    void handleSeen_givenOtherUsersNotification_shouldThrow() {
        Notification otherNotif = Notification.builder()
                .id(notificationId)
                .userId(UUID.randomUUID())
                .build();
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(otherNotif));

        assertThatThrownBy(() -> notificationService.handleSeen(notificationId))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("denied access");
    }

    @Test
    void handleSeen_givenNonExistentNotification_shouldThrow() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.handleSeen(notificationId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllNotifications_shouldReturnPage() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.findAllByUserId(eq(signedUserId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(notificationResponse);

        Page<NotificationResponse> result = notificationService.getAllNotifications(0, 25);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NotificationType.FOLLOW_REQUEST);
    }

    @Test
    void getNotificationsByType_givenValidType_shouldReturnFilteredPage() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.findAllByUserIdAndNotificationType(
                eq(signedUserId), eq(NotificationType.FOLLOW_REQUEST), any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(notificationResponse);

        Page<NotificationResponse> result = notificationService.getNotificationsByType(0, 25, "FOLLOW_REQUEST");

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getNotificationsByType_givenInvalidTypeString_shouldThrow() {
        assertThatThrownBy(() -> notificationService.getNotificationsByType(0, 25, "INVALID_TYPE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCountOfPendingNotifications_shouldReturnCount() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(notificationRepository.countByUserIdAndIsSeen(signedUserId, false)).thenReturn(5);

        Integer count = notificationService.getCountOfPendingNotifications();

        assertThat(count).isEqualTo(5);
    }

    @Test
    void processNotification_givenPostReaction_shouldCreateAndSend() {
        UUID recipientId = postOwnerId;
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(postPort.findPostOwnerId(postId)).thenReturn(postOwnerId);
        when(userPort.findUsernameById(recipientId)).thenReturn("postauthor");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        when(notificationRepository.countByUserIdAndIsSeen(recipientId, false)).thenReturn(1);

        notificationService.processNotification(NotificationType.POST_REACTION, postId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.POST_REACTION);
        assertThat(saved.getContentId()).isEqualTo(postId);
        assertThat(saved.getActorId()).isEqualTo(signedUserId);
        assertThat(saved.getUserId()).isEqualTo(recipientId);

        verify(template).convertAndSendToUser(
                eq(recipientId.toString()),
                eq(NOTIFICATION_SUBJECT),
                eq(notificationResponse)
        );
        verify(template).convertAndSendToUser(
                eq(recipientId.toString()),
                eq(UNREAD_COUNT),
                eq(1)
        );
    }

    @Test
    void processNotification_givenCommentReaction_shouldCreateAndSend() {
        UUID recipientId = commentOwnerId;
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(commentPort.findCommentOwnerId(commentId)).thenReturn(commentOwnerId);
        when(userPort.findUsernameById(recipientId)).thenReturn("commentauthor");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        when(notificationRepository.countByUserIdAndIsSeen(recipientId, false)).thenReturn(1);

        notificationService.processNotification(NotificationType.COMMENT_REACTION, commentId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(recipientId);

        verify(template).convertAndSendToUser(
                eq(recipientId.toString()),
                eq(UNREAD_COUNT),
                eq(1)
        );
    }

    @Test
    void processNotification_givenFollowRequest_shouldUseContentIdAsRecipient() {
        UUID contentId = UUID.randomUUID();
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(userPort.findUsernameById(contentId)).thenReturn("other");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        when(notificationRepository.countByUserIdAndIsSeen(contentId, false)).thenReturn(1);

        notificationService.processNotification(NotificationType.FOLLOW_REQUEST, contentId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(contentId);

        verify(template).convertAndSendToUser(
                eq(contentId.toString()),
                eq(UNREAD_COUNT),
                eq(1)
        );
    }

    @Test
    void processNotification_whenWebSocketFails_shouldLogAndSwallow() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(postPort.findPostOwnerId(postId)).thenReturn(postOwnerId);
        when(userPort.findUsernameById(any(UUID.class))).thenReturn("someone");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        doThrow(new MessagingException("websocket error"))
                .when(template).convertAndSendToUser(eq(postOwnerId.toString()), eq(NOTIFICATION_SUBJECT), any());

        assertDoesNotThrow(() ->
                notificationService.processNotification(NotificationType.POST_REACTION, postId)
        );
    }

    @Test
    void processNotificationByReaction_givenComment_shouldProcessCommentReaction() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(commentPort.findCommentOwnerId(commentId)).thenReturn(commentOwnerId);
        when(userPort.findUsernameById(any(UUID.class))).thenReturn("someone");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);

        notificationService.processNotificationByReaction(EntityType.COMMENT, commentId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.COMMENT_REACTION);
    }

    @Test
    void processNotificationByReaction_givenPost_shouldProcessPostReaction() {
        when(userPort.currentUserId()).thenReturn(signedUserId);
        when(postPort.findPostOwnerId(postId)).thenReturn(postOwnerId);
        when(userPort.findUsernameById(any(UUID.class))).thenReturn("someone");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);

        notificationService.processNotificationByReaction(EntityType.POST, postId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.POST_REACTION);
    }
}

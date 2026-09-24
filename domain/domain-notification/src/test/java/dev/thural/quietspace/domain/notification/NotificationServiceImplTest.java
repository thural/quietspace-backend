package dev.thural.quietspace.notification;

import dev.thural.quietspace.comment.Comment;
import dev.thural.quietspace.comment.CommentRepository;
import dev.thural.quietspace.notification.Notification;
import dev.thural.quietspace.notification.NotificationMapper;
import dev.thural.quietspace.notification.NotificationRepository;
import dev.thural.quietspace.notification.NotificationServiceImpl;
import dev.thural.quietspace.notification.dto.NotificationResponse;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.post.PostRepository;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.notification.NotificationType;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserService;
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

import static dev.thural.quietspace.websocket.constant.WebSocketPaths.*;
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
    private UserService userService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private SimpMessagingTemplate template;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User signedUser;
    private UUID signedUserId;
    private Notification notification;
    private UUID notificationId;
    private NotificationResponse notificationResponse;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        signedUserId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        signedUser = User.builder()
                .id(signedUserId)
                .username("testuser")
                .build();

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

        post = Post.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).username("postauthor").build())
                .build();

        comment = Comment.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).username("commentauthor").build())
                .build();
    }

    @Test
    void handleSeen_givenOwnNotification_shouldMarkSeenAndSendEvent() {
        when(userService.getSignedUser()).thenReturn(signedUser);
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
        when(userService.getSignedUser()).thenReturn(signedUser);
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
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(otherNotif));

        assertThatThrownBy(() -> notificationService.handleSeen(notificationId))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("denied access");
    }

    @Test
    void handleSeen_givenNonExistentNotification_shouldThrow() {
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.handleSeen(notificationId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllNotifications_shouldReturnPage() {
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(notificationRepository.findAllByUserId(eq(signedUserId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(notificationResponse);

        Page<NotificationResponse> result = notificationService.getAllNotifications(0, 25);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NotificationType.FOLLOW_REQUEST);
    }

    @Test
    void getNotificationsByType_givenValidType_shouldReturnFilteredPage() {
        when(userService.getSignedUser()).thenReturn(signedUser);
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
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(notificationRepository.countByUserIdAndIsSeen(signedUserId, false)).thenReturn(5);

        Integer count = notificationService.getCountOfPendingNotifications();

        assertThat(count).isEqualTo(5);
    }

    @Test
    void processNotification_givenPostReaction_shouldCreateAndSend() {
        UUID recipientId = post.getUser().getId();
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(userService.getUserById(recipientId)).thenReturn(Optional.of(post.getUser()));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        when(notificationRepository.countByUserIdAndIsSeen(recipientId, false)).thenReturn(1);

        notificationService.processNotification(NotificationType.POST_REACTION, post.getId());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.POST_REACTION);
        assertThat(saved.getContentId()).isEqualTo(post.getId());
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
        UUID recipientId = comment.getUser().getId();
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userService.getUserById(recipientId)).thenReturn(Optional.of(comment.getUser()));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        when(notificationRepository.countByUserIdAndIsSeen(recipientId, false)).thenReturn(1);

        notificationService.processNotification(NotificationType.COMMENT_REACTION, comment.getId());

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
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(userService.getUserById(contentId)).thenReturn(Optional.of(User.builder().id(contentId).username("other").build()));
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
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(userService.getUserById(any(UUID.class))).thenReturn(Optional.of(post.getUser()));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);
        doThrow(new MessagingException("websocket error"))
                .when(template).convertAndSendToUser(eq(post.getUser().getId().toString()), eq(NOTIFICATION_SUBJECT), any());

        assertDoesNotThrow(() ->
                notificationService.processNotification(NotificationType.POST_REACTION, post.getId())
        );
    }

    @Test
    void processNotificationByReaction_givenComment_shouldProcessCommentReaction() {
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(userService.getUserById(any(UUID.class))).thenReturn(Optional.of(comment.getUser()));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);

        notificationService.processNotificationByReaction(EntityType.COMMENT, comment.getId());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.COMMENT_REACTION);
    }

    @Test
    void processNotificationByReaction_givenPost_shouldProcessPostReaction() {
        when(userService.getSignedUser()).thenReturn(signedUser);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(userService.getUserById(any(UUID.class))).thenReturn(Optional.of(post.getUser()));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(notificationResponse);

        notificationService.processNotificationByReaction(EntityType.POST, post.getId());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.POST_REACTION);
    }
}

package dev.thural.quietspace.notification;

import dev.thural.quietspace.notification.Notification;
import dev.thural.quietspace.notification.NotificationMapper;
import dev.thural.quietspace.notification.dto.NotificationResponse;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.notification.NotificationType;
import dev.thural.quietspace.shared.exception.UserNotFoundException;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationMapper notificationMapper;

    private Notification notification;
    private User actor;
    private UUID notificationId;
    private UUID userId;
    private UUID actorId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        notificationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        contentId = UUID.randomUUID();

        actor = User.builder()
                .id(actorId)
                .username("actor")
                .email("actor@test.com")
                .build();

        notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .actorId(actorId)
                .contentId(contentId)
                .isSeen(false)
                .contentType(EntityType.POST)
                .notificationType(NotificationType.POST_REACTION)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    @Test
    void toResponse_shouldConvertNotificationToResponse() {
        // Given
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notification.getId());
        assertThat(result.getActorId()).isEqualTo(actor.getId());
        assertThat(result.getContentId()).isEqualTo(notification.getContentId());
        assertThat(result.getIsSeen()).isEqualTo(notification.getIsSeen());
        assertThat(result.getType()).isEqualTo(notification.getNotificationType());
        assertThat(result.getCreateDate()).isEqualTo(notification.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(notification.getUpdateDate());

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldThrowExceptionWhenActorNotFound() {
        // Given
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationMapper.toResponse(notification))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleDifferentNotificationTypes() {
        // Given
        notification.setNotificationType(NotificationType.COMMENT);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.COMMENT);

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleDifferentEntityTypes() {
        // Given
        notification.setContentType(EntityType.COMMENT);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        // ContentType is not directly mapped to response, but should be copied by BeanUtils
        // The response focuses on the notification type

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleSeenNotification() {
        // Given
        notification.setIsSeen(true);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsSeen()).isTrue();

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleUnseenNotification() {
        // Given
        notification.setIsSeen(false);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsSeen()).isFalse();

        verify(userRepository).findById(actorId);
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void toResponse_shouldHandleAllNotificationTypes(NotificationType type) {
        notification.setNotificationType(type);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        NotificationResponse result = notificationMapper.toResponse(notification);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(type);
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void toResponse_shouldHandleAllEntityTypes(EntityType type) {
        notification.setContentType(type);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        NotificationResponse result = notificationMapper.toResponse(notification);

        assertThat(result).isNotNull();
    }

    @Test
    void toResponse_shouldHandleNullFields() {
        // Given
        notification.setContentType(null);
        notification.setNotificationType(null);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isNull();

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldCopyAllEntityFields() {
        // Given
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notification.getId());
        assertThat(result.getActorId()).isEqualTo(actor.getId());
        assertThat(result.getContentId()).isEqualTo(notification.getContentId());
        assertThat(result.getIsSeen()).isEqualTo(notification.getIsSeen());
        assertThat(result.getCreateDate()).isEqualTo(notification.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(notification.getUpdateDate());
        // BeanUtils.copyProperties should copy all matching fields

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleFollowNotification() {
        // Given
        notification.setNotificationType(NotificationType.FOLLOW_REQUEST);
        notification.setContentType(EntityType.USER);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.FOLLOW_REQUEST);

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleMessageNotification() {
        // Given
        notification.setNotificationType(NotificationType.MENTION);
        notification.setContentType(EntityType.MESSAGE);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.MENTION);

        verify(userRepository).findById(actorId);
    }

    @Test
    void toResponse_shouldHandleReactionNotification() {
        // Given
        notification.setNotificationType(NotificationType.COMMENT_REACTION);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.COMMENT_REACTION);

        verify(userRepository).findById(actorId);
    }
}

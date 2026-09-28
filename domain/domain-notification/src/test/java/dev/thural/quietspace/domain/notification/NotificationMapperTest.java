package dev.thural.quietspace.domain.notification;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import dev.thural.quietspace.domain.notification.port.NotificationUserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationMapperTest {

    @Mock
    private NotificationUserPort userPort;

    @InjectMocks
    private NotificationMapper notificationMapper;

    private Notification notification;
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
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notification.getId());
        assertThat(result.getActorId()).isEqualTo(actorId);
        assertThat(result.getContentId()).isEqualTo(notification.getContentId());
        assertThat(result.getIsSeen()).isEqualTo(notification.getIsSeen());
        assertThat(result.getType()).isEqualTo(notification.getNotificationType());
        assertThat(result.getCreateDate()).isEqualTo(notification.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(notification.getUpdateDate());

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldThrowExceptionWhenActorNotFound() {
        // Given
        when(userPort.findUsernameById(actorId)).thenThrow(new UserNotFoundException());

        // When & Then
        assertThatThrownBy(() -> notificationMapper.toResponse(notification))
                .isInstanceOf(UserNotFoundException.class);

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleDifferentNotificationTypes() {
        // Given
        notification.setNotificationType(NotificationType.COMMENT);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.COMMENT);

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleDifferentEntityTypes() {
        // Given
        notification.setContentType(EntityType.COMMENT);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        // ContentType is not directly mapped to response, but should be copied by BeanUtils
        // The response focuses on the notification type

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleSeenNotification() {
        // Given
        notification.setIsSeen(true);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsSeen()).isTrue();

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleUnseenNotification() {
        // Given
        notification.setIsSeen(false);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsSeen()).isFalse();

        verify(userPort).findUsernameById(actorId);
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void toResponse_shouldHandleAllNotificationTypes(NotificationType type) {
        notification.setNotificationType(type);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        NotificationResponse result = notificationMapper.toResponse(notification);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(type);
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void toResponse_shouldHandleAllEntityTypes(EntityType type) {
        notification.setContentType(type);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        NotificationResponse result = notificationMapper.toResponse(notification);

        assertThat(result).isNotNull();
    }

    @Test
    void toResponse_shouldHandleNullFields() {
        // Given
        notification.setContentType(null);
        notification.setNotificationType(null);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isNull();

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldCopyAllEntityFields() {
        // Given
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(notification.getId());
        assertThat(result.getActorId()).isEqualTo(actorId);
        assertThat(result.getContentId()).isEqualTo(notification.getContentId());
        assertThat(result.getIsSeen()).isEqualTo(notification.getIsSeen());
        assertThat(result.getCreateDate()).isEqualTo(notification.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(notification.getUpdateDate());
        // BeanUtils.copyProperties should copy all matching fields

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleFollowNotification() {
        // Given
        notification.setNotificationType(NotificationType.FOLLOW_REQUEST);
        notification.setContentType(EntityType.USER);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.FOLLOW_REQUEST);

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleMessageNotification() {
        // Given
        notification.setNotificationType(NotificationType.MENTION);
        notification.setContentType(EntityType.MESSAGE);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.MENTION);

        verify(userPort).findUsernameById(actorId);
    }

    @Test
    void toResponse_shouldHandleReactionNotification() {
        // Given
        notification.setNotificationType(NotificationType.COMMENT_REACTION);
        when(userPort.findUsernameById(actorId)).thenReturn("actor");

        // When
        NotificationResponse result = notificationMapper.toResponse(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.COMMENT_REACTION);

        verify(userPort).findUsernameById(actorId);
    }
}

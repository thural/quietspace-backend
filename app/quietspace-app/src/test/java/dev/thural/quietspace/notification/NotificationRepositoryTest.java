package dev.thural.quietspace.notification;

import dev.thural.quietspace.domain.notification.Notification;
import dev.thural.quietspace.domain.notification.NotificationRepository;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.domain.notification.NotificationType;
import dev.thural.quietspace.core.shared.enums.Role;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private Notification savedNotification;
    private final UUID actorId = UUID.randomUUID();
    private final UUID contentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("notifuser@email.com")
                .username("notifuser")
                .firstname("firstname")
                .lastname("lastname")
                .password("password123")
                .accountLocked(false)
                .role(Role.USER)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        savedUser = userRepository.save(user);

        Notification notification = Notification.builder()
                .userId(savedUser.getId())
                .actorId(actorId)
                .contentId(contentId)
                .isSeen(false)
                .contentType(EntityType.POST)
                .notificationType(NotificationType.POST_REACTION)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        savedNotification = notificationRepository.save(notification);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.delete(savedNotification);
        userRepository.delete(savedUser);
    }

    @Test
    void findAllByUserId_shouldReturnNotifications() {
        Page<Notification> notifications = notificationRepository.findAllByUserId(savedUser.getId(), null);
        assertThat(notifications.toList()).hasSize(1);
        assertThat(notifications.toList().get(0).getId()).isEqualTo(savedNotification.getId());
    }

    @Test
    void findAllByUserIdAndNotificationType_shouldFilterByType() {
        Page<Notification> notifications = notificationRepository
                .findAllByUserIdAndNotificationType(savedUser.getId(), NotificationType.POST_REACTION, null);
        assertThat(notifications.toList()).hasSize(1);
        assertThat(notifications.toList().get(0).getNotificationType())
                .isEqualTo(NotificationType.POST_REACTION);
    }

    @Test
    void findByContentIdAndUserId_shouldReturnNotification() {
        Optional<Notification> notification = notificationRepository
                .findByContentIdAndUserId(contentId, savedUser.getId());
        assertThat(notification).isPresent();
        assertThat(notification.get().getContentId()).isEqualTo(contentId);
    }

    @Test
    void findByContentIdAndUserId_givenNonExistent_shouldReturnEmpty() {
        Optional<Notification> notification = notificationRepository
                .findByContentIdAndUserId(UUID.randomUUID(), savedUser.getId());
        assertThat(notification).isEmpty();
    }

    @Test
    void countByUserIdAndIsSeen_shouldReturnCount() {
        Integer count = notificationRepository.countByUserIdAndIsSeen(savedUser.getId(), false);
        assertThat(count).isEqualTo(1);
    }
}

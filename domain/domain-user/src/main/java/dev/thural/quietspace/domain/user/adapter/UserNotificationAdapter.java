package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.domain.notification.port.NotificationUserPort;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * User-domain implementation of the notification-owned {@link NotificationUserPort}.
 */
@Component
@RequiredArgsConstructor
public class UserNotificationAdapter implements NotificationUserPort {

    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public UUID currentUserId() {
        return userService.getSignedUser().getId();
    }

    @Override
    public String findUsernameById(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElseThrow(UserNotFoundException::new);
    }
}

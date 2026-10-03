package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.security.port.CurrentUserPort;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.notification.port.NotificationUserPort;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * User-domain implementation of the notification-owned {@link NotificationUserPort}.
 */
@Component
@RequiredArgsConstructor
public class UserNotificationAdapter implements NotificationUserPort {

    private final CurrentUserPort currentUserPort;
    private final UserRepository userRepository;

    @Override
    public UUID currentUserId() {
        return currentUserPort.currentUserId();
    }

    @Override
    public String findUsernameById(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElseThrow(UserNotFoundException::new);
    }
}

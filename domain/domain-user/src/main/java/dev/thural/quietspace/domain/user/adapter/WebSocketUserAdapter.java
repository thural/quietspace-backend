package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import dev.thural.quietspace.core.shared.enums.StatusType;
import dev.thural.quietspace.core.shared.ports.WebSocketUserPort;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * User-domain implementation of the shared-kernel {@link WebSocketUserPort}.
 *
 * <p>Lives in domain-user (the provider) so core WebSocket infrastructure
 * never depends on the user domain.</p>
 */
@Component
@RequiredArgsConstructor
public class WebSocketUserAdapter implements WebSocketUserPort {

    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public UUID currentUserId() {
        return userService.getSignedUser().getId();
    }

    @Override
    public UUID userIdForUsername(String username) {
        return userRepository.findUserByUsername(username)
                .map(BaseEntity::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public UserDetails userForId(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> (UserDetails) user)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
    }

    @Override
    public void setOnlineStatus(String username, StatusType status) {
        userService.setOnlineStatus(username, status);
    }
}

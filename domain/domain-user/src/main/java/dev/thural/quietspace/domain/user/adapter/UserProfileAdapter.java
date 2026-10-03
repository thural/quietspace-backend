package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.security.port.CurrentUserPort;
import dev.thural.quietspace.core.shared.ports.UserProfilePort;
import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User-domain implementation of the shared-kernel {@link UserProfilePort}.
 *
 * <p>Lives in domain-user (the provider) so modules like domain-photo can
 * resolve and mutate user-profile state without depending on this module.</p>
 */
@Component
@RequiredArgsConstructor
public class UserProfileAdapter implements UserProfilePort {

    private final CurrentUserPort currentUserPort;
    private final UserRepository userRepository;

    @Override
    public UUID currentUserId() {
        return currentUserPort.currentUserId();
    }

    @Override
    @Transactional
    public void setProfilePhoto(UUID userId, UUID photoId) {
        userRepository.findById(userId).ifPresent(user -> user.setPhotoId(photoId));
    }
}

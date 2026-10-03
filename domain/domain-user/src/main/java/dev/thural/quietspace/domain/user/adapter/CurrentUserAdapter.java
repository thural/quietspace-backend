package dev.thural.quietspace.domain.user.adapter;

import dev.thural.quietspace.core.security.port.CurrentUserPort;
import dev.thural.quietspace.core.shared.exception.UnauthenticatedException;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * User-domain implementation of the core {@link CurrentUserPort}.
 *
 * <p>Resolves the current user id from the Spring Security context (populated
 * by {@code core-security}'s filter chain with a {@code UserDetails}
 * principal) via username lookup. Core stays domain-agnostic: it owns the
 * port, this adapter provides the user-aware resolution.</p>
 */
@Component
@RequiredArgsConstructor
public class CurrentUserAdapter implements CurrentUserPort {

    private final UserRepository userRepository;

    @Override
    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthenticatedException("No authenticated user in security context");
        }
        String username = authentication.getName();
        return userRepository.findUserByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(UserNotFoundException::new);
    }
}

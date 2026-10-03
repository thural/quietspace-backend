package dev.thural.quietspace.core.security.adapter;

import dev.thural.quietspace.core.security.port.CurrentUserPort;
import dev.thural.quietspace.core.shared.exception.UnauthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    @Override
    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthenticatedException("No authenticated user in security context");
        }
        if (authentication.getPrincipal() instanceof UUID) {
            return (UUID) authentication.getPrincipal();
        }
        throw new UnauthenticatedException("Authentication principal is not a UUID");
    }
}
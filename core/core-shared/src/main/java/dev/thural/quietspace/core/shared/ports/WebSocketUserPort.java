package dev.thural.quietspace.core.shared.ports;

import dev.thural.quietspace.core.shared.enums.StatusType;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

/**
 * Driven port decoupling core WebSocket infrastructure from domain user management.
 *
 * <p>Core modules must not depend on domain modules. This interface is owned by
 * core-shared and implemented by the user domain, following the dependency-inversion
 * principle: core defines the contract, domain provides the adapter.</p>
 */
public interface WebSocketUserPort {

    /**
     * Resolve the id of the currently authenticated (signed-in) user.
     *
     * @return id of the signed-in user
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if no user is signed in
     */
    UUID currentUserId();

    /**
     * Resolve a user id by username.
     *
     * @param username the username to look up
     * @return the user id
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if unknown
     */
    UUID userIdForUsername(String username);

    /**
     * Load user details by user id.
     *
     * @param userId the user id
     * @return user details, never {@code null}
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if unknown
     */
    UserDetails userForId(UUID userId);

    /**
     * Update the online presence of a user.
     *
     * @param username the username (or principal name) whose presence changed
     * @param status   the new presence status
     */
    void setOnlineStatus(String username, StatusType status);
}

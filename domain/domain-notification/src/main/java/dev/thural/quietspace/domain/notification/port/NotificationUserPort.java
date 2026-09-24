package dev.thural.quietspace.domain.notification.port;

import java.util.UUID;

/**
 * Driven port decoupling domain-notification from domain-user.
 *
 * <p>Defined by the consumer (domain-notification), implemented by the user
 * domain. Keeps the module dependency pointing a single way: user &rarr; notification.</p>
 */
public interface NotificationUserPort {

    /**
     * Resolve the id of the currently authenticated (signed-in) user.
     *
     * @return id of the signed-in user
     */
    UUID currentUserId();

    /**
     * Resolve a username by user id, validating that the user exists.
     *
     * @param userId the user id
     * @return the username
     * @throws dev.thural.quietspace.core.shared.exception.UserNotFoundException if unknown
     */
    String findUsernameById(UUID userId);
}

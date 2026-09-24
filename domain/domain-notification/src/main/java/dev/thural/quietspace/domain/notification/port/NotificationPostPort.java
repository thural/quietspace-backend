package dev.thural.quietspace.domain.notification.port;

import java.util.UUID;

/**
 * Driven port decoupling domain-notification from domain-post.
 *
 * <p>Defined by the consumer (domain-notification), implemented by the post
 * domain. Keeps the module dependency pointing a single way: post &rarr; notification.</p>
 */
public interface NotificationPostPort {

    /**
     * Resolve the owning author's user id for a post.
     *
     * @param postId the post id
     * @return the author's user id
     * @throws jakarta.persistence.EntityNotFoundException if unknown
     */
    UUID findPostOwnerId(UUID postId);
}

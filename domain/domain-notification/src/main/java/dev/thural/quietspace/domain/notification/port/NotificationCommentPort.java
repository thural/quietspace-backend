package dev.thural.quietspace.domain.notification.port;

import java.util.UUID;

/**
 * Driven port decoupling domain-notification from domain-comment.
 *
 * <p>Defined by the consumer (domain-notification), implemented by the comment
 * domain. Keeps the module dependency pointing a single way: comment &rarr; notification.</p>
 */
public interface NotificationCommentPort {

    /**
     * Resolve the owning author's user id for a comment.
     *
     * @param commentId the comment id
     * @return the author's user id
     * @throws jakarta.persistence.EntityNotFoundException if unknown
     */
    UUID findCommentOwnerId(UUID commentId);
}

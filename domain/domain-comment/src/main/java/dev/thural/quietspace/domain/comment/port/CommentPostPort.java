package dev.thural.quietspace.domain.comment.port;

import java.util.UUID;

/**
 * Driven port decoupling domain-comment from domain-post.
 *
 * <p>Defined by the consumer (domain-comment), implemented by the post domain.
 * Keeps the module dependency pointing a single way: post &rarr; comment.</p>
 */
public interface CommentPostPort {

    /**
     * Check whether a post exists.
     *
     * @param postId the post id
     * @return {@code true} when a post with the given id exists
     */
    boolean postExists(UUID postId);
}

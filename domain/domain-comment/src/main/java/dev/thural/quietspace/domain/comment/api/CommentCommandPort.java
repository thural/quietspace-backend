package dev.thural.quietspace.domain.comment.api;

import java.util.UUID;

/**
 * Inbound command port owned by the comment domain.
 *
 * <p>Lifecycle operations other aggregates must trigger on comments
 * (post deletion cascade). Callers invoke it inside their own transaction —
 * comment cleanup is a referential-integrity guarantee, not an async side effect.</p>
 */
public interface CommentCommandPort {

    /**
     * Delete all comments on the post. Must be called before the post row itself
     * is removed, within the caller's transaction.
     */
    void deleteAllByPostId(UUID postId);
}

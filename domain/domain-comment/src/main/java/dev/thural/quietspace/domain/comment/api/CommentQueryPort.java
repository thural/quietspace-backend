package dev.thural.quietspace.domain.comment.api;

import dev.thural.quietspace.domain.comment.api.dto.CommentSummaryDTO;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound query port owned by the comment domain.
 *
 * <p>Cross-domain read access (comment counts, latest comments on posts) goes
 * through this port — never through {@code CommentRepository} directly.</p>
 */
public interface CommentQueryPort {

    /**
     * @return snapshot, or empty if no comment with the id exists
     */
    Optional<CommentSummaryDTO> getCommentSummary(UUID commentId);

    /**
     * Batch variant to avoid N+1 lookups when rendering lists.
     *
     * @return snapshots keyed by comment id; missing ids are absent from the map
     */
    Map<UUID, CommentSummaryDTO> getCommentsSummary(Set<UUID> commentIds);

    /**
     * @return number of comments on the post
     */
    long countCommentsByPostId(UUID postId);
}

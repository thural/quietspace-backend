package dev.thural.quietspace.domain.post.api;

import dev.thural.quietspace.domain.post.api.dto.PostSummaryDTO;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound query port owned by the post domain.
 *
 * <p>Cross-domain read access (comment threads, reaction targets, repost
 * references) goes through this port — never through {@code PostRepository}
 * directly.</p>
 */
public interface PostQueryPort {

    /**
     * @return snapshot, or empty if no post with the id exists
     */
    Optional<PostSummaryDTO> getPostSummary(UUID postId);

    /**
     * Batch variant to avoid N+1 lookups when rendering lists.
     *
     * @return snapshots keyed by post id; missing ids are absent from the map
     */
    Map<UUID, PostSummaryDTO> getPostsSummary(Set<UUID> postIds);
}

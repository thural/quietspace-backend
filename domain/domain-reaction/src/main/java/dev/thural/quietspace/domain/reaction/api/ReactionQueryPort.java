package dev.thural.quietspace.domain.reaction.api;

import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.reaction.api.dto.ReactionSummaryDTO;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound query port owned by the reaction domain.
 *
 * <p>Cross-domain read access (reaction counts and user reactions on posts,
 * comments, messages) goes through this port — never through
 * {@code ReactionRepository} directly.</p>
 */
public interface ReactionQueryPort {

    /**
     * @return snapshot, or empty if no reaction with the id exists
     */
    Optional<ReactionSummaryDTO> getReactionSummary(UUID reactionId);

    /**
     * Batch variant to avoid N+1 lookups when rendering lists.
     *
     * @return snapshots keyed by reaction id; missing ids are absent from the map
     */
    Map<UUID, ReactionSummaryDTO> getReactionsSummary(Set<UUID> reactionIds);

    /**
     * @return number of reactions of the given type on the content
     */
    long countReactions(UUID contentId, ReactionType reactionType);
}

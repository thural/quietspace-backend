package dev.thural.quietspace.domain.reaction.api.dto;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;

import java.util.UUID;

/**
 * Public read-model contract of the reaction domain for cross-domain consumers.
 *
 * <p>Own-aggregate snapshot. Engagement rollups (like/dislike counts on posts
 * and comments) are served via {@code ReactionQueryPort}.</p>
 */
public record ReactionSummaryDTO(
        UUID id,
        UUID userId,
        String username,
        UUID contentId,
        EntityType contentType,
        ReactionType reactionType
) {
}

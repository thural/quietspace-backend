package dev.thural.quietspace.domain.post.api.dto;

import java.util.UUID;

/**
 * Public read-model contract of the post domain for cross-domain consumers.
 *
 * <p>Own-aggregate snapshot (author, content, attachment reference).
 * Engagement counts live in their owning domains ({@code CommentQueryPort},
 * {@code ReactionQueryPort}).</p>
 */
public record PostSummaryDTO(
        UUID id,
        UUID authorId,
        String title,
        String text,
        UUID photoId
) {
}

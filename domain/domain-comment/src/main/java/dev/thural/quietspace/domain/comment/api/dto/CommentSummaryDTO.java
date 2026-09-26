package dev.thural.quietspace.domain.comment.api.dto;

import java.util.UUID;

/**
 * Public read-model contract of the comment domain for cross-domain consumers.
 *
 * <p>Own-aggregate snapshot. Thread assembly stays in the comment domain via
 * {@code CommentService}.</p>
 */
public record CommentSummaryDTO(
        UUID id,
        UUID postId,
        UUID authorId,
        UUID parentId,
        String text
) {
}

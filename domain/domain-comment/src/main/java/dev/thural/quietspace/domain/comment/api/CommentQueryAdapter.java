package dev.thural.quietspace.domain.comment.api;

import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.domain.comment.CommentRepository;
import dev.thural.quietspace.domain.comment.api.dto.CommentSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default {@link CommentQueryPort} implementation backed by {@link CommentRepository}.
 */
@Component
@RequiredArgsConstructor
public class CommentQueryAdapter implements CommentQueryPort {

    private final CommentRepository commentRepository;

    @Override
    public Optional<CommentSummaryDTO> getCommentSummary(UUID commentId) {
        return commentRepository.findById(commentId).map(this::toSummary);
    }

    @Override
    public Map<UUID, CommentSummaryDTO> getCommentsSummary(Set<UUID> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentRepository.findAllById(commentIds).stream()
                .collect(Collectors.toMap(Comment::getId, this::toSummary));
    }

    @Override
    public long countCommentsByPostId(UUID postId) {
        return commentRepository.findAllByPostId(postId, Pageable.unpaged()).getTotalElements();
    }

    private CommentSummaryDTO toSummary(Comment comment) {
        return new CommentSummaryDTO(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                comment.getParentId(),
                comment.getText()
        );
    }
}

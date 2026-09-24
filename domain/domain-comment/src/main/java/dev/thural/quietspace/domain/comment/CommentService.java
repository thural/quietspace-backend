package dev.thural.quietspace.comment;

import dev.thural.quietspace.comment.dto.CommentRequest;
import dev.thural.quietspace.comment.dto.CommentResponse;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface CommentService {

    Page<CommentResponse> getCommentsByPostId(UUID postId, Integer pageNumber, Integer pageSize);

    CommentResponse createComment(CommentRequest comment);

    Optional<CommentResponse> getCommentById(UUID id);

    CommentResponse updateComment(UUID commentId, CommentRequest comment);

    void deleteComment(UUID id);

    Page<CommentResponse> getRepliesByParentId(UUID commentId, Integer pageNumber, Integer pageSize);

    CommentResponse patchComment(UUID id, CommentRequest comment);

    Page<CommentResponse> getCommentsByUserId(UUID userId, Integer pageNumber, Integer pageSize);

    Optional<CommentResponse> getLatestCommentByUserIdAndPostId(UUID userId, UUID postId);
}

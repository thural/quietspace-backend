package dev.thural.quietspace.domain.comment;

import dev.thural.quietspace.domain.comment.dto.CommentRequest;
import dev.thural.quietspace.domain.comment.dto.CommentResponse;
import dev.thural.quietspace.domain.reaction.ReactionService;
import dev.thural.quietspace.domain.reaction.api.ReactionQueryPort;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static dev.thural.quietspace.core.shared.enums.ReactionType.LIKE;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final CommentRepository commentRepository;
    private final ReactionQueryPort reactionQueryPort;
    private final ReactionService reactionService;
    private final UserQueryPort userQueryPort;


    public Comment commentRequestToEntity(CommentRequest comment) {
        return Comment.builder()
                .parentId(comment.getParentId())
                .text(comment.getText())
                .userId(comment.getUserId())
                .postId(comment.getPostId())
                .build();
    }

    public CommentResponse commentEntityToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .parentId(comment.getParentId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .username(resolveUsername(comment.getUserId()))
                .text(comment.getText())
                .userReaction(getUserReaction(comment.getId()))
                .createDate(comment.getCreateDate())
                .updateDate(comment.getUpdateDate())
                .likeCount(getLikeCount(comment.getId()))
                .replyCount(getReplyCount(comment.getId(), comment.getPostId()))
                .build();
    }

    private String resolveUsername(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userQueryPort.getUserSummary(userId).map(UserSummaryDTO::username).orElse(null);
    }

    private Integer getReplyCount(UUID parentId, UUID postId) {
        return commentRepository.countByParentIdAndPostId(parentId, postId);
    }

    private Integer getLikeCount(UUID commentId) {
        return Math.toIntExact(reactionQueryPort.countReactions(commentId, LIKE));
    }

    private ReactionResponse getUserReaction(UUID commentId) {
        return reactionService.getUserReactionByContentId(commentId).orElse(null);
    }

}

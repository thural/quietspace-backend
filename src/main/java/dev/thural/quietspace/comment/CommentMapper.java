package dev.thural.quietspace.comment;

import dev.thural.quietspace.comment.dto.CommentRequest;
import dev.thural.quietspace.comment.dto.CommentResponse;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.post.PostRepository;
import dev.thural.quietspace.reaction.ReactionRepository;
import dev.thural.quietspace.reaction.ReactionService;
import dev.thural.quietspace.reaction.dto.ReactionResponse;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static dev.thural.quietspace.reaction.ReactionType.LIKE;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final ReactionService reactionService;


    public Comment commentRequestToEntity(CommentRequest comment) {
        return Comment.builder()
                .parentId(comment.getParentId())
                .text(comment.getText())
                .user(getUserById(comment.getUserId()))
                .post(getPostById(comment.getPostId()))
                .build();
    }

    public CommentResponse commentEntityToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .parentId(comment.getParentId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .text(comment.getText())
                .userReaction(getUserReaction(comment.getId()))
                .createDate(comment.getCreateDate())
                .updateDate(comment.getUpdateDate())
                .likeCount(getLikeCount(comment.getId()))
                .replyCount(getReplyCount(comment.getId(), comment.getPost()))
                .build();
    }

    private Post getPostById(UUID postId) {
        return postRepository.findById(postId).orElse(null);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }

    private Integer getReplyCount(UUID parentId, Post post) {
        return commentRepository.countByParentIdAndPost(parentId, post);
    }

    private Integer getLikeCount(UUID commentId) {
        return reactionRepository.countByContentIdAndReactionType(commentId, LIKE);
    }

    private ReactionResponse getUserReaction(UUID commentId) {
        return reactionService.getUserReactionByContentId(commentId).orElse(null);
    }

}

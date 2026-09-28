package dev.thural.quietspace.domain.comment;

import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.domain.comment.dto.CommentRequest;
import dev.thural.quietspace.domain.comment.dto.CommentResponse;
import dev.thural.quietspace.domain.reaction.ReactionService;
import dev.thural.quietspace.domain.reaction.api.ReactionQueryPort;
import dev.thural.quietspace.domain.reaction.dto.ReactionResponse;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.api.UserQueryPort;
import dev.thural.quietspace.domain.user.api.dto.UserSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentMapperTest {

    @Mock
    private UserQueryPort userQueryPort;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReactionQueryPort reactionQueryPort;

    @Mock
    private ReactionService reactionService;

    @InjectMocks
    private CommentMapper commentMapper;

    private CommentRequest commentRequest;
    private Comment comment;
    private User user;
    private ReactionResponse userReaction;
    private UUID userId;
    private UUID postId;
    private UUID commentId;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        parentId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@test.com")
                .build();

        commentRequest = CommentRequest.builder()
                .userId(userId)
                .postId(postId)
                .parentId(parentId)
                .text("This is a test comment")
                .build();

        comment = Comment.builder()
                .id(commentId)
                .parentId(parentId)
                .text("This is a test comment")
                .userId(userId)
                .postId(postId)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();

        userReaction = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .reactionType(ReactionType.LIKE)
                .build();
    }

    @Test
    void commentRequestToEntity_shouldConvertRequestToEntity() {
        // When
        Comment result = commentMapper.commentRequestToEntity(commentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(commentRequest.getParentId());
        assertThat(result.getText()).isEqualTo(commentRequest.getText());
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPostId()).isEqualTo(postId);
    }

    @Test
    void commentRequestToEntity_shouldHandleNullParentId() {
        // Given
        CommentRequest requestWithoutParent = CommentRequest.builder()
                .userId(userId)
                .postId(postId)
                .parentId(null)
                .text("This is a test comment")
                .build();

        // When
        Comment result = commentMapper.commentRequestToEntity(requestWithoutParent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isNull();
        assertThat(result.getText()).isEqualTo(requestWithoutParent.getText());
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPostId()).isEqualTo(postId);
    }

    @Test
    void commentRequestToEntity_shouldSetUserIdDirectlyWithoutLookup() {
        // When
        Comment result = commentMapper.commentRequestToEntity(commentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPostId()).isEqualTo(postId);
    }

    @Test
    void commentRequestToEntity_shouldSetPostIdDirectly() {
        // When
        Comment result = commentMapper.commentRequestToEntity(commentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getPostId()).isEqualTo(postId);
    }

    @Test
    void commentEntityToResponse_shouldConvertEntityToResponse() {
        // Given
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.of(userReaction));
        when(reactionQueryPort.countReactions(commentId, ReactionType.LIKE))
                .thenReturn(5L);
        when(commentRepository.countByParentIdAndPostId(commentId, postId)).thenReturn(3);

        // When
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(comment.getId());
        assertThat(result.getParentId()).isEqualTo(comment.getParentId());
        assertThat(result.getPostId()).isEqualTo(postId);
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getText()).isEqualTo(comment.getText());
        assertThat(result.getUserReaction()).isEqualTo(userReaction);
        assertThat(result.getLikeCount()).isEqualTo(5);
        assertThat(result.getReplyCount()).isEqualTo(3);
        assertThat(result.getCreateDate()).isEqualTo(comment.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(comment.getUpdateDate());

        verify(reactionService).getUserReactionByContentId(commentId);
        verify(reactionQueryPort).countReactions(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPostId(commentId, postId);
    }

    @Test
    void commentEntityToResponse_shouldHandleNullUserReaction() {
        // Given
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.empty());
        when(reactionQueryPort.countReactions(commentId, ReactionType.LIKE))
                .thenReturn(0L);
        when(commentRepository.countByParentIdAndPostId(commentId, postId)).thenReturn(0);

        // When
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserReaction()).isNull();

        verify(reactionService).getUserReactionByContentId(commentId);
        verify(reactionQueryPort).countReactions(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPostId(commentId, postId);
    }

    @Test
    void commentEntityToResponse_shouldHandleZeroCounts() {
        // Given
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.empty());
        when(reactionQueryPort.countReactions(commentId, ReactionType.LIKE))
                .thenReturn(0L);
        when(commentRepository.countByParentIdAndPostId(commentId, postId)).thenReturn(0);

        // When
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLikeCount()).isEqualTo(0);
        assertThat(result.getReplyCount()).isEqualTo(0);

        verify(reactionQueryPort).countReactions(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPostId(commentId, postId);
    }

    @Test
    void commentEntityToResponse_shouldHandleNullParentId() {
        // Given
        comment.setParentId(null);
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.empty());
        when(reactionQueryPort.countReactions(commentId, ReactionType.LIKE))
                .thenReturn(0L);
        when(commentRepository.countByParentIdAndPostId(commentId, postId)).thenReturn(0);

        // When
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isNull();

        verify(commentRepository).countByParentIdAndPostId(commentId, postId);
    }

    @Test
    void commentEntityToResponse_shouldHandleDifferentReactionTypes() {
        // Given
        ReactionResponse dislikeReaction = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .reactionType(ReactionType.DISLIKE)
                .build();
        
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.of(dislikeReaction));
        when(reactionQueryPort.countReactions(commentId, ReactionType.LIKE))
                .thenReturn(2L);
        when(commentRepository.countByParentIdAndPostId(commentId, postId)).thenReturn(1);

        // When
        when(userQueryPort.getUserSummary(userId))
                .thenReturn(Optional.of(new UserSummaryDTO(userId, "testuser", "Test User", null, null)));
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserReaction()).isEqualTo(dislikeReaction);
        assertThat(result.getUserReaction().getReactionType()).isEqualTo(ReactionType.DISLIKE);

        verify(reactionService).getUserReactionByContentId(commentId);
        verify(reactionQueryPort).countReactions(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPostId(commentId, postId);
    }
}

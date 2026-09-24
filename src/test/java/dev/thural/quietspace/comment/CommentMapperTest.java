package dev.thural.quietspace.comment;

import dev.thural.quietspace.comment.Comment;
import dev.thural.quietspace.comment.CommentMapper;
import dev.thural.quietspace.comment.CommentRepository;
import dev.thural.quietspace.comment.dto.CommentRequest;
import dev.thural.quietspace.comment.dto.CommentResponse;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.post.PostRepository;
import dev.thural.quietspace.reaction.ReactionRepository;
import dev.thural.quietspace.reaction.ReactionService;
import dev.thural.quietspace.reaction.dto.ReactionResponse;
import dev.thural.quietspace.reaction.ReactionType;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentMapperTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private ReactionService reactionService;

    @InjectMocks
    private CommentMapper commentMapper;

    private CommentRequest commentRequest;
    private Comment comment;
    private User user;
    private Post post;
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

        post = Post.builder()
                .id(postId)
                .title("Test Post")
                .text("Test content")
                .user(user)
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
                .user(user)
                .post(post)
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
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        Comment result = commentMapper.commentRequestToEntity(commentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(commentRequest.getParentId());
        assertThat(result.getText()).isEqualTo(commentRequest.getText());
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getPost()).isEqualTo(post);

        verify(userRepository).findById(userId);
        verify(postRepository).findById(postId);
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
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        Comment result = commentMapper.commentRequestToEntity(requestWithoutParent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isNull();
        assertThat(result.getText()).isEqualTo(requestWithoutParent.getText());
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getPost()).isEqualTo(post);

        verify(userRepository).findById(userId);
        verify(postRepository).findById(postId);
    }

    @Test
    void commentRequestToEntity_shouldHandleNonExistentUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        Comment result = commentMapper.commentRequestToEntity(commentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNull();
        assertThat(result.getPost()).isEqualTo(post);

        verify(userRepository).findById(userId);
        verify(postRepository).findById(postId);
    }

    @Test
    void commentRequestToEntity_shouldHandleNonExistentPost() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When
        Comment result = commentMapper.commentRequestToEntity(commentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getPost()).isNull();

        verify(userRepository).findById(userId);
        verify(postRepository).findById(postId);
    }

    @Test
    void commentEntityToResponse_shouldConvertEntityToResponse() {
        // Given
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.of(userReaction));
        when(reactionRepository.countByContentIdAndReactionType(commentId, ReactionType.LIKE))
                .thenReturn(5);
        when(commentRepository.countByParentIdAndPost(commentId, post)).thenReturn(3);

        // When
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(comment.getId());
        assertThat(result.getParentId()).isEqualTo(comment.getParentId());
        assertThat(result.getPostId()).isEqualTo(post.getId());
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getText()).isEqualTo(comment.getText());
        assertThat(result.getUserReaction()).isEqualTo(userReaction);
        assertThat(result.getLikeCount()).isEqualTo(5);
        assertThat(result.getReplyCount()).isEqualTo(3);
        assertThat(result.getCreateDate()).isEqualTo(comment.getCreateDate());
        assertThat(result.getUpdateDate()).isEqualTo(comment.getUpdateDate());

        verify(reactionService).getUserReactionByContentId(commentId);
        verify(reactionRepository).countByContentIdAndReactionType(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPost(commentId, post);
    }

    @Test
    void commentEntityToResponse_shouldHandleNullUserReaction() {
        // Given
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.empty());
        when(reactionRepository.countByContentIdAndReactionType(commentId, ReactionType.LIKE))
                .thenReturn(0);
        when(commentRepository.countByParentIdAndPost(commentId, post)).thenReturn(0);

        // When
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserReaction()).isNull();

        verify(reactionService).getUserReactionByContentId(commentId);
        verify(reactionRepository).countByContentIdAndReactionType(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPost(commentId, post);
    }

    @Test
    void commentEntityToResponse_shouldHandleZeroCounts() {
        // Given
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.empty());
        when(reactionRepository.countByContentIdAndReactionType(commentId, ReactionType.LIKE))
                .thenReturn(0);
        when(commentRepository.countByParentIdAndPost(commentId, post)).thenReturn(0);

        // When
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLikeCount()).isEqualTo(0);
        assertThat(result.getReplyCount()).isEqualTo(0);

        verify(reactionRepository).countByContentIdAndReactionType(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPost(commentId, post);
    }

    @Test
    void commentEntityToResponse_shouldHandleNullParentId() {
        // Given
        comment.setParentId(null);
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.empty());
        when(reactionRepository.countByContentIdAndReactionType(commentId, ReactionType.LIKE))
                .thenReturn(0);
        when(commentRepository.countByParentIdAndPost(commentId, post)).thenReturn(0);

        // When
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isNull();

        verify(commentRepository).countByParentIdAndPost(commentId, post);
    }

    @Test
    void commentEntityToResponse_shouldHandleDifferentReactionTypes() {
        // Given
        ReactionResponse dislikeReaction = ReactionResponse.builder()
                .id(UUID.randomUUID())
                .reactionType(ReactionType.DISLIKE)
                .build();
        
        when(reactionService.getUserReactionByContentId(commentId)).thenReturn(Optional.of(dislikeReaction));
        when(reactionRepository.countByContentIdAndReactionType(commentId, ReactionType.LIKE))
                .thenReturn(2);
        when(commentRepository.countByParentIdAndPost(commentId, post)).thenReturn(1);

        // When
        CommentResponse result = commentMapper.commentEntityToResponse(comment);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserReaction()).isEqualTo(dislikeReaction);
        assertThat(result.getUserReaction().getReactionType()).isEqualTo(ReactionType.DISLIKE);

        verify(reactionService).getUserReactionByContentId(commentId);
        verify(reactionRepository).countByContentIdAndReactionType(commentId, ReactionType.LIKE);
        verify(commentRepository).countByParentIdAndPost(commentId, post);
    }
}

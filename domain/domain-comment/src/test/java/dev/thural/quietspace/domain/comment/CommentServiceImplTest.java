package dev.thural.quietspace.domain.comment;

import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.domain.comment.CommentMapper;
import dev.thural.quietspace.domain.comment.CommentRepository;
import dev.thural.quietspace.domain.comment.CommentServiceImpl;
import dev.thural.quietspace.domain.comment.dto.CommentRequest;
import dev.thural.quietspace.domain.comment.dto.CommentResponse;
import dev.thural.quietspace.domain.comment.port.CommentPostPort;
import dev.thural.quietspace.core.shared.util.PagingProvider;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static dev.thural.quietspace.core.shared.util.PagingProvider.BY_CREATED_DATE_ASC;
import static dev.thural.quietspace.core.shared.util.PagingProvider.buildPageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserService userService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentPostPort postPort;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UUID userId;
    private UUID postId;
    private User user;
    private Comment comment;
    private CommentResponse commentResponse;
    private CommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        this.userId = UUID.randomUUID();
        this.postId = UUID.randomUUID();

        this.user = User.builder()
                .id(userId)
                .username("user")
                .email("user@email.com")
                .password("pAsSword")
                .build();

        this.comment = Comment.builder()
                .id(UUID.randomUUID())
                .parentId(UUID.randomUUID())
                .user(user)
                .postId(postId)
                .text("sample text")
                .build();

        this.commentRequest = CommentRequest.builder()
                .userId(user.getId())
                .text("sample text")
                .postId(postId)
                .build();

        this.commentResponse = CommentResponse.builder()
                .id(UUID.randomUUID())
                .text("sample text")
                .postId(postId)
                .username(user.getUsername())
                .userId(user.getId())
                .build();
    }

    @Test
    void getCommentsByPost_shouldReturnComments() {
        PageRequest pageRequest = PagingProvider.buildPageRequest(1, 50, BY_CREATED_DATE_ASC);

        when(commentRepository.findAllByPostId(postId, pageRequest)).thenReturn(Page.empty());

        Page<CommentResponse> commentPage = commentService.getCommentsByPostId(postId, 1, 50);

        assertThat(commentPage).isEqualTo(Page.empty());
        verify(commentRepository, times(1)).findAllByPostId(postId, pageRequest);
    }

    @Test
    void getCommentsByUser_shouldReturnComments() {
        PageRequest pageRequest = PagingProvider.buildPageRequest(1, 50, null);
        when(commentRepository.findAllByUserId(userId, pageRequest)).thenReturn(Page.empty());
        when(userService.getSignedUser()).thenReturn(user);

        Page<CommentResponse> commentPage = commentService.getCommentsByUserId(userId, 1, 50);

        assertThat(commentPage).isEqualTo(Page.empty());
        verify(commentRepository, times(1)).findAllByUserId(user.getId(), pageRequest);
    }

    @Test
    void createComment_shouldReturnComment() {
        when(userService.getSignedUser()).thenReturn(user);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(postPort.postExists(comment.getPostId())).thenReturn(true);
        when(commentMapper.commentRequestToEntity(commentRequest)).thenReturn(comment);
        when(commentMapper.commentEntityToResponse(comment)).thenReturn(commentResponse);

        CommentResponse savedComment = commentService.createComment(commentRequest);

        assertThat(savedComment).isEqualTo(commentResponse);
        verify(commentRepository, times(1)).save(comment);
        verify(postPort, times(1)).postExists(comment.getPostId());
        verify(commentMapper, times(1)).commentRequestToEntity(commentRequest);
    }

    @Test
    void getCommentById_shouldReturnComment() {
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentMapper.commentEntityToResponse(comment)).thenReturn(commentResponse);

        Optional<CommentResponse> foundComment = commentService.getCommentById(comment.getId());

        assertThat(foundComment).isNotEmpty();
        assertThat(foundComment.get()).isEqualTo(commentResponse);
        verify(commentRepository, times(1)).findById(comment.getId());
    }

    @Test
    void updateComment_shouldReturnComment() {
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();
        comment.setUser(otherUser);
        when(userService.getSignedUser()).thenReturn(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentMapper.commentEntityToResponse(any(Comment.class))).thenReturn(commentResponse);

        CommentResponse savedComment = commentService.updateComment(comment.getId(), commentRequest);

        assertThat(savedComment).isEqualTo(commentResponse);
        verify(commentRepository, times(1)).findById(comment.getId());
    }

    @Test
    void deleteComment_shouldSucceed() {
        comment.setParentId(null);
        when(userService.getSignedUser()).thenReturn(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        commentService.deleteComment(comment.getId());

        verify(commentRepository, times(1)).findById(comment.getId());
        verify(commentRepository, times(1)).deleteById(comment.getId());
        verify(commentRepository, times(1)).deleteAllByParentId(comment.getId());
    }

    @Test
    void getRepliesByParentId() {
        PageRequest pageRequest = buildPageRequest(1, 50, null);
        when(commentRepository.findAllByParentId(comment.getId(), pageRequest)).thenReturn(Page.empty());

        Page<CommentResponse> commentPage = commentService.getRepliesByParentId(comment.getId(), 1, 50);

        assertThat(commentPage).isEqualTo(Page.empty());
        verify(commentRepository, times(1)).findAllByParentId(comment.getId(), pageRequest);
    }

    @Test
    void patchComment_shouldReturnComment() {
        when(userService.getSignedUser()).thenReturn(user);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentMapper.commentEntityToResponse(comment)).thenReturn(commentResponse);

        CommentResponse savedComment = commentService.patchComment(comment.getId(), commentRequest);

        assertThat(savedComment).isEqualTo(commentResponse);
        verify(commentRepository, times(1)).findById(comment.getId());
    }

    @Test
    void getLatestCommentByUserIdAndPostId_givenExistingComment_shouldReturnResponse() {
        when(commentRepository.findLatestCommentByPostAndUserByUpdateDate(postId, userId))
                .thenReturn(Optional.of(comment));
        when(commentMapper.commentEntityToResponse(comment)).thenReturn(commentResponse);

        Optional<CommentResponse> result = commentService.getLatestCommentByUserIdAndPostId(userId, postId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(commentResponse);
    }

    @Test
    void getLatestCommentByUserIdAndPostId_givenNoComment_shouldReturnEmpty() {
        when(commentRepository.findLatestCommentByPostAndUserByUpdateDate(postId, userId))
                .thenReturn(Optional.empty());

        Optional<CommentResponse> result = commentService.getLatestCommentByUserIdAndPostId(userId, postId);

        assertThat(result).isEmpty();
    }

}
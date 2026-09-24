package dev.thural.quietspace.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.comment.Comment;
import dev.thural.quietspace.comment.CommentService;
import dev.thural.quietspace.comment.dto.CommentRequest;
import dev.thural.quietspace.comment.dto.CommentResponse;
import dev.thural.quietspace.notification.NotificationService;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "user", roles = "USER", authorities = "USER, ADMIN")
class CommentControllerTest {

    MockMvc mockMvc;
    @Spy
    ObjectMapper objectMapper;

    @Mock
    CommentService commentService;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    CommentController commentController;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;
    @Captor
    ArgumentCaptor<CommentRequest> commentRequestCaptor;

    private Comment comment;
    private CommentResponse commentResponse;
    private CommentRequest commentRequest;
    private Post post;
    private User user;

    @BeforeEach
    public void setUp() {

        this.mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();

        this.user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email("user@email.com")
                .role(Role.ADMIN)
                .password("pAsSword")
                .build();

        this.post = Post.builder()
                .id(UUID.randomUUID())
                .user(user)
                .text("sample text")
                .build();

        this.comment = Comment.builder()
                .id(UUID.randomUUID())
                .text("sample text")
                .post(post)
                .user(user)
                .build();

        this.commentRequest = CommentRequest.builder()
                .postId(post.getId())
                .text("sample text")
                .userId(user.getId())
                .build();

        this.commentResponse = CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .username(comment.getUser().getUsername())
                .userId(user.getId())
                .postId(post.getId())
                .build();
    }

    @Test
    void getCommentsByPostId() throws Exception {

        mockMvc.perform(get(CommentController.COMMENT_PATH + "/post/" + post.getId())
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService, times(1)).getCommentsByPostId(post.getId(), 1, 10);
    }

    @Test
    void getCommentsByUserId() throws Exception {

        mockMvc.perform(get(CommentController.COMMENT_PATH + "/user/" + user.getId())
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService).getCommentsByUserId(user.getId(), 1, 10);
    }

    @Test
    void getCommentRepliesById() throws Exception {

        mockMvc.perform(get(CommentController.COMMENT_PATH + "/" + comment.getId() + "/replies")
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService).getRepliesByParentId(comment.getId(), 1, 10);
    }

    @Test
    void getCommentById() throws Exception {
        when(commentService.getCommentById(comment.getId())).thenReturn(Optional.ofNullable(commentResponse));

        mockMvc.perform(get(CommentController.COMMENT_PATH + "/" + comment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId", is(commentResponse.getPostId().toString())))
                .andExpect(jsonPath("$.userId", is(commentResponse.getUserId().toString())))
                .andExpect(jsonPath("$.text", is(commentResponse.getText())));

        verify(commentService, times(1)).getCommentById(uuidArgumentCaptor.capture());
        assertThat(commentResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void createComment() throws Exception {
        when(commentService.createComment(any(CommentRequest.class))).thenReturn(commentResponse);

        mockMvc.perform(post(CommentController.COMMENT_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId", is(commentResponse.getPostId().toString())))
                .andExpect(jsonPath("$.userId", is(commentResponse.getUserId().toString())))
                .andExpect(jsonPath("$.text", is(commentResponse.getText())));

        verify(commentService, times(1)).createComment(commentRequestCaptor.capture());
        assertThat(commentRequest.getText()).isEqualTo(commentRequestCaptor.getValue().getText());
    }

    @Test
    void putComment() throws Exception {
        when(commentService.updateComment(any(UUID.class), any(CommentRequest.class))).thenReturn(commentResponse);

        mockMvc.perform(put(CommentController.COMMENT_PATH + "/" + comment.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId", is(commentResponse.getPostId().toString())))
                .andExpect(jsonPath("$.userId", is(commentResponse.getUserId().toString())))
                .andExpect(jsonPath("$.text", is(commentResponse.getText())));

        verify(commentService).updateComment(uuidArgumentCaptor.capture(), commentRequestCaptor.capture());
        assertThat(commentRequest.getText()).isEqualTo(commentRequestCaptor.getValue().getText());
        assertThat(commentResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void deleteComment() throws Exception {
        doNothing().when(commentService).deleteComment(any());

        mockMvc.perform(delete(CommentController.COMMENT_PATH + "/" + comment.getId()))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(uuidArgumentCaptor.capture());
        assertThat(comment.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void patchComment() throws Exception {
        ArgumentCaptor<CommentRequest> commentRequestCaptor = ArgumentCaptor.forClass(CommentRequest.class);
        when(commentService.patchComment(any(UUID.class), any(CommentRequest.class))).thenReturn(commentResponse);

        mockMvc.perform(patch(CommentController.COMMENT_PATH + "/" + comment.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId", is(commentResponse.getPostId().toString())))
                .andExpect(jsonPath("$.userId", is(commentResponse.getUserId().toString())))
                .andExpect(jsonPath("$.text", is(commentResponse.getText())));

        verify(commentService).patchComment(uuidArgumentCaptor.capture(), commentRequestCaptor.capture());
        assertThat(commentRequest.getText()).isEqualTo(commentRequestCaptor.getValue().getText());
        assertThat(commentResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }
}

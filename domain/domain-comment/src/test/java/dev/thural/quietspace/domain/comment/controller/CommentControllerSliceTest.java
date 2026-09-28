package dev.thural.quietspace.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.core.shared.enums.Role;
import dev.thural.quietspace.core.shared.security.JwtTokenService;
import dev.thural.quietspace.core.shared.security.TokenRepository;
import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.domain.comment.CommentService;
import dev.thural.quietspace.domain.comment.dto.CommentRequest;
import dev.thural.quietspace.domain.comment.dto.CommentResponse;
import dev.thural.quietspace.domain.notification.NotificationService;
import dev.thural.quietspace.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = CommentController.class)
class CommentControllerSliceTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CommentService commentService;
    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    TokenRepository tokenRepository;
    @MockitoBean
    JwtTokenService jwtTokenService;
    @MockitoBean
    UserDetailsService userDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }

    ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<CommentRequest> commentRequestCaptor = ArgumentCaptor.forClass(CommentRequest.class);

    private Comment comment;
    private CommentResponse commentResponse;
    private CommentRequest commentRequest;
    private UUID postId;
    private User user;

    @BeforeEach
    public void setUp() {

        this.user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email("user@email.com")
                .role(Role.ADMIN)
                .password("pAsSword")
                .build();

        this.postId = UUID.randomUUID();

        this.comment = Comment.builder()
                .id(UUID.randomUUID())
                .text("sample text")
                .postId(postId)
                .userId(user.getId())
                .build();

        this.commentRequest = CommentRequest.builder()
                .postId(postId)
                .text("sample text")
                .userId(user.getId())
                .build();

        this.commentResponse = CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getText())
                .username(user.getUsername())
                .userId(user.getId())
                .postId(postId)
                .build();
    }

    @Test
    void getCommentsByPostId() throws Exception {

        mockMvc.perform(get(CommentController.COMMENT_PATH + "/post/" + postId)
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentService, times(1)).getCommentsByPostId(postId, 1, 10);
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
    @WithMockUser(username = "user", roles = "USER")
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
    void deleteComment() {
    }

    @Test
    void patchComment() throws Exception {
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

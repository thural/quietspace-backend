package dev.thural.quietspace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.comment.CommentService;
import dev.thural.quietspace.notification.NotificationService;
import dev.thural.quietspace.post.PostService;
import dev.thural.quietspace.reaction.ReactionService;
import dev.thural.quietspace.security.JwtService;
import dev.thural.quietspace.security.TokenRepository;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserRequest;
import dev.thural.quietspace.user.dto.UserResponse;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = UserController.class)
public class UserControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<UserRequest> userArgumentCaptor = ArgumentCaptor.forClass(UserRequest.class);

    @MockitoBean
    UserService userService;
    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    TokenRepository tokenRepository;
    @MockitoBean
    PostService postService;
    @MockitoBean
    CommentService commentService;
    @MockitoBean
    ReactionService reactionService;
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    UserDetailsService userDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }

    private UUID userId;
    private UserRequest registerRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {

        this.userId = UUID.randomUUID();

        this.registerRequest = UserRequest.builder()
                .username("user")
                .firstname("Test")
                .lastname("User")
                .role("user")
                .email("user@email.com")
                .build();

        this.userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("user")
                .email("user@email.com")
                .role("USER")
                .build();
    }

    @Test
    @WithAnonymousUser
    void searchUsersPaginated() throws Exception {

        mockMvc.perform(get(UserController.USER_PATH + "/search")
                        .param("page-number", "0")
                        .param("page-size", "10")
                        .param("username", "user"))
                .andExpect(status().isOk());

        verify(userService, times(1)).listUsersByUsername("user", 0, 10);
    }

    @Test
    void queryUsersPaginated() throws Exception {

        mockMvc.perform(get(UserController.USER_PATH + "/query")
                        .param("username", "user")
                        .param("page-number", "0")
                        .param("page-size", "10"))
                .andExpect(status().isOk());

        verify(userService).queryUsers("user", null, null, 0, 10);
    }

    @Test
    void getUserById() throws Exception {
        when(userService.getUserResponseById(userId)).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get(UserController.USER_PATH + "/" + userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(userResponse.getId().toString())))
                .andExpect(jsonPath("$.username", is(userResponse.getUsername())));

        verify(userService, times(1)).getUserResponseById(uuidArgumentCaptor.capture());
        assertThat(userId).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void deleteUserById() throws Exception {

        mockMvc.perform(delete(UserController.USER_PATH + "/" + userId))
                .andExpect(status().isNoContent());
        verify(userService, times(1)).deleteUserById(uuidArgumentCaptor.capture());
        assertThat(userId).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void patchUser() throws Exception {
        registerRequest.setUsername("testUserUpdated");
        registerRequest.setPassword("testPasswordUpdated");

        String userBodyJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(patch(UserController.USER_PATH)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBodyJson))
                .andExpect(status().isOk());

        verify(userService).patchUser(userArgumentCaptor.capture());

        assertThat(registerRequest.getUsername()).isEqualTo(userArgumentCaptor.getValue().getUsername());
        assertThat(registerRequest.getPassword()).isEqualTo(userArgumentCaptor.getValue().getPassword());
    }

    @Test
    void userByIdNotFound() throws Exception {

        mockMvc.perform(get(UserController.USER_PATH + "/" + userId))
                .andExpect(status().isNotFound());

        verify(userService).getUserResponseById(uuidArgumentCaptor.capture());
        assertThat(userId).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void getAuthenticatedUser() throws Exception {
        when(userService.getLoggedUserResponse()).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get(UserController.USER_PATH + "/" + "profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is(userResponse.getUsername())))
                .andExpect(status().isOk());

        verify(userService, times(1)).getLoggedUserResponse();
    }

    @Test
    void updateUser_givenTooLongUsername_shouldReturn400() throws Exception {
        registerRequest.setUsername("a".repeat(33));

        mockMvc.perform(patch(UserController.USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_givenTooLongPassword_shouldReturn400() throws Exception {
        registerRequest.setPassword("a".repeat(33));

        mockMvc.perform(patch(UserController.USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMe() throws Exception {
        when(userService.getLoggedUserResponse()).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get(UserController.USER_PATH + "/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is(userResponse.getUsername())))
                .andExpect(status().isOk());

        verify(userService).getLoggedUserResponse();
    }

    @Test
    void getMe_whenNotFound_shouldReturn404() throws Exception {
        when(userService.getLoggedUserResponse()).thenReturn(Optional.empty());

        mockMvc.perform(get(UserController.USER_PATH + "/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchMe() throws Exception {
        registerRequest.setPassword("validPassword123");
        when(userService.patchUser(any())).thenReturn(userResponse);

        mockMvc.perform(patch(UserController.USER_PATH + "/me")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void followUser() throws Exception {
        doNothing().when(userService).followUser(any());
        doNothing().when(notificationService).processNotification(any(), any());

        mockMvc.perform(post(UserController.USER_PATH + "/" + userId + "/follow"))
                .andExpect(status().isOk());

        verify(userService).followUser(uuidArgumentCaptor.capture());
        assertThat(userId).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void unfollowUser() throws Exception {
        doNothing().when(userService).unfollowUser(any());

        mockMvc.perform(delete(UserController.USER_PATH + "/" + userId + "/follow"))
                .andExpect(status().isNoContent());

        verify(userService).unfollowUser(uuidArgumentCaptor.capture());
        assertThat(userId).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void unblockUserProfile() throws Exception {
        doNothing().when(userService).removeUserFromBlockList(any());

        mockMvc.perform(delete(UserController.USER_PATH + "/profile/block/" + userId))
                .andExpect(status().isNoContent());

        verify(userService).removeUserFromBlockList(uuidArgumentCaptor.capture());
        assertThat(userId).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void getBlockedUsers() throws Exception {
        when(userService.getBlockedUsers()).thenReturn(List.of(userResponse));

        mockMvc.perform(get(UserController.USER_PATH + "/profile/blocked")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].username", is(userResponse.getUsername())))
                .andExpect(status().isOk());

        verify(userService).getBlockedUsers();
    }

    @Test
    void getOnlineUsers() throws Exception {
        when(userService.findConnectedFollowings()).thenReturn(List.of(userResponse));

        mockMvc.perform(get(UserController.USER_PATH + "/online")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].username", is(userResponse.getUsername())))
                .andExpect(status().isOk());

        verify(userService).findConnectedFollowings();
    }

}

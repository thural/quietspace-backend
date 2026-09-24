package dev.thural.quietspace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.user.UserService;
import dev.thural.quietspace.user.dto.UserRequest;
import dev.thural.quietspace.user.dto.UserResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "user", roles = "USER", authorities = "USER, ADMIN")
public class UserControllerTest {

    private MockMvc mockMvc;

    @Spy
    ObjectMapper objectMapper;

    @Mock
    UserService userService;

    @InjectMocks
    UserController userController;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;
    @Captor
    ArgumentCaptor<UserRequest> userArgumentCaptor;

    UUID userId;
    UserRequest registerRequest;
    UserResponse userResponse;

    @BeforeEach
    void setUp() {

        this.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

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
    void queryUsersPaginatedQuery() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH + "/query")
                        .param("username", "admin")
                        .param("firstname", "admin")
                        .param("lastname", "admin")
                        .param("page-number", "0")
                        .param("page-size", "10"))
                .andExpect(status().isOk());

        verify(userService).queryUsers("admin", "admin", "admin", 0, 10);
    }

    @Test
    void getUserById() throws Exception {
        when(userService.getUserResponseById(uuidArgumentCaptor.capture())).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get(UserController.USER_PATH + "/" + userResponse.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(userResponse.getId().toString())))
                .andExpect(jsonPath("$.username", is(userResponse.getUsername())));

        verify(userService, times(1)).getUserResponseById(uuidArgumentCaptor.capture());
    }

    @Test
    void deleteUserById() throws Exception {
        doNothing().when(userService).deleteUserById(any());

        mockMvc.perform(delete(UserController.USER_PATH + "/" + userId)
                        .requestAttr("Authorization", "Bearer dhj3h32hd2k"))
                .andExpect(status().isNoContent());
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

        mockMvc.perform(get(UserController.USER_PATH + "/" + userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService).getUserResponseById(uuidArgumentCaptor.capture());
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

}

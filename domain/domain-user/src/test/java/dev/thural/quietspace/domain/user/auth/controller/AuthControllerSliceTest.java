package dev.thural.quietspace.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.auth.AuthService;
import dev.thural.quietspace.auth.dto.AuthRequest;
import dev.thural.quietspace.auth.dto.AuthResponse;
import dev.thural.quietspace.auth.dto.RegistrationRequest;
import dev.thural.quietspace.security.JwtService;
import dev.thural.quietspace.security.TokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = AuthController.class)
class AuthControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private TokenRepository tokenRepository;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void register_givenValidRequest_shouldReturn200() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("testuser")
                .firstname("Test")
                .lastname("User")
                .email("test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void register_givenInvalidEmail_shouldReturn400() throws Exception {
        RegistrationRequest request = RegistrationRequest.builder()
                .username("testuser")
                .firstname("Test")
                .lastname("User")
                .email("invalid")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_givenMissingBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticate_givenValidCredentials_shouldReturn200WithBody() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId("user-id")
                .message("authentication was successful")
                .build();
        when(authService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);

        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.userId").value("user-id"))
                .andExpect(jsonPath("$.message").value("authentication was successful"));
    }

    @Test
    void activateAccount_givenValidToken_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", "valid-code"))
                .andExpect(status().isOk());
    }

    @Test
    void activateAccount_givenMissingToken_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/activate-account"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_givenAuthHeader_shouldReturn200WithBody() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .message("token was refreshed")
                .build();
        when(authService.refreshToken(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("Authorization", "Bearer some.refresh.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.message").value("token was refreshed"));
    }

    @Test
    void signout_givenAuthHeader_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signout")
                        .header("Authorization", "Bearer some.jwt.token"))
                .andExpect(status().isOk());
    }

    @Test
    void loginAlias() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId("user-id")
                .message("authentication was successful")
                .build();
        when(authService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);

        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void logoutAlias() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer some.jwt.token"))
                .andExpect(status().isOk());
    }

    @Test
    void refreshAlias() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .message("token was refreshed")
                .build();
        when(authService.refreshToken(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer some.refresh.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.message").value("token was refreshed"));
    }

}

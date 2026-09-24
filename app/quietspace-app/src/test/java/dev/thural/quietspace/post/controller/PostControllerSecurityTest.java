package dev.thural.quietspace.post.controller;

import dev.thural.quietspace.notification.NotificationService;
import dev.thural.quietspace.post.PostService;
import dev.thural.quietspace.security.JwtService;
import dev.thural.quietspace.security.TokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
class PostControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;
    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private TokenRepository tokenRepository;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getPostById_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllPosts_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletePost_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}

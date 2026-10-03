package dev.thural.quietspace.domain.reaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.core.security.port.JwtTokenService;
import dev.thural.quietspace.domain.notification.NotificationService;
import dev.thural.quietspace.domain.reaction.ReactionService;
import dev.thural.quietspace.domain.reaction.dto.ReactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = ReactionController.class)
class ReactionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ReactionService reactionService;

    @MockitoBean
    NotificationService notificationService;
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

    @Test
    void getReactionsByUser_shouldReturnPage() throws Exception {
        when(reactionService.getReactionsByUserIdAndContentType(any(), any(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/reactions/user")
                        .param("userId", UUID.randomUUID().toString())
                        .param("contentType", "POST")
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getReactionsByContent_shouldReturnPage() throws Exception {
        when(reactionService.getReactionsByContentIdAndContentType(any(), any(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/reactions/content")
                        .param("contentId", UUID.randomUUID().toString())
                        .param("contentType", "POST")
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleReaction_shouldReturn200() throws Exception {
        ReactionRequest request = ReactionRequest.builder()
                .userId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions/toggle-reaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void addReaction() throws Exception {
        doNothing().when(reactionService).addReaction(any());
        doNothing().when(notificationService).processNotificationByReaction(any(), any());

        ReactionRequest request = ReactionRequest.builder()
                .userId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void removeReaction() throws Exception {
        doNothing().when(reactionService).removeReaction(any());

        mockMvc.perform(delete("/api/v1/reactions/{reactionId}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getReactionsByPostId() throws Exception {
        when(reactionService.getReactionsByContentIdAndContentType(any(), any(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/reactions/post/{postId}", UUID.randomUUID())
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void countByContentIdAndReactionType_shouldReturnCount() throws Exception {
        when(reactionService.countByContentIdAndReactionType(any(), any())).thenReturn(5);

        mockMvc.perform(get("/api/v1/reactions/count")
                        .param("contentId", UUID.randomUUID().toString())
                        .param("type", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }
}

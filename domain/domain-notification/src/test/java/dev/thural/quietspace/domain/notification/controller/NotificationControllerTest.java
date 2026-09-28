package dev.thural.quietspace.domain.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.core.shared.security.JwtTokenService;
import dev.thural.quietspace.core.shared.security.TokenRepository;
import dev.thural.quietspace.domain.notification.NotificationService;
import dev.thural.quietspace.domain.notification.NotificationType;
import dev.thural.quietspace.domain.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = NotificationController.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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

    @Test
    void getAllNotifications_shouldReturnPage() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .type(NotificationType.FOLLOW_REQUEST)
                .build();
        when(notificationService.getAllNotifications(any(), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get(NotificationController.NOTIFICATION_PATH)
                        .param("pageNumber", "1")
                        .param("pageSize", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("FOLLOW_REQUEST"));
    }

    @Test
    void getNotificationsByType_shouldReturnPage() throws Exception {
        when(notificationService.getNotificationsByType(any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get(NotificationController.NOTIFICATION_PATH + "/type/{type}", "POST_REACTION")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getCountOfPendingNotifications_shouldReturnCount() throws Exception {
        when(notificationService.getCountOfPendingNotifications()).thenReturn(5);

        mockMvc.perform(get(NotificationController.NOTIFICATION_PATH + "/count-pending"))
                .andExpect(status().isOk());
    }

    @Test
    void handleSeen_shouldReturn202() throws Exception {
        mockMvc.perform(post(NotificationController.NOTIFICATION_PATH + "/seen/{contentId}", UUID.randomUUID()))
                .andExpect(status().isAccepted());
    }

    @Test
    void processNotificationByReaction_shouldReturn200() throws Exception {
        mockMvc.perform(post(NotificationController.NOTIFICATION_PATH + "/process-reaction")
                        .param("type", "POST")
                        .param("contentId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void processNotification_shouldReturn200() throws Exception {
        mockMvc.perform(post(NotificationController.NOTIFICATION_PATH + "/process")
                        .param("type", "FOLLOW_REQUEST")
                        .param("contentId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        verify(notificationService).processNotification(
                eq(NotificationType.FOLLOW_REQUEST), any(UUID.class));
    }
}

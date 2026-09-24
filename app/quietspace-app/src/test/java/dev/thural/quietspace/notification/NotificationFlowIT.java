package dev.thural.quietspace.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.domain.notification.Notification;
import dev.thural.quietspace.domain.notification.NotificationRepository;
import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.core.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class NotificationFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PhotoService photoService;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    private IntegrationTestHelper helper;
    private String user1Jwt;
    private String user2Jwt;
    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        userRepository.deleteAll();
        entityManager.flush();
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        user1Jwt = helper.registerAndLogin("notifuser1@test.com", "password123");
        user1Id = userRepository.findUserEntityByEmail("notifuser1@test.com").orElseThrow().getId();
        user2Jwt = helper.registerAndLogin("notifuser2@test.com", "password456");
        user2Id = userRepository.findUserEntityByEmail("notifuser2@test.com").orElseThrow().getId();
    }

    @Test
    void getAllNotifications_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/users/follow/{userId}/toggle-follow", user1Id)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getNotificationsByType_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/users/follow/{userId}/toggle-follow", user1Id)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/type/{type}", "FOLLOW_REQUEST")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void countPendingNotifications_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/users/follow/{userId}/toggle-follow", user1Id)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/count-pending")
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void markNotificationAsSeen_shouldReturn202() throws Exception {
        Notification notification = Notification.builder()
                .userId(user1Id)
                .actorId(user2Id)
                .contentId(user2Id)
                .isSeen(false)
                .notificationType(dev.thural.quietspace.domain.notification.NotificationType.FOLLOW_REQUEST)
                .build();
        notificationRepository.save(notification);

        mockMvc.perform(post("/api/v1/notifications/seen/{contentId}", notification.getId())
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isAccepted());
    }

    @Test
    void processNotification_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/process")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("type", "FOLLOW_REQUEST")
                        .param("contentId", user2Id.toString()))
                .andExpect(status().isOk());
    }
}

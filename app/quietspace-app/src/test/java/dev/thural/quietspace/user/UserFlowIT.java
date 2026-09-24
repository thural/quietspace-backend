package dev.thural.quietspace.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.user.UserRepository;
import dev.thural.quietspace.user.controller.UserController;
import dev.thural.quietspace.user.dto.ProfileSettingsRequest;
import dev.thural.quietspace.user.dto.UserRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class UserFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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
        user1Jwt = helper.registerAndLogin("user1@test.com", "password123");
        user1Id = userRepository.findUserEntityByEmail("user1@test.com").orElseThrow().getId();
        user2Jwt = helper.registerAndLogin("user2@test.com", "password456");
        user2Id = userRepository.findUserEntityByEmail("user2@test.com").orElseThrow().getId();
    }

    @Test
    void searchUser_byUsername_shouldReturn200() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH + "/search")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("username", "user1")
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getUserById_givenExistingId_shouldReturn200() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH + "/{userId}", user1Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1Id.toString()))
                .andExpect(jsonPath("$.email").value("user1@test.com"));
    }

    @Test
    void getUserById_givenNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH + "/{userId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_givenValidRequest_shouldReturn200() throws Exception {
        UserRequest update = UserRequest.builder()
                .username("updatedUser1")
                .firstname("Updated")
                .lastname("User")
                .email("user1@test.com")
                .password("password123")
                .role("USER")
                .build();

        mockMvc.perform(patch(UserController.USER_PATH)
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedUser1"));
    }

    @Test
    void getProfile_shouldReturn200() throws Exception {
        mockMvc.perform(get(UserController.USER_PATH + "/profile")
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@test.com"));
    }

    @Test
    void toggleFollow_shouldReturn200() throws Exception {
        mockMvc.perform(post(UserController.USER_PATH + "/follow/{userId}/toggle-follow", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());
    }

    @Test
    void getFollowers_shouldReturn200() throws Exception {
        mockMvc.perform(post(UserController.USER_PATH + "/follow/{userId}/toggle-follow", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get(UserController.USER_PATH + "/{userId}/followers", user2Id)
                        .header("Authorization", "Bearer " + user2Jwt)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getFollowings_shouldReturn200() throws Exception {
        mockMvc.perform(post(UserController.USER_PATH + "/follow/{userId}/toggle-follow", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(get(UserController.USER_PATH + "/{userId}/followings", user1Id)
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void blockUser_shouldReturn200() throws Exception {
        mockMvc.perform(post(UserController.USER_PATH + "/profile/block/{userId}", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());
    }

    @Test
    void unblockUser_shouldReturn204() throws Exception {
        mockMvc.perform(post(UserController.USER_PATH + "/profile/block/{userId}", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(delete(UserController.USER_PATH + "/profile/block/{userId}", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFollower_shouldReturn200() throws Exception {
        mockMvc.perform(post(UserController.USER_PATH + "/follow/{userId}/toggle-follow", user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());

        mockMvc.perform(post(UserController.USER_PATH + "/followers/remove/{userId}", user1Id)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfileSettings_shouldReturn200() throws Exception {
        ProfileSettingsRequest settings = ProfileSettingsRequest.builder()
                .bio("Hello, this is my bio")
                .isPrivateAccount(false)
                .isNotificationsMuted(false)
                .build();

        mockMvc.perform(patch(UserController.USER_PATH + "/profile/settings")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settings)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Hello, this is my bio"));
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        mockMvc.perform(delete(UserController.USER_PATH + "/{userId}", user1Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNoContent());
    }
}

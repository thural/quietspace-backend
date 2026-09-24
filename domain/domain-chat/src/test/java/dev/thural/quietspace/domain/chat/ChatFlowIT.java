package dev.thural.quietspace.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.chat.ChatRepository;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.chat.dto.UpdateChatRequest;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.message.MessageRepository;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class ChatFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PhotoService photoService;

    private IntegrationTestHelper helper;
    private String user1Jwt;
    private String user2Jwt;
    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        user1Jwt = helper.registerAndLogin("chatuser1@test.com", "password123");
        user1Id = userRepository.findUserEntityByEmail("chatuser1@test.com").orElseThrow().getId();
        user2Jwt = helper.registerAndLogin("chatuser2@test.com", "password456");
        user2Id = userRepository.findUserEntityByEmail("chatuser2@test.com").orElseThrow().getId();
    }

    @Test
    void createChat_givenValidRequest_shouldReturn200() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Hello!")
                .userIds(List.of(user1Id, user2Id))
                .build();

        mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void getChatById_givenExistingId_shouldReturn200() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Hi!")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String chatId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/chats/{chatId}", chatId)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chatId));
    }

    @Test
    void getChatById_givenNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/chats/{chatId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChatsByMemberId_shouldReturnList() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Hello!")
                .userIds(List.of(user1Id, user2Id))
                .build();

        mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/chats/members/{userId}", user1Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteChat_givenExistingId_shouldReturn204() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Bye!")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String chatId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/chats/{chatId}", chatId)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void addMember_toChat_shouldReturn200() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Group chat")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String chatId = objectMapper.readTree(responseBody).get("id").asText();

        var user3Jwt = helper.registerAndLogin("chatuser3@test.com", "password789");
        var user3Id = userRepository.findUserEntityByEmail("chatuser3@test.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/chats/{chatId}/members/add/{userId}", chatId, user3Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk());
    }

    @Test
    void updateChat_givenValidRequest_shouldReturn200() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Hello!")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String chatId = objectMapper.readTree(responseBody).get("id").asText();

        UpdateChatRequest updateRequest = UpdateChatRequest.builder().name("Updated Chat Name").build();

        mockMvc.perform(patch("/api/v1/chats/{chatId}", chatId)
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void removeMember_fromChat_shouldReturn200() throws Exception {
        CreateChatRequest request = CreateChatRequest.builder()
                .isGroupChat(true)
                .recipientId(user2Id)
                .text("Group chat")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String chatId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(patch("/api/v1/chats/{chatId}/members/remove/{userId}", chatId, user2Id)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNoContent());
    }
}

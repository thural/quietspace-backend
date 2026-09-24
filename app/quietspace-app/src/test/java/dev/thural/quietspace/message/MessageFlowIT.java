package dev.thural.quietspace.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.chat.ChatRepository;
import dev.thural.quietspace.chat.dto.CreateChatRequest;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.message.MessageRepository;
import dev.thural.quietspace.message.dto.MessageRequest;
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

import java.nio.charset.StandardCharsets;
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
class MessageFlowIT {

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
    private String chatId;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        messageRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);

        user1Jwt = helper.registerAndLogin("msguser1@test.com", "password123");
        user1Id = userRepository.findUserEntityByEmail("msguser1@test.com").orElseThrow().getId();

        user2Jwt = helper.registerAndLogin("msguser2@test.com", "password456");
        user2Id = userRepository.findUserEntityByEmail("msguser2@test.com").orElseThrow().getId();

        CreateChatRequest chatRequest = CreateChatRequest.builder()
                .isGroupChat(false)
                .recipientId(user2Id)
                .text("Hello!")
                .userIds(List.of(user1Id, user2Id))
                .build();

        String chatResponse = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        chatId = objectMapper.readTree(chatResponse).get("id").asText();
    }

    @Test
    void createMessage_givenValidRequest_shouldReturn200() throws Exception {
        MessageRequest request = MessageRequest.builder()
                .chatId(UUID.fromString(chatId))
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Test message content")
                .build();

        mockMvc.perform(multipart("/api/v1/messages")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "messageRequest", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Test message content"));
    }

    @Test
    void getMessagesByChatId_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/messages/chat/{chatId}", chatId)
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getMessageById_givenExistingIds_shouldReturn200() throws Exception {
        MessageRequest request = MessageRequest.builder()
                .chatId(UUID.fromString(chatId))
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Message to find")
                .build();

        String responseBody = mockMvc.perform(multipart("/api/v1/messages")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "messageRequest", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String messageId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/messages/chat/{chatId}/message/{messageId}", chatId, messageId)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId));
    }

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        MessageRequest request = MessageRequest.builder()
                .chatId(UUID.fromString(chatId))
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Unread message")
                .build();

        mockMvc.perform(multipart("/api/v1/messages")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "messageRequest", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/messages/unread")
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_givenExistingMessage_shouldReturn200() throws Exception {
        MessageRequest request = MessageRequest.builder()
                .chatId(UUID.fromString(chatId))
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Message to mark as read")
                .build();

        String responseBody = mockMvc.perform(multipart("/api/v1/messages")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "messageRequest", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String messageId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(put("/api/v1/messages/{messageId}/read", messageId)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId));
    }

    @Test
    void deleteMessage_givenExistingId_shouldReturn204() throws Exception {
        MessageRequest request = MessageRequest.builder()
                .chatId(UUID.fromString(chatId))
                .senderId(user1Id)
                .recipientId(user2Id)
                .text("Message to delete")
                .build();

        String responseBody = mockMvc.perform(multipart("/api/v1/messages")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "messageRequest", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String messageId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/messages/{messageId}", messageId)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isNoContent());
    }
}

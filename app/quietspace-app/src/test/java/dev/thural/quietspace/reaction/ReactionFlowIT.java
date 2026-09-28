package dev.thural.quietspace.reaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.core.shared.enums.ReactionType;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.domain.photo.PhotoService;
import dev.thural.quietspace.domain.reaction.ReactionRepository;
import dev.thural.quietspace.domain.reaction.dto.ReactionRequest;
import dev.thural.quietspace.domain.user.UserRepository;
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
class ReactionFlowIT {

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

    @Autowired
    private ReactionRepository reactionRepository;

    private IntegrationTestHelper helper;
    private String jwtToken;
    private UUID userId;
    private UUID postId;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        userRepository.deleteAll();
        entityManager.flush();
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        jwtToken = helper.registerAndLogin("reactuser@test.com", "password123");
        userId = userRepository.findUserEntityByEmail("reactuser@test.com").orElseThrow().getId();

        var request = dev.thural.quietspace.domain.post.dto.PostRequest.builder()
                .userId(userId)
                .title("Test Post for Reaction")
                .text("This post will be reacted to")
                .build();

        var responseBody = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/v1/posts")
                                .file(new org.springframework.mock.web.MockMultipartFile(
                                        "post", "", "application/json",
                                        objectMapper.writeValueAsString(request).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                                .header("Authorization", "Bearer " + jwtToken)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        postId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());
    }

    @Test
    void addReaction_shouldReturn200() throws Exception {
        ReactionRequest reaction = ReactionRequest.builder()
                .userId(userId)
                .contentId(postId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reaction)))
                .andExpect(status().isOk());
    }

    @Test
    void removeReaction_shouldReturn204() throws Exception {
        ReactionRequest reaction = ReactionRequest.builder()
                .userId(userId)
                .contentId(postId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions/toggle-reaction")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reaction)))
                .andExpect(status().isOk());

        entityManager.flush();

        UUID reactionId = reactionRepository.findAllByContentIdAndContentType(
                postId, EntityType.POST,
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent().get(0).getId();

        mockMvc.perform(delete("/api/v1/reactions/{reactionId}", reactionId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void toggleReaction_shouldReturn200() throws Exception {
        ReactionRequest reaction = ReactionRequest.builder()
                .userId(userId)
                .contentId(postId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions/toggle-reaction")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reaction)))
                .andExpect(status().isOk());
    }

    @Test
    void getReactionsByUser_shouldReturn200() throws Exception {
        ReactionRequest reaction = ReactionRequest.builder()
                .userId(userId)
                .contentId(postId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions/toggle-reaction")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reaction)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reactions/user")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("userId", userId.toString())
                        .param("contentType", "POST")
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getReactionsByContent_shouldReturn200() throws Exception {
        ReactionRequest reaction = ReactionRequest.builder()
                .userId(userId)
                .contentId(postId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions/toggle-reaction")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reaction)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reactions/content")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("contentId", postId.toString())
                        .param("contentType", "POST")
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void countReactions_shouldReturn200() throws Exception {
        ReactionRequest reaction = ReactionRequest.builder()
                .userId(userId)
                .contentId(postId)
                .contentType(EntityType.POST)
                .reactionType(ReactionType.LIKE)
                .build();

        mockMvc.perform(post("/api/v1/reactions/toggle-reaction")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reaction)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reactions/count")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("contentId", postId.toString())
                        .param("type", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }
}

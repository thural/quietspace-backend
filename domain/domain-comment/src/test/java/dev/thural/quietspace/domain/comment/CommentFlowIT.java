package dev.thural.quietspace.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.comment.CommentRepository;
import dev.thural.quietspace.comment.dto.CommentRequest;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.post.PostRepository;
import dev.thural.quietspace.post.dto.PostRequest;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class CommentFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

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
    private String postId;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();

        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);

        user1Jwt = helper.registerAndLogin("commentuser1@test.com", "password123");
        user1Id = userRepository.findUserEntityByEmail("commentuser1@test.com").orElseThrow().getId();

        PostRequest postRequest = PostRequest.builder()
                .userId(user1Id)
                .title("Test Post for Comments")
                .text("This post will get comments")
                .build();

        String postResponse = mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(postRequest).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        postId = objectMapper.readTree(postResponse).get("id").asText();

        user2Jwt = helper.registerAndLogin("commentuser2@test.com", "password456");
        user2Id = userRepository.findUserEntityByEmail("commentuser2@test.com").orElseThrow().getId();
    }

    @Test
    void createComment_givenValidRequest_shouldReturn200() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .userId(user2Id)
                .postId(UUID.fromString(postId))
                .text("This is a test comment")
                .build();

        mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("This is a test comment"));
    }

    @Test
    void getCommentsByPostId_shouldReturnPagedResults() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .userId(user2Id)
                .postId(UUID.fromString(postId))
                .text("A test comment")
                .build();

        mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/comments/post/{postId}", postId)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deleteComment_givenOwnComment_shouldReturn204() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .userId(user2Id)
                .postId(UUID.fromString(postId))
                .text("Comment to delete")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String commentId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_givenOtherUsersComment_shouldReturn403() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .userId(user2Id)
                .postId(UUID.fromString(postId))
                .text("Comment by user2")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String commentId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                        .header("Authorization", "Bearer " + user1Jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCommentById_givenNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/comments/{commentId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReplies_toComment_shouldReturn200() throws Exception {
        CommentRequest parentReq = CommentRequest.builder()
                .userId(user2Id)
                .postId(UUID.fromString(postId))
                .text("Parent comment")
                .build();

        String parentBody = mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String parentCommentId = objectMapper.readTree(parentBody).get("id").asText();

        CommentRequest replyReq = CommentRequest.builder()
                .userId(user1Id)
                .postId(UUID.fromString(postId))
                .parentId(UUID.fromString(parentCommentId))
                .text("Reply to parent")
                .build();

        mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/comments/{commentId}/replies", parentCommentId)
                        .header("Authorization", "Bearer " + user1Jwt)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getLatestComment_byUserOnPost_shouldReturn200() throws Exception {
        CommentRequest request = CommentRequest.builder()
                .userId(user2Id)
                .postId(UUID.fromString(postId))
                .text("Latest comment by this user")
                .build();

        mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + user2Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/comments/user/{userId}/post/{postId}/latest", user2Id, postId)
                        .header("Authorization", "Bearer " + user2Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Latest comment by this user"));
    }
}

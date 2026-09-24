package dev.thural.quietspace.post;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
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
class PostFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PhotoService photoService;

    private IntegrationTestHelper helper;
    private String jwtToken;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        postRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        jwtToken = helper.registerAndLogin("postuser@test.com", "password123");
        userId = userRepository.findUserEntityByEmail("postuser@test.com").orElseThrow().getId();
    }

    @Test
    void createPost_givenValidRequest_shouldReturn200() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("Test Post")
                .text("This is a test post content")
                .build();

        mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("This is a test post content"))
                .andExpect(jsonPath("$.title").value("Test Post"));
    }

    @Test
    void createPost_givenEmptyText_shouldReturn400() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .text("")
                .build();

        mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPostById_givenExistingId_shouldReturn200() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("Test Post")
                .text("This is a test post content")
                .build();

        String responseBody = mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String postId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId));
    }

    @Test
    void getPostById_givenNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{postId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePost_givenOwnPost_shouldReturn200() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("Original Title")
                .text("Original text")
                .build();

        String responseBody = mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String postId = objectMapper.readTree(responseBody).get("id").asText();

        PostRequest update = PostRequest.builder()
                .userId(userId)
                .title("Updated Title")
                .text("Updated text")
                .build();

        mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    void deletePost_givenOwnPost_shouldReturn204() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("Test Post")
                .text("This is a test post content")
                .build();

        String responseBody = mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String postId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllPosts_shouldReturnPagedResults() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("Test Post")
                .text("This is a test post content")
                .build();

        mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchPosts_byQuery_shouldReturn200() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("Unique Search Title")
                .text("This is a unique searchable text")
                .build();

        mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("query", "Unique Search")
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getPosts_byUser_shouldReturn200() throws Exception {
        PostRequest request = PostRequest.builder()
                .userId(userId)
                .title("User Post")
                .text("Post by this user")
                .build();

        mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/user/{userId}", userId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getCommentedPosts_shouldReturn200() throws Exception {
        String postId = objectMapper.readTree(mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(
                                        PostRequest.builder().userId(userId).title("C Post").text("Comment target").build()
                                ).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        var commentReq = dev.thural.quietspace.comment.dto.CommentRequest.builder()
                .userId(userId)
                .postId(UUID.fromString(postId))
                .text("A comment on this post")
                .build();

        mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/user/{userId}/commented", userId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void savePost_shouldReturn200() throws Exception {
        String postId = objectMapper.readTree(mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(
                                        PostRequest.builder().userId(userId).title("Savable").text("Save me").build()
                                ).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/v1/posts/saved/{postId}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void unsavePost_shouldReturn204() throws Exception {
        String postId = objectMapper.readTree(mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(
                                        PostRequest.builder().userId(userId).title("Unsavable").text("Unsave me").build()
                                ).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/posts/{postId}/save", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/posts/{postId}/save", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getSavedPosts_shouldReturn200() throws Exception {
        String postId = objectMapper.readTree(mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(
                                        PostRequest.builder().userId(userId).title("Saved Target").text("To be saved").build()
                                ).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/v1/posts/saved/{postId}", postId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/saved")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page-number", "1")
                        .param("page-size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void repost_shouldReturn200() throws Exception {
        String postId = objectMapper.readTree(mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(
                                        PostRequest.builder().userId(userId).title("Original").text("To be reposted").build()
                                ).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        var repostReq = dev.thural.quietspace.post.dto.RepostRequest.builder()
                .postId(UUID.fromString(postId))
                .text("Reposting this!")
                .build();

        mockMvc.perform(post("/api/v1/posts/repost")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repostReq)))
                .andExpect(status().isOk());
    }

    @Test
    void votePoll_shouldReturn200() throws Exception {
        var pollReq = dev.thural.quietspace.post.dto.PollRequest.builder()
                .dueDate(OffsetDateTime.now().plusDays(7))
                .options(List.of("Option A", "Option B"))
                .build();

        PostRequest postWithPoll = PostRequest.builder()
                .userId(userId)
                .title("Poll Post")
                .text("Vote on this poll")
                .poll(pollReq)
                .build();

        String postId = objectMapper.readTree(mockMvc.perform(multipart("/api/v1/posts")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "post", "", "application/json",
                                objectMapper.writeValueAsString(postWithPoll).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        var voteReq = dev.thural.quietspace.post.dto.VoteRequest.builder()
                .userId(userId)
                .postId(UUID.fromString(postId))
                .option("Option A")
                .build();

        mockMvc.perform(post("/api/v1/posts/vote-poll")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteReq)))
                .andExpect(status().isOk());
    }
}

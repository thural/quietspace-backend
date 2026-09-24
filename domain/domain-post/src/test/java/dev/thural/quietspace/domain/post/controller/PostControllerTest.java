package dev.thural.quietspace.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.post.PostService;
import dev.thural.quietspace.post.dto.PostRequest;
import dev.thural.quietspace.post.dto.PostResponse;
import dev.thural.quietspace.post.dto.VoteRequest;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "user", roles = "USER", authorities = "USER, ADMIN")
class PostControllerTest {

    MockMvc mockMvc;
    @Spy
    ObjectMapper objectMapper;

    @Mock
    PostService postService;

    @InjectMocks
    PostController postController;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;
    @Captor
    ArgumentCaptor<PostRequest> postRequestArgumentCaptor;

    private PostRequest postRequest;
    private VoteRequest voteRequest;
    private PostResponse postResponse;

    @BeforeEach
    public void setUp() {

        this.mockMvc = MockMvcBuilders.standaloneSetup(postController).build();

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("user")
                .email("user@email.com")
                .role(Role.ADMIN)
                .password("pAsSword")
                .build();

        Post post = Post.builder()
                .id(UUID.randomUUID())
                .user(user)
                .text("sample text")
                .title("sample title")
                .build();

        this.postRequest = PostRequest.builder()
                .userId(user.getId())
                .text("sample text")
                .title("sample title")
                .poll(null)
                .build();

        this.postResponse = PostResponse.builder()
                .id(post.getId())
                .text(post.getText())
                .username(post.getUser().getUsername())
                .title(post.getTitle())
                .build();

        this.voteRequest = VoteRequest.builder()
                .postId(post.getId())
                .userId(UUID.randomUUID())
                .build();
    }

    @Test
    void getAllPosts() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        when(postService.getAllPosts(1, 10)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get(PostController.POST_PATH)
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(postService, times(1)).getAllPosts(1, 10);

    }

    @Test
    void getPostsByQuery() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        when(postService.getAllByQuery(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get(PostController.POST_PATH + "/search")
                        .param("page-number", "1")
                        .param("page-size", "10")
                        .param("query", "sample")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(postService).getAllByQuery("sample", 1, 10);
    }

    @Test
    void createPost() throws Exception {
        when(postService.addPost(any(PostRequest.class))).thenReturn(postResponse);

        mockMvc.perform(multipart(PostController.POST_PATH)
                        .file(new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsString(postRequest).getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("photoData", "photo.jpg", "image/jpeg", "photo-content".getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(jsonPath("$.text", is(postResponse.getText())))
                .andExpect(jsonPath("$.username", is(postResponse.getUsername())))
                .andExpect(jsonPath("$.title", is(postResponse.getTitle())))
                .andExpect(jsonPath("$.id", is(postResponse.getId().toString())))
                .andExpect(status().isOk());

        verify(postService, times(1)).addPost(postRequestArgumentCaptor.capture());
        assertThat(postRequestArgumentCaptor.getValue().getUserId()).isEqualTo(postRequest.getUserId());
        assertThat(postRequestArgumentCaptor.getValue().getPhotoData()).isNotNull();
    }

    @Test
    void createPostWithoutPhoto() throws Exception {
        when(postService.addPost(any(PostRequest.class))).thenReturn(postResponse);

        mockMvc.perform(multipart(PostController.POST_PATH)
                        .file(new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsString(postRequest).getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(jsonPath("$.text", is(postResponse.getText())))
                .andExpect(jsonPath("$.username", is(postResponse.getUsername())))
                .andExpect(jsonPath("$.title", is(postResponse.getTitle())))
                .andExpect(jsonPath("$.id", is(postResponse.getId().toString())))
                .andExpect(status().isOk());

        verify(postService, times(1)).addPost(postRequestArgumentCaptor.capture());
        assertThat(postRequestArgumentCaptor.getValue().getPhotoData()).isNull();
    }

    @Test
    void createPostInvalidPayload() throws Exception {
        PostRequest invalidRequest = PostRequest.builder()
                .userId(null)
                .text("")
                .build();

        mockMvc.perform(multipart(PostController.POST_PATH)
                        .file(new MockMultipartFile("post", "", "application/json", objectMapper.writeValueAsString(invalidRequest).getBytes(StandardCharsets.UTF_8)))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPostById() throws Exception {
        when(postService.getPostById(any())).thenReturn(Optional.of(postResponse));

        mockMvc.perform(get(PostController.POST_PATH + "/" + postResponse.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(postService).getPostById(uuidArgumentCaptor.capture());
        assertThat(postResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void putPost() throws Exception {
        when(postService.updatePost(any(), any())).thenReturn(postResponse);

        mockMvc.perform(put(PostController.POST_PATH + "/" + postResponse.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(jsonPath("$.text", is(postResponse.getText())))
                .andExpect(jsonPath("$.username", is(postResponse.getUsername())))
                .andExpect(jsonPath("$.title", is(postResponse.getTitle())))
                .andExpect(jsonPath("$.id", is(postResponse.getId().toString())))
                .andExpect(status().isOk());

        verify(postService, times(1)).updatePost(uuidArgumentCaptor.capture(), postRequestArgumentCaptor.capture());
        assertThat(postResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
        assertThat(postResponse.getText()).isEqualTo(postRequestArgumentCaptor.getValue().getText());
    }

    @Test
    void deletePost() throws Exception {
        mockMvc.perform(delete(PostController.POST_PATH + "/" + postResponse.getId()))
                .andExpect(status().isNoContent());

        verify(postService, times(1)).deletePost(uuidArgumentCaptor.capture());
        assertThat(postResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
    }

    @Test
    void patchPost() throws Exception {
        when(postService.patchPost(any(), any())).thenReturn(postResponse);

        mockMvc.perform(patch(PostController.POST_PATH + "/" + postResponse.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(jsonPath("$.text", is(postResponse.getText())))
                .andExpect(jsonPath("$.username", is(postResponse.getUsername())))
                .andExpect(jsonPath("$.title", is(postResponse.getTitle())))
                .andExpect(jsonPath("$.id", is(postResponse.getId().toString())))
                .andExpect(status().isOk());

        verify(postService).patchPost(uuidArgumentCaptor.capture(), postRequestArgumentCaptor.capture());
        assertThat(postResponse.getId()).isEqualTo(uuidArgumentCaptor.getValue());
        assertThat(postResponse.getText()).isEqualTo(postRequestArgumentCaptor.getValue().getText());
    }

    @Test
    void votePoll() throws Exception {
        ArgumentCaptor<VoteRequest> voteRequestArgumentCaptor = ArgumentCaptor.forClass(VoteRequest.class);

        mockMvc.perform(post(PostController.POST_PATH + "/" + "vote-poll")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isOk());

        verify(postService).votePoll(voteRequestArgumentCaptor.capture());
        assertThat(voteRequestArgumentCaptor.getValue().getPostId()).isEqualTo(voteRequest.getPostId());
    }
}

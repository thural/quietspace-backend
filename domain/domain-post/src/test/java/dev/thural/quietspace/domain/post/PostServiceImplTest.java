package dev.thural.quietspace.post;

import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.post.*;
import dev.thural.quietspace.post.dto.PostRequest;
import dev.thural.quietspace.post.dto.PostResponse;
import dev.thural.quietspace.post.dto.RepostRequest;
import dev.thural.quietspace.post.dto.VoteRequest;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.shared.util.PagingProvider;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private UserService userService;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostSpecifications postSpecifications;
    @Mock
    private PhotoService photoService;

    @InjectMocks
    private PostServiceImpl postService;

    @SuppressWarnings("unchecked")
    private <T> Specification<T> anySpec() {
        return any(Specification.class);
    }

    private User user;
    private Post post;
    private Poll poll;
    private PostRequest postRequest;
    private VoteRequest voteRequest;
    private PostResponse postResponse;

    @BeforeEach
    public void setUp() {
        UUID userId = UUID.randomUUID();

        this.user = User.builder()
                .id(userId)
                .username("user")
                .email("user@email.com")
                .role(Role.USER)
                .password("pAsSword")
                .build();

        this.post = Post.builder()
                .id(UUID.randomUUID())
                .user(user)
                .text("sample text")
                .poll(poll)
                .build();

        Set<UUID> userIds = new HashSet<>();
        userIds.add(userId);

        PollOption option = PollOption.builder()
                .label("sample label")
                .poll(poll)
                .votes(userIds)
                .build();

        this.poll = Poll.builder()
                .options(List.of(option))
                .build();

        this.post.setPoll(this.poll);

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
                .option("sample label")
                .userId(userId)
                .build();
    }

    @Test
    void getAllPosts_shouldReturnPosts() {
        Specification<Post> mockSpec = Specification.where((root, query, cb) -> cb.conjunction());
        when(postSpecifications.visibleToUser()).thenReturn(mockSpec);
        when(postRepository.findAll(anySpec(), any(PageRequest.class))).thenReturn(Page.empty());

        Page<PostResponse> posts = postService.getAllPosts(1, 50);

        assertThat(posts.getContent()).isEmpty();
        verify(postRepository, times(1)).findAll(anySpec(), any(PageRequest.class));
    }

    @Test
    void getPostByUserId_shouldReturnPosts() {
        Specification<Post> mockSpec = Specification.where((root, query, cb) -> cb.conjunction());
        when(postSpecifications.visibleToUser()).thenReturn(mockSpec);
        when(postRepository.findAll(anySpec(), any(PageRequest.class))).thenReturn(Page.empty());

        Page<PostResponse> posts = postService.getPostsByUserId(user.getId(), 1, 50);

        assertThat(posts.getContent()).isEmpty();
        verify(postRepository, times(1)).findAll(anySpec(), any(PageRequest.class));
    }

    @Test
    void addPostById_shouldReturnPost() {
        when(userService.getSignedUser()).thenReturn(user);
        when(postMapper.postRequestToEntity(any(PostRequest.class))).thenReturn(post);
        when(postMapper.postEntityToResponse(post)).thenReturn(postResponse);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        PostResponse postResponse = postService.addPost(postRequest);

        assertThat(postResponse).isInstanceOf(PostResponse.class);
        verify(postRepository, times(1)).save(post);
    }

    @Test
    void getVotedOptionLabel_shouldReturnLabel() {
        when(userService.getSignedUser()).thenReturn(user);
        String optionLabel = postService.getVotedPollOptionLabel(poll);

        assertThat(optionLabel).isEqualTo("sample label");
    }

    @Test
    void getPostResponseById_shouldReturnPost() {
        when(postRepository.findById(post.getId())).thenReturn(Optional.ofNullable(post));
        when(postMapper.postEntityToResponse(post)).thenReturn(postResponse);

        PostResponse foundPost = postService.getPostById(post.getId()).orElse(null);

        assertThat(foundPost).isEqualTo(postResponse);
        verify(postRepository, times(1)).findById(post.getId());
    }

    @Test
    void updatePost_shouldReturnPost() {
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.ofNullable(post));
        when(postMapper.postEntityToResponse(post)).thenReturn(postResponse);

        PostResponse responseBody = postService.updatePost(post.getId(), postRequest);

        assertThat(responseBody).isEqualTo(postResponse);
        verify(postRepository, times(1)).findById(post.getId());
    }

    @Test
    void votePoll_shouldSucceed() {
        when(postRepository.findById(post.getId())).thenReturn(Optional.ofNullable(post));
        postService.votePoll(voteRequest);

        Set<UUID> voterIds = postRepository.findById(post.getId()).orElseThrow()
                .getPoll().getOptions().stream()
                .filter(option -> option.getLabel().equals(voteRequest.getOption()))
                .findFirst().map(PollOption::getVotes).orElseThrow();

        assertThat(voterIds).contains(voteRequest.getUserId());
        verify(postRepository, times(2)).findById(post.getId());
    }

    @Test
    void deletePost_shouldSucceed() {
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.ofNullable(post));

        postService.deletePost(post.getId());

        verify(postRepository, times(1)).findById(post.getId());
        verify(postRepository, times(1)).deleteById(post.getId());
    }

    @Test
    void patchPost_givenAuthor_shouldUpdateFieldsAndReturn() {
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postMapper.postEntityToResponse(post)).thenReturn(postResponse);

        PostResponse result = postService.patchPost(post.getId(), postRequest);

        assertThat(result).isEqualTo(postResponse);
    }

    @Test
    void patchPost_givenNonAuthor_shouldThrow() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        Post otherPost = Post.builder().id(UUID.randomUUID()).user(otherUser).build();
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(otherPost.getId())).thenReturn(Optional.of(otherPost));

        assertThatThrownBy(() -> postService.patchPost(otherPost.getId(), postRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void patchPost_givenNullPhotoData_shouldDeleteExistingPhoto() {
        PostRequest requestWithoutPhoto = PostRequest.builder()
                .userId(user.getId()).text("updated").photoData(null).build();
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postMapper.postEntityToResponse(post)).thenReturn(postResponse);

        postService.patchPost(post.getId(), requestWithoutPhoto);

        verify(photoService).deletePhotoByEntityId(post.getId());
    }

    @Test
    void getAllByQuery_givenSearchText_shouldReturnFilteredPage() {
        Specification<Post> mockSpec = Specification.where((root, query, cb) -> cb.conjunction());
        when(postSpecifications.visibleToUser()).thenReturn(mockSpec);
        when(postSpecifications.containsText(any())).thenReturn(mockSpec);
        PageRequest pageRequest = PagingProvider.buildPageRequest(0, 10, null);
        when(postRepository.findAll(anySpec(), any(PageRequest.class))).thenReturn(Page.empty());

        Page<PostResponse> result = postService.getAllByQuery("test", 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllByQuery_givenNullSearchText_shouldReturnAllVisible() {
        Specification<Post> mockSpec = Specification.where((root, query, cb) -> cb.conjunction());
        when(postSpecifications.visibleToUser()).thenReturn(mockSpec);
        when(postRepository.findAll(anySpec(), any(PageRequest.class))).thenReturn(Page.empty());

        Page<PostResponse> result = postService.getAllByQuery(null, 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void addRepost_givenValidRequest_shouldSaveAndReturn() {
        RepostRequest repostRequest = RepostRequest.builder().build();
        Post repostPost = Post.builder().id(UUID.randomUUID()).build();
        when(postMapper.repostRequestToEntity(repostRequest)).thenReturn(repostPost);
        when(postRepository.save(repostPost)).thenReturn(repostPost);
        when(postMapper.postEntityToResponse(repostPost)).thenReturn(postResponse);

        PostResponse result = postService.addRepost(repostRequest);

        assertThat(result).isEqualTo(postResponse);
        verify(postRepository).save(repostPost);
    }

    @Test
    void getSavedPostsByUser_shouldReturnSavedPostsPage() {
        Specification<Post> mockSpec = Specification.where((root, query, cb) -> cb.conjunction());
        when(postSpecifications.visibleToUser()).thenReturn(mockSpec);
        when(postSpecifications.savedByUser(any())).thenReturn(mockSpec);
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findAll(anySpec(), any(PageRequest.class))).thenReturn(Page.empty());

        Page<PostResponse> result = postService.getSavedPostsByUser(0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void savePostForUser_givenExistingPost_shouldAddToSaved() {
        user.setSavedPosts(new ArrayList<>());
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postService.savePostForUser(post.getId());

        assertThat(user.getSavedPosts()).contains(post);
    }

    @Test
    void savePostForUser_givenNonExistentPost_shouldThrow() {
        when(postRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.savePostForUser(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void unsavePostForUser_givenSavedPost_shouldRemove() {
        user.setSavedPosts(new ArrayList<>(List.of(post)));
        when(userService.getSignedUser()).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postService.unsavePostForUser(post.getId());

        assertThat(user.getSavedPosts()).doesNotContain(post);
    }

    @Test
    void getCommentedPostsByUserId_givenValidUser_shouldReturnPage() {
        Specification<Post> mockSpec = Specification.where((root, query, cb) -> cb.conjunction());
        when(postSpecifications.visibleToUser()).thenReturn(mockSpec);
        when(postSpecifications.commentedByUser(any())).thenReturn(mockSpec);
        when(postRepository.findAll(anySpec(), any(PageRequest.class))).thenReturn(Page.empty());

        Page<PostResponse> result = postService.getCommentedPostsByUserId(user.getId(), 0, 10);

        assertThat(result).isEmpty();
    }

}

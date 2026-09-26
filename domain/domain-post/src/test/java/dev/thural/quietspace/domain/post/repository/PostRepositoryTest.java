package dev.thural.quietspace.domain.post.repository;

import dev.thural.quietspace.domain.comment.Comment;
import dev.thural.quietspace.domain.comment.CommentRepository;
import dev.thural.quietspace.domain.post.Post;
import dev.thural.quietspace.domain.post.PostRepository;
import dev.thural.quietspace.domain.post.PostSpecifications;
import dev.thural.quietspace.core.shared.enums.Role;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.api.UserQueryAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    CommentRepository commentRepository;

    private final User user = User.builder()
            .email("user@email.com")
            .username("user")
            .firstname("firstname")
            .lastname("lastname")
            .password("78921731")
            .accountLocked(false)
            .role(Role.USER)
            .username("test user")
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private final Post post = Post.builder()
            .text("sample text")
            .authorId(UUID.randomUUID())
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private User savedUser;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        this.savedUser = userRepository.save(user);
        post.setUser(savedUser);
        post.setAuthorId(savedUser.getId());
        this.savedPost = postRepository.save(post);
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(user);
        postRepository.delete(post);
    }

    @Test
    void testGetPostsByUserId() {
        Page<Post> list = postRepository.findAllByUserId(user.getId(), null);
        assertThat(list.toList().size()).isEqualTo(1);
        assertThat(list.toList().get(0)).isEqualTo(savedPost);
    }

    @Test
    void testFindAllByQuery() {
        Page<Post> list = postRepository.findAllByQuery("sample", null);
        assertThat(list.toList().size()).isEqualTo(1);
        assertThat(list.toList().get(0)).isEqualTo(savedPost);
    }

    @Test
    void testContainsText_shouldMatchTitle() {
        var specs = new PostSpecifications(
                org.mockito.Mockito.mock(UserService.class),
                new UserQueryAdapter(userRepository),
                org.mockito.Mockito.mock(dev.thural.quietspace.domain.comment.api.CommentQueryPort.class));
        Page<Post> list = postRepository.findAll(specs.containsText("sample"), Pageable.unpaged());
        assertThat(list.toList()).hasSize(1);
    }

    @Test
    void testContainsText_givenUnknownTerm_shouldReturnEmpty() {
        var specs = new PostSpecifications(
                org.mockito.Mockito.mock(UserService.class),
                new UserQueryAdapter(userRepository),
                org.mockito.Mockito.mock(dev.thural.quietspace.domain.comment.api.CommentQueryPort.class));
        Page<Post> list = postRepository.findAll(specs.containsText("zzz-no-match"), Pageable.unpaged());
        assertThat(list.toList()).isEmpty();
    }

    @Test
    void testFindSavedPostsByUserId() {
        savedUser.getSavedPostIds().add(savedPost.getId());
        userRepository.save(savedUser);
        Page<Post> list = postRepository.findSavedPostsByIds(savedUser.getSavedPostIds(), null);
        assertThat(list.toList()).hasSize(1);
    }

    @Test
    void testFindByCommentsUserId() {
        Comment comment = Comment.builder()
                .text("test comment")
                .userId(savedUser.getId())
                .postId(savedPost.getId())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        commentRepository.save(comment);
        Page<Post> list = postRepository.findByCommentsUserId(savedUser.getId(), null);
        assertThat(list.toList()).hasSize(1);
    }

    @Test
    void testDeleteByRepostId() {
        Post repost = Post.builder()
                .text("repost")
                .user(savedUser)
                .authorId(savedUser.getId())
                .repostId(savedPost.getId().toString())
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        postRepository.save(repost);
        postRepository.deleteByRepostId(savedPost.getId().toString());
        Page<Post> list = postRepository.findAllByQuery("repost", null);
        assertThat(list.toList()).isEmpty();
    }
}

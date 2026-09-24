package dev.thural.quietspace.post;

import dev.thural.quietspace.comment.Comment;
import dev.thural.quietspace.comment.CommentRepository;
import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.post.PostRepository;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;

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
            .user(user)
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private User savedUser;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        this.savedUser = userRepository.save(user);
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
    void testFindSavedPostsByUserId() {
        savedUser.getSavedPosts().add(savedPost);
        userRepository.save(savedUser);
        Page<Post> list = postRepository.findSavedPostsByUserId(savedUser.getId(), null);
        assertThat(list.toList()).hasSize(1);
    }

    @Test
    void testFindByCommentsUserId() {
        Comment comment = Comment.builder()
                .text("test comment")
                .user(savedUser)
                .post(savedPost)
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
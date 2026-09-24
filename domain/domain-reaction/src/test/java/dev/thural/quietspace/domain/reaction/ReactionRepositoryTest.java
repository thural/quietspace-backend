package dev.thural.quietspace.reaction;

import dev.thural.quietspace.post.Post;
import dev.thural.quietspace.post.PostRepository;
import dev.thural.quietspace.reaction.Reaction;
import dev.thural.quietspace.reaction.ReactionRepository;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.reaction.ReactionType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReactionRepositoryTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    ReactionRepository reactionRepository;

    private final User user = User.builder()
            .email("user@email.com")
            .username("user")
            .firstname("firstname")
            .lastname("lastname")
            .password("78921731")
            .accountLocked(false)
            .username("test user")
            .role(Role.USER)
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private final Post post = Post.builder()
            .text("sample text")
            .user(user)
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private Reaction reaction = Reaction.builder()
            .userId(null)
            .contentId(null)
            .contentType(EntityType.POST)
            .reactionType(ReactionType.LIKE)
            .username(user.getUsername())
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private User savedUser;
    private Post savedPost;
    private Reaction savedReaction;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(user);
        savedPost = postRepository.save(post);
        reaction.setContentId(savedPost.getId());
        reaction.setUserId(savedUser.getId());
        savedReaction = reactionRepository.save(reaction);
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(savedUser);
        postRepository.delete(savedPost);
        reactionRepository.delete(savedReaction);
    }

    @Test
    void findAllByContentId() {
        Page<Reaction> reactions = reactionRepository.findAllByContentId(savedReaction.getContentId(), null);
        assertThat(reactions.toList().size()).isEqualTo(1);
        assertThat(reactions.toList().get(0)).isEqualTo(savedReaction);
    }

    @Test
    void findAllByUserId() {
        Page<Reaction> reactions = reactionRepository.findAllByUserId(savedUser.getId(), null);
        assertThat(reactions.toList().size()).isEqualTo(1);
        assertThat(reactions.toList().get(0)).isEqualTo(savedReaction);
    }

    @Test
    void existsByContentIdAndUserId() {
        boolean exists = reactionRepository.existsByContentIdAndUserId(post.getId(), user.getId());
        assertThat(exists).isTrue();
    }

    @Test
    void findByContentIdAndUserId() {
        Optional<Reaction> reaction = reactionRepository.findByContentIdAndUserId(post.getId(), user.getId());
        assertThat(reaction.isPresent()).isTrue();
        assertThat(reaction.get()).isEqualTo(savedReaction);
    }

    @Test
    void findAllByContentTypeAndUserId() {
        Page<Reaction> reactions = reactionRepository.findAllByContentTypeAndUserId(EntityType.POST, savedUser.getId(), null);
        assertThat(reactions.toList().size()).isEqualTo(1);
        assertThat(reactions.toList().get(0)).isEqualTo(savedReaction);
    }

    @Test
    void findAllByContentIdAndContentType() {
        Page<Reaction> reactions = reactionRepository.findAllByContentId(savedReaction.getContentId(), null);
        assertThat(reactions.toList().size()).isEqualTo(1);
        assertThat(reactions.toList().get(0)).isEqualTo(savedReaction);
    }

    @Test
    void findAllByUserIdAndContentType() {
        Page<Reaction> reactions = reactionRepository.findAllByUserIdAndContentType(savedUser.getId(), EntityType.POST, null);
        assertThat(reactions.toList().size()).isEqualTo(1);
        assertThat(reactions.toList().get(0)).isEqualTo(savedReaction);
    }

    @Test
    void countByContentIdAndLikeType() {
        long count = reactionRepository.countByContentIdAndReactionType(post.getId(), ReactionType.LIKE);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findAllByContentIdAndLikeType() {
        Page<Reaction> reactions = reactionRepository.findAllByContentIdAndReactionType(post.getId(), ReactionType.LIKE, null);
        assertThat(reactions.toList().size()).isEqualTo(1);
        assertThat(reactions.toList().get(0)).isEqualTo(savedReaction);
    }
}
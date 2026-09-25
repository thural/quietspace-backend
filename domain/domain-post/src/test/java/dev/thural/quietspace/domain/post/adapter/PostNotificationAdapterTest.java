package dev.thural.quietspace.domain.post.adapter;

import dev.thural.quietspace.domain.post.Post;
import dev.thural.quietspace.domain.post.PostRepository;
import dev.thural.quietspace.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostNotificationAdapterTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostNotificationAdapter adapter;

    @Test
    void findPostOwnerId_givenExistingPost_shouldReturnOwnerId() {
        UUID postId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Post post = Post.builder()
                .id(postId)
                .user(User.builder().id(ownerId).username("owner").build())
                .text("hello")
                .build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThat(adapter.findPostOwnerId(postId)).isEqualTo(ownerId);
    }

    @Test
    void findPostOwnerId_givenMissingPost_shouldThrow() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findPostOwnerId(postId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

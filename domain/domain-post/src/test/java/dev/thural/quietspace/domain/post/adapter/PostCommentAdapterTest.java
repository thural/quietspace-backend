package dev.thural.quietspace.domain.post.adapter;

import dev.thural.quietspace.domain.post.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCommentAdapterTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostCommentAdapter adapter;

    @Test
    void postExists_givenExistingPost_shouldReturnTrue() {
        UUID postId = UUID.randomUUID();
        when(postRepository.existsById(postId)).thenReturn(true);

        assertThat(adapter.postExists(postId)).isTrue();
    }

    @Test
    void postExists_givenMissingPost_shouldReturnFalse() {
        UUID postId = UUID.randomUUID();
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThat(adapter.postExists(postId)).isFalse();
    }
}

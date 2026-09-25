package dev.thural.quietspace.domain.post.health;

import dev.thural.quietspace.domain.post.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostHealthIndicatorTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostHealthIndicator healthIndicator;

    @Test
    void health_givenRepositoryWorks_shouldBeUpWithCount() {
        when(postRepository.count()).thenReturn(3L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalPosts", 3L);
    }

    @Test
    void health_givenRepositoryFails_shouldBeDown() {
        when(postRepository.count()).thenThrow(new RuntimeException("db down"));

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}

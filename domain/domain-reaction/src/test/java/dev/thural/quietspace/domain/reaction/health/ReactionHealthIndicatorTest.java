package dev.thural.quietspace.domain.reaction.health;

import dev.thural.quietspace.domain.reaction.ReactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionHealthIndicatorTest {

    @Mock
    private ReactionRepository reactionRepository;

    @InjectMocks
    private ReactionHealthIndicator healthIndicator;

    @Test
    void health_givenRepositoryWorks_shouldBeUpWithCount() {
        when(reactionRepository.count()).thenReturn(9L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalReactions", 9L);
    }

    @Test
    void health_givenRepositoryFails_shouldBeDown() {
        when(reactionRepository.count()).thenThrow(new RuntimeException("db down"));

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}

package dev.thural.quietspace.domain.user.health;

import dev.thural.quietspace.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserHealthIndicatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserHealthIndicator healthIndicator;

    @Test
    void health_givenRepositoryWorks_shouldBeUpWithCount() {
        when(userRepository.count()).thenReturn(42L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalUsers", 42L);
    }

    @Test
    void health_givenRepositoryFails_shouldBeDown() {
        when(userRepository.count()).thenThrow(new RuntimeException("db down"));

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}

package dev.thural.quietspace.domain.photo.health;

import dev.thural.quietspace.domain.photo.PhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoHealthIndicatorTest {

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private PhotoHealthIndicator healthIndicator;

    @Test
    void health_givenRepositoryWorks_shouldBeUpWithCount() {
        when(photoRepository.count()).thenReturn(10L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalPhotos", 10L);
    }

    @Test
    void health_givenRepositoryFails_shouldBeDown() {
        when(photoRepository.count()).thenThrow(new RuntimeException("db down"));

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
package dev.thural.quietspace.domain.chat.health;

import dev.thural.quietspace.domain.chat.ChatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHealthIndicatorTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatHealthIndicator healthIndicator;

    @Test
    void health_givenRepositoryWorks_shouldBeUpWithCount() {
        when(chatRepository.count()).thenReturn(5L);

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalChats", 5L);
    }

    @Test
    void health_givenRepositoryFails_shouldBeDown() {
        when(chatRepository.count()).thenThrow(new RuntimeException("db down"));

        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
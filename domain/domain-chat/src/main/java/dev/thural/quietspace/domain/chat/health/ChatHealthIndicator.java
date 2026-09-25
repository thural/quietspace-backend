package dev.thural.quietspace.domain.chat.health;

import dev.thural.quietspace.domain.chat.ChatRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ChatHealthIndicator implements HealthIndicator {

    private final ChatRepository chatRepository;

    public ChatHealthIndicator(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public Health health() {
        try {
            long count = chatRepository.count();
            return Health.up()
                    .withDetail("totalChats", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

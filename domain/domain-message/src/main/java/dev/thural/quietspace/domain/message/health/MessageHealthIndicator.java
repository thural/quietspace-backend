package dev.thural.quietspace.domain.message.health;

import dev.thural.quietspace.domain.message.MessageRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MessageHealthIndicator implements HealthIndicator {

    private final MessageRepository messageRepository;

    public MessageHealthIndicator(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public Health health() {
        try {
            long count = messageRepository.count();
            return Health.up()
                    .withDetail("totalMessages", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

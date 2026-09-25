package dev.thural.quietspace.domain.reaction.health;

import dev.thural.quietspace.domain.reaction.ReactionRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ReactionHealthIndicator implements HealthIndicator {

    private final ReactionRepository reactionRepository;

    public ReactionHealthIndicator(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }

    @Override
    public Health health() {
        try {
            long count = reactionRepository.count();
            return Health.up()
                    .withDetail("totalReactions", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

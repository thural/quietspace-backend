package dev.thural.quietspace.domain.comment.health;

import dev.thural.quietspace.domain.comment.CommentRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CommentHealthIndicator implements HealthIndicator {

    private final CommentRepository commentRepository;

    public CommentHealthIndicator(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    public Health health() {
        try {
            long count = commentRepository.count();
            return Health.up()
                    .withDetail("totalComments", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

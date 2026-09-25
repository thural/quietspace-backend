package dev.thural.quietspace.domain.post.health;

import dev.thural.quietspace.domain.post.PostRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PostHealthIndicator implements HealthIndicator {

    private final PostRepository postRepository;

    public PostHealthIndicator(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public Health health() {
        try {
            long count = postRepository.count();
            return Health.up()
                    .withDetail("totalPosts", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

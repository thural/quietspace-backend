package dev.thural.quietspace.domain.user.health;

import dev.thural.quietspace.domain.user.UserRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class UserHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    public UserHealthIndicator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Health health() {
        try {
            long count = userRepository.count();
            return Health.up()
                    .withDetail("totalUsers", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

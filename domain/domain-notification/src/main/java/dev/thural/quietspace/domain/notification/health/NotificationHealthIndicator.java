package dev.thural.quietspace.domain.notification.health;

import dev.thural.quietspace.domain.notification.NotificationRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class NotificationHealthIndicator implements HealthIndicator {

    private final NotificationRepository notificationRepository;

    public NotificationHealthIndicator(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Health health() {
        try {
            long count = notificationRepository.count();
            return Health.up()
                    .withDetail("totalNotifications", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

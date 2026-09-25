package dev.thural.quietspace.domain.photo.health;

import dev.thural.quietspace.domain.photo.PhotoRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class PhotoHealthIndicator implements HealthIndicator {

    private final PhotoRepository photoRepository;

    public PhotoHealthIndicator(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    @Override
    public Health health() {
        try {
            long count = photoRepository.count();
            return Health.up()
                    .withDetail("totalPhotos", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}

package dev.thural.quietspace.shared.messaging;

import dev.thural.quietspace.shared.event.OutboxEvent;
import dev.thural.quietspace.shared.event.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> unpublishedEvents = outboxEventRepository.findUnpublishedEvents();
        
        if (unpublishedEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} unpublished outbox events", unpublishedEvents.size());

        for (OutboxEvent event : unpublishedEvents) {
            try {
                outboxEventPublisher.publish(event);
                event.setPublishedAt(java.time.OffsetDateTime.now());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage(), e);
            }
        }
    }
}
package dev.thural.quietspace.domain.post;

import dev.thural.quietspace.core.shared.event.ProcessedEvent;
import dev.thural.quietspace.core.shared.event.ProcessedEventRepository;
import dev.thural.quietspace.core.shared.event.UserFollowedEvent;
import dev.thural.quietspace.core.shared.event.UserPrivacyChangedEvent;
import dev.thural.quietspace.core.shared.event.UserRegisteredEvent;
import dev.thural.quietspace.core.shared.event.UserUnfollowedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Maintains the feed-visibility read model ({@code post_author_visibility},
 * {@code viewer_author_access}) from user lifecycle events (see ADR 005).
 *
 * <p>Idempotent via {@code processed_events}: at-least-once delivery is safe.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostVisibilityProjector {

    private final PostAuthorVisibilityRepository visibilityRepository;
    private final ViewerAuthorAccessRepository accessRepository;
    private final ProcessedEventRepository processedEventRepository;

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        if (alreadyProcessed(event.getEventId(), event.getEventType())) {
            return;
        }
        visibilityRepository.findById(event.getAggregateId())
                .orElseGet(() -> visibilityRepository.save(PostAuthorVisibility.builder()
                        .authorId(event.getAggregateId())
                        .isPrivate(false)
                        .build()));
        markProcessed(event.getEventId(), event.getEventType());
    }

    @EventListener
    @Transactional
    public void onUserPrivacyChanged(UserPrivacyChangedEvent event) {
        if (alreadyProcessed(event.getEventId(), event.getEventType())) {
            return;
        }
        PostAuthorVisibility visibility = visibilityRepository.findById(event.getUserId())
                .orElse(PostAuthorVisibility.builder().authorId(event.getUserId()).build());
        visibility.setIsPrivate(event.isPrivate());
        visibilityRepository.save(visibility);
        markProcessed(event.getEventId(), event.getEventType());
    }

    @EventListener
    @Transactional
    public void onUserFollowed(UserFollowedEvent event) {
        if (alreadyProcessed(event.getEventId(), event.getEventType())) {
            return;
        }
        if (!accessRepository.existsByViewerIdAndAuthorId(event.getFollowerId(), event.getFollowedId())) {
            accessRepository.save(ViewerAuthorAccess.builder()
                    .viewerId(event.getFollowerId())
                    .authorId(event.getFollowedId())
                    .build());
        }
        markProcessed(event.getEventId(), event.getEventType());
    }

    @EventListener
    @Transactional
    public void onUserUnfollowed(UserUnfollowedEvent event) {
        if (alreadyProcessed(event.getEventId(), event.getEventType())) {
            return;
        }
        accessRepository.deleteByViewerIdAndAuthorId(event.getFollowerId(), event.getFollowedId());
        markProcessed(event.getEventId(), event.getEventType());
    }

    private boolean alreadyProcessed(java.util.UUID eventId, String eventType) {
        if (processedEventRepository.existsByEventId(eventId)) {
            log.debug("Event {} already processed, skipping", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(java.util.UUID eventId, String eventType) {
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(OffsetDateTime.now())
                .build());
    }
}

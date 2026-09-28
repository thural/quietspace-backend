package dev.thural.quietspace.core.shared.event;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;
    private final ApplicationEventPublisher applicationEventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public <T extends DomainEvent> void publish(T event) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(eventSerializer.serialize(event))
                .createdAt(OffsetDateTime.now())
                .build();

        outboxEventRepository.save(outboxEvent);
        entityManager.flush();

        log.debug("Published domain event {} with aggregateId {} to outbox", event.getEventType(), event.getAggregateId());
    }

    @Transactional
    public <T extends DomainEvent> void publishFromEntity(BaseEntity entity, T event) {
        event.setAggregateType(entity.getClass().getSimpleName());
        event.setAggregateId(entity.getId());
        publish(event);
    }
}
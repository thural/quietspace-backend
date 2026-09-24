package dev.thural.quietspace.core.shared.event;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public abstract class DomainEvent {

    private UUID eventId = UUID.randomUUID();
    private OffsetDateTime timestamp = OffsetDateTime.now();
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
}
package dev.thural.quietspace.core.shared.event;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_processed_events_event_id", columnList = "eventId", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent extends BaseEntity {

    @Id    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "processed_at", nullable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime processedAt;
}
package dev.thural.quietspace.core.shared.event;

import dev.thural.quietspace.core.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

    @Id    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 255)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime createdAt;

    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime publishedAt;
}
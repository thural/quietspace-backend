package dev.thural.quietspace.core.shared.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL ORDER BY o.createdAt ASC")
    Page<OutboxEvent> findUnpublishedEvents(Pageable pageable);

    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    @Query("SELECT o FROM OutboxEvent o WHERE o.aggregateType = :aggregateType AND o.aggregateId = :aggregateId")
    List<OutboxEvent> findByAggregate(UUID aggregateId, String aggregateType);

    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NOT NULL AND o.createdAt < :cutoffDate")
    List<OutboxEvent> findOldPublishedEvents(OffsetDateTime cutoffDate);
}
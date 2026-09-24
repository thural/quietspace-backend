package dev.thural.quietspace.core.shared.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ProcessedEvent p WHERE p.eventId = :eventId")
    boolean existsByEventId(UUID eventId);
}
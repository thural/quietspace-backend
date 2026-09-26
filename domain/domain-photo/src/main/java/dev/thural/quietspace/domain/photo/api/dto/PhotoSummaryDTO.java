package dev.thural.quietspace.domain.photo.api.dto;

import dev.thural.quietspace.core.shared.enums.EntityType;

import java.util.UUID;

/**
 * Public read-model contract of the photo domain for cross-domain consumers.
 *
 * <p>Lightweight metadata snapshot — no binary payload. Consumers needing the
 * bytes (download endpoints) keep using {@code PhotoService}.</p>
 */
public record PhotoSummaryDTO(
        UUID id,
        String name,
        String type,
        UUID userId,
        UUID entityId,
        EntityType entityType
) {
}

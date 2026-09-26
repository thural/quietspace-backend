package dev.thural.quietspace.domain.user.api.dto;

import dev.thural.quietspace.core.shared.enums.StatusType;

import java.util.UUID;

/**
 * Public read-model contract of the user domain for cross-domain consumers.
 *
 * <p>Immutable snapshot — consumers must treat it as a value object and never
 * persist or mutate it. Photo resolution (photoId &rarr; URL) stays in
 * domain-photo via {@code PhotoQueryPort}.</p>
 */
public record UserSummaryDTO(
        UUID id,
        String username,
        String displayName,
        UUID photoId,
        StatusType statusType
) {
}

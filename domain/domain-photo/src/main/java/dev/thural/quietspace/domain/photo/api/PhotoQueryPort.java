package dev.thural.quietspace.domain.photo.api;

import dev.thural.quietspace.domain.photo.api.dto.PhotoSummaryDTO;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inbound query port owned by the photo domain.
 *
 * <p>Cross-domain metadata reads (attachments in posts/messages, avatars in
 * user profiles) go through this port — never through
 * {@code PhotoRepository} directly.</p>
 */
public interface PhotoQueryPort {

    /**
     * @return metadata snapshot, or empty if no photo with the id exists
     */
    Optional<PhotoSummaryDTO> getPhotoSummary(UUID photoId);

    /**
     * @return metadata snapshot, or empty if no photo attached to the entity
     */
    Optional<PhotoSummaryDTO> getPhotoSummaryByEntityId(UUID entityId);

    /**
     * Batch variant to avoid N+1 lookups when rendering lists.
     *
     * @return snapshots keyed by photo id; missing ids are absent from the map
     */
    Map<UUID, PhotoSummaryDTO> getPhotosSummary(Set<UUID> photoIds);
}

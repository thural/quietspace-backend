package dev.thural.quietspace.domain.photo.api;

import dev.thural.quietspace.domain.photo.Photo;
import dev.thural.quietspace.domain.photo.PhotoRepository;
import dev.thural.quietspace.domain.photo.api.dto.PhotoSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default {@link PhotoQueryPort} implementation backed by {@link PhotoRepository}.
 */
@Component
@RequiredArgsConstructor
public class PhotoQueryAdapter implements PhotoQueryPort {

    private final PhotoRepository photoRepository;

    @Override
    public Optional<PhotoSummaryDTO> getPhotoSummary(UUID photoId) {
        return photoRepository.findById(photoId).map(this::toSummary);
    }

    @Override
    public Optional<PhotoSummaryDTO> getPhotoSummaryByEntityId(UUID entityId) {
        return photoRepository.findByEntityId(entityId).map(this::toSummary);
    }

    @Override
    public Map<UUID, PhotoSummaryDTO> getPhotosSummary(Set<UUID> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return photoRepository.findAllById(photoIds).stream()
                .collect(Collectors.toMap(Photo::getId, this::toSummary));
    }

    private PhotoSummaryDTO toSummary(Photo photo) {
        return new PhotoSummaryDTO(
                photo.getId(),
                photo.getName(),
                photo.getType(),
                photo.getUserId(),
                photo.getEntityId(),
                photo.getEntityType()
        );
    }
}

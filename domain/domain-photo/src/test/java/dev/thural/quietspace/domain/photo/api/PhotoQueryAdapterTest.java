package dev.thural.quietspace.domain.photo.api;

import dev.thural.quietspace.core.shared.enums.EntityType;
import dev.thural.quietspace.domain.photo.Photo;
import dev.thural.quietspace.domain.photo.PhotoRepository;
import dev.thural.quietspace.domain.photo.api.dto.PhotoSummaryDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoQueryAdapterTest {

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private PhotoQueryAdapter adapter;

    private static Photo photo(UUID id, UUID entityId) {
        return Photo.builder()
                .id(id)
                .name("pic.jpg")
                .type("image/jpeg")
                .data(new byte[]{1})
                .userId(UUID.randomUUID())
                .entityId(entityId)
                .entityType(EntityType.POST)
                .build();
    }

    @Test
    void getPhotoSummary_givenExistingPhoto_shouldReturnSnapshotWithoutPayload() {
        UUID id = UUID.randomUUID();
        when(photoRepository.findById(id)).thenReturn(Optional.of(photo(id, UUID.randomUUID())));

        Optional<PhotoSummaryDTO> result = adapter.getPhotoSummary(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().name()).isEqualTo("pic.jpg");
        assertThat(result.get().entityType()).isEqualTo(EntityType.POST);
    }

    @Test
    void getPhotoSummary_givenMissingPhoto_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();
        when(photoRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.getPhotoSummary(id)).isEmpty();
    }

    @Test
    void getPhotoSummaryByEntityId_givenAttachedPhoto_shouldReturnSnapshot() {
        UUID entityId = UUID.randomUUID();
        Photo entityPhoto = photo(UUID.randomUUID(), entityId);
        when(photoRepository.findByEntityId(entityId)).thenReturn(Optional.of(entityPhoto));

        Optional<PhotoSummaryDTO> result = adapter.getPhotoSummaryByEntityId(entityId);

        assertThat(result).isPresent();
        assertThat(result.get().entityId()).isEqualTo(entityId);
    }

    @Test
    void getPhotosSummary_givenIds_shouldReturnKeyedMap() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(photoRepository.findAllById(Set.of(id1, id2)))
                .thenReturn(List.of(photo(id1, UUID.randomUUID()), photo(id2, UUID.randomUUID())));

        Map<UUID, PhotoSummaryDTO> result = adapter.getPhotosSummary(Set.of(id1, id2));

        assertThat(result).hasSize(2);
        assertThat(result.get(id1).name()).isEqualTo("pic.jpg");
    }

    @Test
    void getPhotosSummary_givenEmptySet_shouldReturnEmptyMapWithoutQuery() {
        assertThat(adapter.getPhotosSummary(Set.of())).isEmpty();
    }
}

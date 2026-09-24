package dev.thural.quietspace.photo;

import dev.thural.quietspace.photo.Photo;
import dev.thural.quietspace.photo.PhotoRepository;
import dev.thural.quietspace.reaction.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PhotoRepositoryTest {

    @Autowired
    private PhotoRepository photoRepository;

    private Photo savedPhoto;
    private final UUID entityId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Photo photo = Photo.builder()
                .name("test-photo.jpg")
                .type("image/jpeg")
                .data(new byte[]{1, 2, 3})
                .userId(userId)
                .entityId(entityId)
                .entityType(EntityType.POST)
                .build();
        savedPhoto = photoRepository.save(photo);
    }

    @AfterEach
    void tearDown() {
        photoRepository.delete(savedPhoto);
    }

    @Test
    void findByName_shouldReturnPhoto() {
        Optional<Photo> photo = photoRepository.findByName("test-photo.jpg");
        assertThat(photo).isPresent();
        assertThat(photo.get().getName()).isEqualTo("test-photo.jpg");
    }

    @Test
    void findByName_givenNonExistentName_shouldReturnEmpty() {
        Optional<Photo> photo = photoRepository.findByName("non-existent.jpg");
        assertThat(photo).isEmpty();
    }

    @Test
    void findByEntityId_shouldReturnPhoto() {
        Optional<Photo> photo = photoRepository.findByEntityId(entityId);
        assertThat(photo).isPresent();
        assertThat(photo.get().getEntityId()).isEqualTo(entityId);
    }

    @Test
    void deleteByEntityId_shouldRemovePhoto() {
        photoRepository.deleteByEntityId(entityId);
        Optional<Photo> photo = photoRepository.findByEntityId(entityId);
        assertThat(photo).isEmpty();
    }
}

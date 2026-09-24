package dev.thural.quietspace.photo;

import dev.thural.quietspace.photo.Photo;
import dev.thural.quietspace.photo.PhotoRepository;
import dev.thural.quietspace.photo.PhotoServiceImpl;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.shared.exception.ImageUploadException;
import dev.thural.quietspace.shared.exception.UnsupportedImageTypeException;
import dev.thural.quietspace.shared.service.CommonService;
import dev.thural.quietspace.shared.util.ImageCompressionUtil;
import dev.thural.quietspace.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceImplTest {

    @Mock
    private CommonService commonService;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @InjectMocks
    private PhotoServiceImpl photoService;

    private User signedUser;
    private UUID userId;
    private Photo photo;
    private byte[] compressedData;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        signedUser = User.builder()
                .id(userId)
                .username("testuser")
                .build();

        compressedData = "compressed-image-data".getBytes();

        photo = Photo.builder()
                .id(UUID.randomUUID())
                .name("photo-name.jpg")
                .type("image/jpeg")
                .data(compressedData)
                .userId(userId)
                .entityId(userId)
                .entityType(EntityType.USER)
                .build();
    }

    @Test
    void uploadProfilePhoto_givenValidJpeg_shouldReturnPhotoName() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "original-image-data".getBytes()
        );
        when(commonService.getSignedUser()).thenReturn(signedUser);
        when(imageCompressionUtil.compressImage(any(InputStream.class), anyInt())).thenReturn(compressedData);
        when(photoRepository.save(any(Photo.class))).thenReturn(photo);

        String photoName = photoService.uploadProfilePhoto(file);

        assertThat(photoName).isEqualTo("photo-name.jpg");

        ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository).save(photoCaptor.capture());
        Photo savedPhoto = photoCaptor.getValue();
        assertThat(savedPhoto.getEntityId()).isEqualTo(userId);
        assertThat(savedPhoto.getEntityType()).isEqualTo(EntityType.USER);
    }

    @Test
    void uploadProfilePhoto_givenFileExceeding2MB_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", new byte[3 * 1024 * 1024]
        );
        when(commonService.getSignedUser()).thenReturn(signedUser);

        assertThatThrownBy(() -> photoService.uploadProfilePhoto(file))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("2MB");
    }

    @Test
    void uploadProfilePhoto_givenUnsupportedContentType_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "file.txt", "text/plain", "data".getBytes()
        );
        when(commonService.getSignedUser()).thenReturn(signedUser);

        assertThatThrownBy(() -> photoService.uploadProfilePhoto(file))
                .isInstanceOf(UnsupportedImageTypeException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void uploadProfilePhoto_givenNullContentType_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "file.dat", null, "data".getBytes()
        );
        when(commonService.getSignedUser()).thenReturn(signedUser);

        assertThatThrownBy(() -> photoService.uploadProfilePhoto(file))
                .isInstanceOf(UnsupportedImageTypeException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void uploadPhoto_shouldPersistWithNullEntityType() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "original-image-data".getBytes()
        );
        when(commonService.getSignedUser()).thenReturn(signedUser);
        when(imageCompressionUtil.compressImage(any(InputStream.class), anyInt())).thenReturn(compressedData);
        when(photoRepository.save(any(Photo.class))).thenReturn(photo);

        PhotoResponse result = photoService.uploadPhoto(file);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("photo-name.jpg");

        ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository).save(photoCaptor.capture());
        Photo savedPhoto = photoCaptor.getValue();
        assertThat(savedPhoto.getEntityType()).isNull();
    }

    @Test
    void persistPhotoEntity_whenCompressionFails_shouldThrow() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "data".getBytes()
        );
        when(commonService.getSignedUser()).thenReturn(signedUser);
        when(imageCompressionUtil.compressImage(any(InputStream.class), anyInt())).thenThrow(new IOException("compression failed"));

        assertThatThrownBy(() -> photoService.persistPhotoEntity(file, userId, EntityType.USER))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("Failed to upload");
    }

    @Test
    void getPhotoByName_givenExistingName_shouldReturnResponse() throws IOException {
        when(photoRepository.findByName("photo-name.jpg")).thenReturn(Optional.of(photo));
        when(imageCompressionUtil.decompressImage(compressedData)).thenReturn(compressedData);

        PhotoResponse response = photoService.getPhotoByName("photo-name.jpg");

        assertThat(response.getName()).isEqualTo("photo-name.jpg");
        assertThat(response.getType()).isEqualTo("image/jpeg");
        assertThat(response.getData()).isEqualTo(compressedData);
    }

    @Test
    void getPhotoByName_givenUnknownName_shouldThrow() {
        when(photoRepository.findByName("unknown.jpg")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.getPhotoByName("unknown.jpg"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Photo not found with name");
    }

    @Test
    void getPhotoByEntityId_givenExistingEntity_shouldReturnResponse() throws IOException {
        when(photoRepository.findByEntityId(userId)).thenReturn(Optional.of(photo));
        when(imageCompressionUtil.decompressImage(compressedData)).thenReturn(compressedData);

        PhotoResponse response = photoService.getPhotoByEntityId(userId);

        assertThat(response.getName()).isEqualTo("photo-name.jpg");
        assertThat(response.getData()).isEqualTo(compressedData);
    }

    @Test
    void getPhotoByEntityId_givenUnknownEntity_shouldThrow() {
        when(photoRepository.findByEntityId(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.getPhotoByEntityId(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getPhotoById_givenExistingPhoto_shouldReturnResponse() throws IOException {
        when(photoRepository.findById(photo.getId())).thenReturn(Optional.of(photo));
        when(imageCompressionUtil.decompressImage(compressedData)).thenReturn(compressedData);

        PhotoResponse response = photoService.getPhotoById(photo.getId());

        assertThat(response.getName()).isEqualTo("photo-name.jpg");
        assertThat(response.getType()).isEqualTo("image/jpeg");
    }

    @Test
    void getPhotoById_givenUnknownPhoto_shouldThrow() {
        when(photoRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.getPhotoById(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getPhotoData_givenExistingName_shouldReturnDecompressedBytes() throws IOException {
        when(photoRepository.findByName("photo-name.jpg")).thenReturn(Optional.of(photo));
        when(imageCompressionUtil.decompressImage(compressedData)).thenReturn(compressedData);

        byte[] result = photoService.getPhotoData("photo-name.jpg");

        assertThat(result).isEqualTo(compressedData);
    }

    @Test
    void getPhotoData_givenUnknownName_shouldThrow() {
        when(photoRepository.findByName("unknown.jpg")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.getPhotoData("unknown.jpg"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deletePhotoByEntityId_shouldCallRepositoryDelete() {
        photoService.deletePhotoByEntityId(userId);

        verify(photoRepository).deleteByEntityId(userId);
    }

    @Test
    void deletePhotoById_shouldCallRepositoryDelete() {
        photoService.deletePhotoById(photo.getId());

        verify(photoRepository).deleteById(photo.getId());
    }
}

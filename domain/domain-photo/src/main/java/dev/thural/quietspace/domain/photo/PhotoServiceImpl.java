package dev.thural.quietspace.photo;

import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.reaction.EntityType;
import dev.thural.quietspace.shared.exception.ImageUploadException;
import dev.thural.quietspace.shared.exception.UnsupportedImageTypeException;
import dev.thural.quietspace.shared.service.CommonService;
import dev.thural.quietspace.shared.util.ImageCompressionUtil;
import dev.thural.quietspace.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoServiceImpl implements PhotoService {

    private final CommonService commonService;
    private final PhotoRepository photoRepository;
    private final ImageCompressionUtil imageCompressionUtil;

    private static final List<String> SUPPORTED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "image/bmp", ".bmp"
    );

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int TARGET_IMAGE_SIZE = 100 * 1024; // 100KB

    @Override
    @Transactional
    public String uploadProfilePhoto(MultipartFile file) {
        User signedUser = commonService.getSignedUser();
        Photo savedPhoto = persistPhotoEntity(file, signedUser.getId(), EntityType.USER);
        signedUser.setPhotoId(savedPhoto.getId());
        return savedPhoto.getName();
    }

    @Override
    @Transactional
    public PhotoResponse uploadPhoto(MultipartFile file) {
        User signedUser = commonService.getSignedUser();
        Photo savedPhoto = persistPhotoEntity(file, signedUser.getId(), null);
        return PhotoResponse.builder()
                .id(savedPhoto.getId())
                .name(savedPhoto.getName())
                .type(savedPhoto.getType())
                .build();
    }

    @Override
    @Transactional
    public Photo persistPhotoEntity(MultipartFile file, UUID entityId, EntityType entityType) {

        String contentType = file.getContentType();
        validatePhotoDataElseThrow(file, contentType);

        try {
            UUID signedUserId = commonService.getSignedUser().getId();
            String uniqueFileName = generateUniqueFileName(contentType);

            byte[] compressedImage = imageCompressionUtil.compressImage(
                    file.getInputStream(),
                    TARGET_IMAGE_SIZE
            );

            return photoRepository.save(Photo.builder()
                    .name(uniqueFileName)
                    .type(contentType)
                    .data(compressedImage)
                    .userId(signedUserId)
                    .entityId(entityId)
                    .entityType(entityType)
                    .build());
        } catch (IOException e) {
            log.error("Error uploading photo", e);
            throw new ImageUploadException("Failed to upload photo", e);
        }
    }

    private void validatePhotoDataElseThrow(MultipartFile file, String contentType) {
        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("File size exceeds maximum limit of 2MB");
            throw new ImageUploadException("File size should not exceed 2MB");
        }

        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            log.error("Unsupported image type: {}", contentType);
            throw new UnsupportedImageTypeException(
                    "Unsupported image type. Supported types: " +
                            String.join(", ", SUPPORTED_CONTENT_TYPES)
            );
        }
    }

    @Override
    public PhotoResponse getPhotoByName(String name) {
        Photo foundPhoto = findPhotoByName(name);
        return getDecompressedPhoto(foundPhoto);
    }

    @Override
    public PhotoResponse getPhotoByEntityId(UUID entityId) {
        Photo foundPhoto = photoRepository.findByEntityId(entityId)
                .orElseThrow(EntityNotFoundException::new);
        return getDecompressedPhoto(foundPhoto);
    }

    @Override
    public PhotoResponse getPhotoById(UUID photoId) {
        Photo foundPhoto = photoRepository.findById(photoId)
                .orElseThrow(EntityNotFoundException::new);
        return getDecompressedPhoto(foundPhoto);
    }

    private PhotoResponse getDecompressedPhoto(Photo photo) {
        return PhotoResponse.builder()
                .name(photo.getName())
                .type(photo.getType())
                .data(imageCompressionUtil.decompressImage(photo.getData()))
                .build();
    }

    @Override
    public byte[] getPhotoData(String name) {
        Photo foundPhoto = findPhotoByName(name);
        return imageCompressionUtil.decompressImage(foundPhoto.getData());
    }

    public Photo findPhotoByName(String name) {
        return photoRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Photo not found with name: " + name
                ));
    }

    private String generateUniqueFileName(String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType, ".jpg");
        return UUID.randomUUID() + extension;
    }

    @Override
    public void deletePhotoByEntityId(UUID entityId) {
        photoRepository.deleteByEntityId(entityId);
    }

    @Override
    public void deletePhotoById(UUID photoId) {
        photoRepository.deleteById(photoId);
    }
}

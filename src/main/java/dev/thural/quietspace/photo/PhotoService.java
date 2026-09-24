package dev.thural.quietspace.photo;

import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.reaction.EntityType;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PhotoService {

    String uploadProfilePhoto(MultipartFile file);

    PhotoResponse uploadPhoto(MultipartFile file);

    Photo persistPhotoEntity(MultipartFile file, UUID entityId, EntityType entityType);

    PhotoResponse getPhotoByName(String name);

    PhotoResponse getPhotoByEntityId(UUID entityId);

    PhotoResponse getPhotoById(UUID photoId);

    byte[] getPhotoData(String name);

    void deletePhotoByEntityId(UUID entityId);

    void deletePhotoById(UUID photoId);

}

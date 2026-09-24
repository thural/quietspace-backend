package dev.thural.quietspace.photo.controller;

import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/photos")
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping
    public ResponseEntity<PhotoResponse> uploadPhoto(@RequestParam("image") MultipartFile file) {
        PhotoResponse response = photoService.uploadPhoto(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/profile")
    public ResponseEntity<Map<String, String>> uploadProfilePhoto(@RequestParam("image") MultipartFile file) {
        String photoName = photoService.uploadProfilePhoto(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("photoName", photoName));
    }

    private static final List<MediaType> ALLOWED_IMAGE_TYPES = List.of(
            MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF,
            MediaType.parseMediaType("image/webp")
    );

    @GetMapping("/{name}")
    public ResponseEntity<byte[]> getPhotoByName(@PathVariable String name) {
        PhotoResponse photoResponse = photoService.getPhotoByName(name);
        MediaType mediaType = MediaType.parseMediaType(photoResponse.getType());
        if (!ALLOWED_IMAGE_TYPES.contains(mediaType)) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(photoResponse.getData());
    }

    @DeleteMapping("profile/{userId}")
    public ResponseEntity<Void> removePhotoByUserId(@PathVariable UUID userId) {
        photoService.deletePhotoByEntityId(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<PhotoResponse> getPhotoByPostId(@PathVariable UUID postId) {
        PhotoResponse response = photoService.getPhotoByEntityId(postId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhotoById(@PathVariable UUID photoId) {
        photoService.deletePhotoById(photoId);
        return ResponseEntity.noContent().build();
    }

}

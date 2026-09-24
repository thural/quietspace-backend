package dev.thural.quietspace.photo.controller;

import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.photo.dto.PhotoResponse;
import dev.thural.quietspace.shared.exception.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PhotoControllerTest {

    MockMvc mockMvc;

    @Mock
    private PhotoService photoService;

    @InjectMocks
    private PhotoController photoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(photoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadProfilePhoto_shouldReturn201WithName() throws Exception {
        when(photoService.uploadProfilePhoto(any())).thenReturn("photo-name.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/photos/profile")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoName").value("photo-name.jpg"));
    }

    @Test
    void getPhotoByName_shouldReturn200WithImage() throws Exception {
        PhotoResponse photoResponse = PhotoResponse.builder()
                .name("photo.jpg")
                .type("image/jpeg")
                .data("image-bytes".getBytes())
                .build();
        when(photoService.getPhotoByName("photo.jpg")).thenReturn(photoResponse);

        mockMvc.perform(get("/api/v1/photos/photo.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes("image-bytes".getBytes()));
    }

    @Test
    void getPhotoByName_whenNotFound_shouldReturn404() throws Exception {
        when(photoService.getPhotoByName("unknown.jpg"))
                .thenThrow(new EntityNotFoundException("Photo not found with name: unknown.jpg"));

        mockMvc.perform(get("/api/v1/photos/unknown.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removePhotoByUserId_shouldReturn204() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/photos/profile/{userId}", userId))
                .andExpect(status().isNoContent());
    }
}

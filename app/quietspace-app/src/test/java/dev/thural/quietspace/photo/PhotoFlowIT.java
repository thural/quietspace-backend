package dev.thural.quietspace.photo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class PhotoFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private IntegrationTestHelper helper;
    private String jwtToken;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        userRepository.deleteAll();
        entityManager.flush();
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        jwtToken = helper.registerAndLogin("photouser@test.com", "password123");
        userId = userRepository.findUserEntityByEmail("photouser@test.com").orElseThrow().getId();
    }

    @Test
    void uploadProfilePhoto_shouldReturn201() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", imageBytes
        );

        mockMvc.perform(multipart("/api/v1/photos/profile")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated());
    }

    @Test
    void uploadProfilePhoto_givenUnsupportedType_shouldReturn400() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "file.txt", MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/photos/profile")
                        .file(image)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPhotoByName_givenNonExistentName_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/photos/{name}", "nonexistent.jpg")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPhoto_shouldReturn201() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", imageBytes
        );

        mockMvc.perform(multipart("/api/v1/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").isNotEmpty());
    }

    @Test
    void deletePhotoById_shouldReturn204() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", imageBytes
        );

        String responseBody = mockMvc.perform(multipart("/api/v1/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String photoId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/v1/photos/{photoId}", photoId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPhotoByPostId_givenNonExistent_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/photos/post/{postId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProfilePhoto_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/photos/profile/{userId}", userId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}

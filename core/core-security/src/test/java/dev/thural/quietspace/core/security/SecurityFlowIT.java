package dev.thural.quietspace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.auth.dto.AuthRequest;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.shared.service.EmailService;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.user.ProfileSettings;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class SecurityFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        IntegrationTestHelper.cleanDatabase(entityManager);
    }

    @Test
    void accessPublicEndpoint_withoutToken_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hello")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void accessActuatorHealth_withoutToken_shouldSucceed() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_shouldReturnJsonError() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/posts"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void accessAdminEndpoint_withUserRole_shouldReturn403() throws Exception {
        User user = User.builder()
                .username("regularuser")
                .email("user@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstname("Regular")
                .lastname("User")
                .role(Role.USER)
                .enabled(true)
                .accountLocked(false)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        user.setProfileSettings(ProfileSettings.builder().user(user).build());
        userRepository.save(user);

        AuthRequest authRequest = AuthRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        String body = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void accessSwaggerUi_withoutToken_shouldSucceed() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    @Test
    void authenticate_withInvalidCredentials_shouldReturn401() throws Exception {
        AuthRequest invalid = AuthRequest.builder()
                .email("unknown@test.com")
                .password("wrong")
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_withoutBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withoutBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

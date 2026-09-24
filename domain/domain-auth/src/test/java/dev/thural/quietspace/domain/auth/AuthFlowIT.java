package dev.thural.quietspace.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.auth.dto.AuthRequest;
import dev.thural.quietspace.auth.dto.AuthResponse;
import dev.thural.quietspace.auth.dto.RegistrationRequest;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.security.Token;
import dev.thural.quietspace.security.TokenRepository;
import dev.thural.quietspace.shared.service.EmailService;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class AuthFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private PhotoService photoService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        IntegrationTestHelper.cleanDatabase(entityManager);
        validRequest = RegistrationRequest.builder()
                .username("testuser")
                .firstname("Test")
                .lastname("User")
                .email("test@example.com")
                .password("password123")
                .build();
    }

    @Test
    void register_givenValidRequest_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void register_givenExistingEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_givenInvalidBody_shouldReturn400() throws Exception {
        RegistrationRequest invalid = RegistrationRequest.builder()
                .username("")
                .firstname("")
                .lastname("")
                .email("invalid")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateAccount_givenValidToken_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        Token token = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("test@example.com"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", token.getToken()))
                .andExpect(status().isOk());

        User user = userRepository.findUserEntityByEmail("test@example.com").orElseThrow();
        assert user.isEnabled();
    }

    @Test
    void activateAccount_givenInvalidToken_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", "000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticate_givenValidCredentials_shouldReturn200WithJwt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        Token token = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("test@example.com"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", token.getToken()))
                .andExpect(status().isOk());

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.message").value("authentication was successful"));
    }

    @Test
    void authenticate_givenWrongPassword_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        Token token = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("test@example.com"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", token.getToken()))
                .andExpect(status().isOk());

        AuthRequest wrongPassword = AuthRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_givenUnknownEmail_shouldReturn401() throws Exception {
        AuthRequest unknown = AuthRequest.builder()
                .email("unknown@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknown)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_givenValidToken_shouldReturnNewAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        Token activationToken = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("test@example.com"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", activationToken.getToken()))
                .andExpect(status().isOk());

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String refreshToken = authResponse.getRefreshToken();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.message").value("token was refreshed"));
    }

    @Test
    void refreshToken_givenExpiredToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("Authorization", "Bearer expired.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signout_givenValidBearer_shouldBlacklistTokenAndReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        Token activationToken = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("test@example.com"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", activationToken.getToken()))
                .andExpect(status().isOk());

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String accessToken = authResponse.getAccessToken();

        mockMvc.perform(post("/api/v1/auth/signout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withBlacklistedToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        Token activationToken = tokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals("test@example.com"))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/auth/activate-account")
                        .param("token", activationToken.getToken()))
                .andExpect(status().isOk());

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String accessToken = authResponse.getAccessToken();

        mockMvc.perform(post("/api/v1/auth/signout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

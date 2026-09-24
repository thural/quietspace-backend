package dev.thural.quietspace.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.shared.util.IntegrationTestHelper;
import dev.thural.quietspace.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class AdminFlowIT {

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

    @MockitoBean
    private PhotoService photoService;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    private IntegrationTestHelper helper;
    private String adminJwt;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationTestHelper.cleanDatabase(entityManager);
        userRepository.deleteAll();
        entityManager.flush();
        helper = new IntegrationTestHelper(mockMvc, objectMapper, userRepository, passwordEncoder);
        adminJwt = helper.registerAndLoginAdmin("admin@test.com", "password123");
    }

    @Test
    void sayHello_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/admin")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        String userJwt = helper.registerAndLogin("todelete@test.com", "password123");
        var userId = userRepository.findUserEntityByEmail("todelete@test.com").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/{userId}", userId)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void listUsers_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminJwt)
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void sayHello_withoutAdminRole_shouldReturn403() throws Exception {
        String userJwt = helper.registerAndLogin("user@test.com", "password123");

        mockMvc.perform(get("/api/v1/admin")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isForbidden());
    }
}

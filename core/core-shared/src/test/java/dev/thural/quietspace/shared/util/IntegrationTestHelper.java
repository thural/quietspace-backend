package dev.thural.quietspace.shared.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.thural.quietspace.auth.dto.AuthRequest;
import dev.thural.quietspace.auth.dto.AuthResponse;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.ProfileSettings;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IntegrationTestHelper {

    public static void cleanDatabase(EntityManager entityManager) {
        entityManager.createNativeQuery("DELETE FROM message").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM reaction").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM notification").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM comment").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM poll_option_votes").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM poll_option").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM poll").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM post").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM token").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_followings").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_chat").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_saved_posts").executeUpdate();
    }

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public IntegrationTestHelper(MockMvc mockMvc, ObjectMapper objectMapper,
                                  UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String registerAndLogin(String email, String password) throws Exception {
        User user = User.builder()
                .username(email.split("@")[0])
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstname("Test")
                .lastname("User")
                .role(Role.USER)
                .enabled(true)
                .accountLocked(false)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        ProfileSettings settings = ProfileSettings.builder()
                .user(user)
                .build();
        user.setProfileSettings(settings);
        userRepository.save(user);

        AuthRequest authRequest = AuthRequest.builder()
                .email(email)
                .password(password)
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        return authResponse.getAccessToken();
    }

    public String registerAndLoginAdmin(String email, String password) throws Exception {
        User user = User.builder()
                .username(email.split("@")[0])
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstname("Admin")
                .lastname("User")
                .role(Role.ADMIN)
                .enabled(true)
                .accountLocked(false)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        ProfileSettings settings = ProfileSettings.builder()
                .user(user)
                .build();
        user.setProfileSettings(settings);
        userRepository.save(user);

        AuthRequest authRequest = AuthRequest.builder()
                .email(email)
                .password(password)
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        return authResponse.getAccessToken();
    }
}

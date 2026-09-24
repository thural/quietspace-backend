package dev.thural.quietspace.user;

import dev.thural.quietspace.config.TestcontainersConfig;
import dev.thural.quietspace.photo.PhotoService;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.ProfileSettings;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import dev.thural.quietspace.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
@Transactional
class UserServiceIT {

    @MockitoBean
    private PhotoService photoService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = User.builder()
                .username("testuser1")
                .email("testuser1@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstname("Test")
                .lastname("User1")
                .role(Role.USER)
                .enabled(true)
                .accountLocked(false)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        ProfileSettings settings1 = ProfileSettings.builder().user(user1).build();
        user1.setProfileSettings(settings1);

        user2 = User.builder()
                .username("testuser2")
                .email("testuser2@test.com")
                .password(passwordEncoder.encode("password456"))
                .firstname("Test")
                .lastname("User2")
                .role(Role.USER)
                .enabled(true)
                .accountLocked(false)
                .createDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        ProfileSettings settings2 = ProfileSettings.builder().user(user2).build();
        user2.setProfileSettings(settings2);

        userRepository.saveAll(List.of(user1, user2));
    }

    @Test
    void listUsers_shouldReturnAllUsers() {
        var users = userService.listUsers(0, 10);
        assertThat(users.getContent()).hasSize(2);
    }

    @Test
    void getUsersFromIdList_shouldReturnMatchingUsers() {
        List<User> users = userService.getUsersFromIdList(List.of(user1.getId(), user2.getId()));
        assertThat(users).hasSize(2);
    }

    @Test
    void getUserById_shouldReturnUser() {
        var user = userService.getUserById(user1.getId());
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("testuser1@test.com");
    }
}

package dev.thural.quietspace.user;

import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    private final User user = User.builder()
            .email("user@email.com")
            .username("user")
            .firstname("firstname")
            .lastname("lastname")
            .password("78921731")
            .role(Role.USER)
            .accountLocked(false)
            .username("test user")
            .createDate(OffsetDateTime.now())
            .updateDate(OffsetDateTime.now())
            .build();

    private User savedUser;

    @BeforeEach
    void setUp() {
        this.savedUser = userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(user);
    }

    @Test
    void testGetUserListByName() {
        Page<User> list = userRepository.findAllByUsernameIsLikeIgnoreCase("%user%", null);
        assertThat(list.toList().size()).isEqualTo(1);
        assertThat(list.toList().get(0).getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void getUserById() {
        User foundUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void testFindUserByUsername() {
        User userEntity = userRepository.findUserByUsername(user.getUsername()).orElse(null);
        assertThat(userEntity).isNotNull();
        assertThat(userEntity.getId()).isEqualTo(savedUser.getId());
        assertThat(userEntity.getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    void testFindUserEntityByEmail() {
        User userEntity = userRepository.findUserEntityByEmail(user.getEmail()).orElse(null);
        assertThat(userEntity).isNotNull();
        assertThat(userEntity.getId()).isEqualTo(savedUser.getId());
        assertThat(userEntity.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void testSavedUserNameIsTooLong() {
        assertThrows(ConstraintViolationException.class, () -> {
            userRepository.save(User.builder()
                    .username("test user random text longer than 32 characters")
                    .build());
            userRepository.flush();
        });
    }
}
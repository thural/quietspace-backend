package dev.thural.quietspace.security;

import dev.thural.quietspace.security.Token;
import dev.thural.quietspace.security.TokenRepository;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TokenRepositoryTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;

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

    private Token token = Token.builder()
            .token("32r33fg32fg22d33f4g42fg3")
            .user(user)
            .createDate(OffsetDateTime.now())
            .email("user@gmail.com")
            .build();

    private User savedUser;
    private Token savedToken;

    @BeforeEach
    void setUp() {
        this.savedUser = userRepository.save(user);
        this.savedToken = tokenRepository.save(token);
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(user);
        tokenRepository.delete(token);
    }

    @Test
    void existsByToken() {
        boolean isExists = tokenRepository.existsByToken(token.getToken());
        assertTrue(isExists);
    }

    @Test
    void existsByEmail() {
        boolean isExists = tokenRepository.existsByEmail(token.getEmail());
        assertTrue(isExists);
    }

    @Test
    void deleteByEmail() {
        tokenRepository.deleteByEmail(user.getEmail());
        boolean isExists = tokenRepository.existsByEmail(user.getEmail());
        assertFalse(isExists);
    }

    @Test
    void getByEmail() {
        Optional<Token> foundToken = tokenRepository.getByEmail(token.getEmail());
        assertTrue(foundToken.isPresent());
        assertEquals(token.getToken(), foundToken.get().getToken());
    }

    @Test
    void findByToken() {
        Optional<Token> foundToken = tokenRepository.findByToken(token.getToken());
        assertTrue(foundToken.isPresent());
        assertEquals(token.getToken(), foundToken.get().getToken());
    }

    @Test
    void existsByJti_whenJtiExists_shouldReturnTrue() {
        Token jtiToken = Token.builder()
                .token("jti-token-value")
                .jti("unique-jti-123")
                .user(savedUser)
                .createDate(OffsetDateTime.now())
                .email("jti-test@email.com")
                .build();
        tokenRepository.save(jtiToken);

        boolean exists = tokenRepository.existsByJti("unique-jti-123");

        assertTrue(exists);
    }

    @Test
    void findByJti_whenJtiExists_shouldReturnToken() {
        Token jtiToken = Token.builder()
                .token("find-by-jti-token")
                .jti("findable-jti")
                .user(savedUser)
                .createDate(OffsetDateTime.now())
                .email("find-jti@email.com")
                .build();
        tokenRepository.save(jtiToken);

        Optional<Token> found = tokenRepository.findByJti("findable-jti");

        assertTrue(found.isPresent());
        assertEquals("find-by-jti-token", found.get().getToken());
    }

    @Test
    void deleteByExpireDateBefore_shouldRemoveExpiredTokens() {
        Token expiredToken = Token.builder()
                .token("expired-token-value")
                .user(savedUser)
                .createDate(OffsetDateTime.now())
                .expireDate(OffsetDateTime.now().minusDays(1))
                .email("expired@email.com")
                .build();
        tokenRepository.save(expiredToken);

        int deleted = tokenRepository.deleteByExpireDateBefore(OffsetDateTime.now());

        assertEquals(1, deleted);
        assertFalse(tokenRepository.existsByToken("expired-token-value"));
    }
}
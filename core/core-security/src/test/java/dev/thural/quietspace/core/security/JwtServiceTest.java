package dev.thural.quietspace.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails userDetails;

    private static final String SECRET_KEY = "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciB0ZXN0aW5nIHB1cnBvc2VzIG9ubHkgMTIzNDU2Nzg5MA==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "quietspace-backend");
        ReflectionTestUtils.setField(jwtService, "audience", "quietspace-backend");

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void generateToken_givenUserDetails_shouldReturnValidJwt() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_givenToken_shouldReturnSubject() {
        String token = jwtService.generateToken(userDetails);

        String extracted = jwtService.extractUsername(token);

        assertThat(extracted).isEqualTo("testuser");
    }

    @Test
    void isTokenValid_givenMatchingUser_shouldReturnTrue() {
        String token = jwtService.generateToken(userDetails);

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_givenWrongUser_shouldReturnFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails wrongUser = User.builder()
                .username("otheruser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        boolean valid = jwtService.isTokenValid(token, wrongUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenExpired_givenJustGeneratedToken_shouldReturnFalse() {
        String token = jwtService.generateToken(userDetails);

        boolean expired = jwtService.isTokenExpired(token);

        assertThat(expired).isFalse();
    }

    @Test
    void generateRefreshToken_shouldReturnToken() {
        String token = jwtService.generateRefreshToken(Collections.emptyMap(), userDetails);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void extractClaim_givenCustomClaim_shouldReturnValue() {
        String token = jwtService.generateToken(userDetails);

        String subject = jwtService.extractClaim(token, claims -> claims.getSubject());

        assertThat(subject).isEqualTo("testuser");
    }
}

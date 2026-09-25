package dev.thural.quietspace.core.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceImplTest {

    private JwtTokenServiceImpl service;
    private String secretKeyString;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        secretKeyString = java.util.Base64.getEncoder().encodeToString(secretKey.getEncoded());
        service = new JwtTokenServiceImpl();
        setField(service, "secretKey", secretKeyString);
        setField(service, "jwtExpiration", 3600000L);
        setField(service, "jwtRefreshExpiration", 86400000L);
        setField(service, "issuer", "test-issuer");
        setField(service, "audience", "test-audience");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserDetails userDetails() {
        return User.withUsername("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generateToken_createsValidToken() {
        String token = service.generateToken(userDetails());

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_withExtraClaims_includesClaims() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("custom", "value");
        String token = service.generateToken(extraClaims, userDetails());

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("custom")).isEqualTo("value");
    }

    @Test
    void generateRefreshToken_createsTokenWithLongerExpiry() {
        String refreshToken = service.generateRefreshToken(Map.of(), userDetails());

        assertThat(refreshToken).isNotNull();
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

        Date exp = claims.getExpiration();
        Date now = new Date();
        long diff = exp.getTime() - now.getTime();
        assertThat(diff).isGreaterThan(86300000L);
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = service.generateToken(userDetails());

        String username = service.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractJti_returnsIdClaim() {
        String token = service.generateToken(userDetails());

        String jti = service.extractJti(token);

        assertThat(jti).isNotNull();
    }

    @Test
    void extractClaim_withResolver_returnsResolvedValue() {
        String token = service.generateToken(userDetails());

        String subject = service.extractClaim(token, Claims::getSubject);

        assertThat(subject).isEqualTo("testuser");
    }

    @Test
    void isTokenValid_givenValidTokenAndMatchingUser_returnsTrue() {
        String token = service.generateToken(userDetails());

        boolean valid = service.isTokenValid(token, userDetails());

        assertThat(valid).isTrue();
    }

    @Test
    void isTokenValid_givenValidTokenButDifferentUser_returnsFalse() {
        String token = service.generateToken(userDetails());
        UserDetails otherUser = User.withUsername("other").password("p").authorities("ROLE_USER").build();

        boolean valid = service.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValid_givenExpiredToken_returnsFalse() {
        setField(service, "jwtExpiration", -1000L);
        String token = service.generateToken(userDetails());

        boolean valid = service.isTokenValid(token, userDetails());

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenExpired_givenExpiredToken_throwsException() {
        setField(service, "jwtExpiration", -1000L);
        String token = service.generateToken(userDetails());

        assertThatThrownBy(() -> service.isTokenExpired(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void isTokenExpired_givenValidToken_returnsFalse() {
        String token = service.generateToken(userDetails());

        boolean expired = service.isTokenExpired(token);

        assertThat(expired).isFalse();
    }

    @Test
    void extractExpiration_returnsExpirationDate() {
        String token = service.generateToken(userDetails());

        Date exp = service.extractExpiration(token);

        assertThat(exp).isNotNull();
        assertThat(exp).isAfter(new Date());
    }
}
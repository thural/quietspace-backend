package dev.thural.quietspace.domain.user.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

    @Transactional
    public void saveRefreshToken(UUID userId, String email, String jti, String token, long expirationMillis) {
        OffsetDateTime expireDate = OffsetDateTime.now().plus(expirationMillis, ChronoUnit.MILLIS);
        Token tokenEntity = Token.builder()
                .token(token)
                .jti(jti)
                .email(email)
                .userId(userId)
                .expireDate(expireDate)
                .used(false)
                .build();
        tokenRepository.save(tokenEntity);
    }

    @Transactional
    public void blacklistToken(String token) {
        tokenRepository.findByToken(token).ifPresent(t -> {
            t.setUsed(true);
            tokenRepository.save(t);
        });
    }

    @Transactional
    public void revokeTokenByJti(String jti) {
        tokenRepository.findByJti(jti).ifPresent(t -> {
            t.setUsed(true);
            tokenRepository.save(t);
        });
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenRepository.existsByToken(token);
    }

    public boolean isTokenRevokedByJti(String jti) {
        return tokenRepository.findByJti(jti)
                .map(Token::isUsed)
                .orElse(false);
    }

    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteByExpireDateBefore(OffsetDateTime.now());
    }
}
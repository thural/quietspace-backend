package dev.thural.quietspace.domain.user.token;

import dev.thural.quietspace.core.security.port.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenBlacklistAdapter implements TokenBlacklistPort {

    private final TokenRepository tokenRepository;

    @Override
    public boolean isBlacklisted(String token) {
        return tokenRepository.existsByToken(token);
    }

    @Override
    public boolean isRevokedByJti(String jti) {
        return tokenRepository.existsByJti(jti);
    }
}
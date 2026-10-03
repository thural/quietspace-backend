package dev.thural.quietspace.domain.user.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void saveRefreshToken_shouldPersistWithExpiryAndUnusedFlag() {
        UUID userId = UUID.randomUUID();

        tokenService.saveRefreshToken(userId, "u@x.com", "jti-1", "refresh-1", 60000L);

        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(captor.capture());
        Token saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getJti()).isEqualTo("jti-1");
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpireDate()).isNotNull();
    }

    @Test
    void blacklistToken_givenExistingToken_shouldMarkUsed() {
        Token token = Token.builder().token("t").used(false).build();
        when(tokenRepository.findByToken("t")).thenReturn(Optional.of(token));

        tokenService.blacklistToken("t");

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    void isTokenBlacklisted_shouldDelegateToRepository() {
        when(tokenRepository.existsByToken("t")).thenReturn(true);

        assertThat(tokenService.isTokenBlacklisted("t")).isTrue();
    }

    @Test
    void isTokenRevokedByJti_givenUsedToken_shouldReturnTrue() {
        Token token = Token.builder().jti("j").used(true).build();
        when(tokenRepository.findByJti("j")).thenReturn(Optional.of(token));

        assertThat(tokenService.isTokenRevokedByJti("j")).isTrue();
    }

    @Test
    void isTokenRevokedByJti_givenMissingToken_shouldReturnFalse() {
        when(tokenRepository.findByJti("missing")).thenReturn(Optional.empty());

        assertThat(tokenService.isTokenRevokedByJti("missing")).isFalse();
    }

    @Test
    void cleanExpiredTokens_shouldDelegateToRepository() {
        tokenService.cleanExpiredTokens();

        verify(tokenRepository).deleteByExpireDateBefore(any());
    }
}

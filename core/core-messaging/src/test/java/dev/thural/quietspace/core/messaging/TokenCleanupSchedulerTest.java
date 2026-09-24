package dev.thural.quietspace.config;

import dev.thural.quietspace.security.TokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenCleanupScheduler scheduler;

    @Test
    void cleanExpiredTokens_shouldDeleteTokensBeforeCutoff() {
        when(tokenRepository.deleteByExpireDateBefore(any(OffsetDateTime.class))).thenReturn(5);

        scheduler.cleanExpiredTokens();

        verify(tokenRepository).deleteByExpireDateBefore(any(OffsetDateTime.class));
    }

    @Test
    void cleanExpiredTokens_whenNoTokensToDelete_shouldStillCallRepository() {
        when(tokenRepository.deleteByExpireDateBefore(any(OffsetDateTime.class))).thenReturn(0);

        scheduler.cleanExpiredTokens();

        verify(tokenRepository).deleteByExpireDateBefore(any(OffsetDateTime.class));
    }
}

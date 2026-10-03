package dev.thural.quietspace.domain.user.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenCleanupScheduler scheduler;

    @Test
    void cleanExpiredTokens_shouldDeleteTokensOlderThanOneDay() {
        when(tokenRepository.deleteByExpireDateBefore(any())).thenReturn(5);

        scheduler.cleanExpiredTokens();

        verify(tokenRepository).deleteByExpireDateBefore(any());
    }

    @Test
    void cleanExpiredTokens_whenNothingExpired_shouldStillRun() {
        when(tokenRepository.deleteByExpireDateBefore(any())).thenReturn(0);

        scheduler.cleanExpiredTokens();

        verify(tokenRepository).deleteByExpireDateBefore(any());
    }
}

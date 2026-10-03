package dev.thural.quietspace.domain.user.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistAdapterTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenBlacklistAdapter adapter;

    @Test
    void isBlacklisted_shouldDelegateToRepository() {
        when(tokenRepository.existsByToken("t")).thenReturn(true);

        assertThat(adapter.isBlacklisted("t")).isTrue();
    }

    @Test
    void isRevokedByJti_shouldDelegateToRepository() {
        when(tokenRepository.existsByJti("jti")).thenReturn(false);

        assertThat(adapter.isRevokedByJti("jti")).isFalse();
    }
}

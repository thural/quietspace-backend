package dev.thural.quietspace.domain.user.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Periodic hygiene for revoked/expired tokens.
 *
 * <p>Migrated from the monolith's {@code TokenCleaner}; uses a bulk delete
 * instead of load-then-delete-all.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenRepository tokenRepository;

    @Scheduled(fixedRate = 86400000L)
    public void cleanExpiredTokens() {
        log.info("running token cleanup...");
        int deleted = tokenRepository.deleteByExpireDateBefore(OffsetDateTime.now().minusDays(1));
        log.info("token cleanup removed {} expired tokens", deleted);
    }
}

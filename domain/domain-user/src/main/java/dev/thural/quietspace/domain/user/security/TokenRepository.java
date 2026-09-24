package dev.thural.quietspace.domain.user.security;

import dev.thural.quietspace.core.shared.security.TokenRepository;
import dev.thural.quietspace.domain.user.security.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID>, dev.thural.quietspace.core.shared.security.TokenRepository {

    boolean existsByToken(String jwtToken);

    boolean existsByJti(String jti);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    int deleteByExpireDateBefore(OffsetDateTime cutoff);

    Optional<Token> getByEmail(String email);

    Optional<Token> findByToken(String token);

    Optional<Token> findByJti(String jti);
}

package dev.thural.quietspace.domain.user.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByToken(String token);
    Optional<Token> findByJti(String jti);
    boolean existsByToken(String token);
    boolean existsByJti(String jti);
    boolean existsByEmail(String email);
    Optional<Token> findByEmail(String email);
    void deleteByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.expireDate < :date")
    int deleteByExpireDateBefore(@Param("date") OffsetDateTime date);
}
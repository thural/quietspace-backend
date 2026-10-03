package dev.thural.quietspace.core.security.port;

public interface TokenBlacklistPort {
    boolean isBlacklisted(String token);
    boolean isRevokedByJti(String jti);
}
package dev.thural.quietspace.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtTokenService {
    String generateToken(UserDetails userDetails);
    String generateToken(java.util.Map<String, Object> extraClaims, UserDetails userDetails);
    String generateRefreshToken(java.util.Map<String, Object> extraClaims, UserDetails userDetails);
    boolean isTokenValid(String token, UserDetails userDetails);
    boolean isTokenExpired(String token);
    java.util.Date extractExpiration(String token);
    String extractUsername(String token);
    String extractJti(String token);
}
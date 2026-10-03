package dev.thural.quietspace.core.security;

import dev.thural.quietspace.core.security.port.JwtTokenService;
import dev.thural.quietspace.core.security.port.TokenBlacklistPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private TokenBlacklistPort tokenBlacklistPort;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void doFilter_givenNoAuthHeader_shouldSkipAndContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_givenNonBearerHeader_shouldSkip() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic some-token");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_givenBlacklistedToken_shouldSkip() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer blacklisted-token");
        when(tokenBlacklistPort.isBlacklisted("blacklisted-token")).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_givenRevokedJti_shouldSkip() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer revoked-jti-token");
        when(tokenBlacklistPort.isBlacklisted("revoked-jti-token")).thenReturn(false);
        when(jwtTokenService.extractJti("revoked-jti-token")).thenReturn("revoked-jti");
        when(tokenBlacklistPort.isRevokedByJti("revoked-jti")).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_givenNullUsername_shouldSkip() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(tokenBlacklistPort.isBlacklisted("some-token")).thenReturn(false);
        when(jwtTokenService.extractUsername("some-token")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_givenMalformedToken_shouldSkipAndContinueChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed-token");
        when(tokenBlacklistPort.isBlacklisted("malformed-token")).thenReturn(false);
        when(jwtTokenService.extractJti("malformed-token"))
                .thenThrow(new io.jsonwebtoken.MalformedJwtException("bad token"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_givenValidToken_shouldSetAuthenticationAndContinue() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenBlacklistPort.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtTokenService.extractUsername("valid-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtTokenService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_whenUserDetailsNotFound_shouldPropagate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(tokenBlacklistPort.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtTokenService.extractUsername("valid-token")).thenReturn("unknown");
        when(userDetailsService.loadUserByUsername("unknown")).thenThrow(new RuntimeException("User not found"));

        try {
            jwtFilter.doFilterInternal(request, response, filterChain);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("User not found");
        }

        verify(filterChain, never()).doFilter(request, response);
    }
}

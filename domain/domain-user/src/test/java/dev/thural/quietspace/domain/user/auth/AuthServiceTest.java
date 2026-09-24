package dev.thural.quietspace.auth;

import dev.thural.quietspace.auth.dto.AuthRequest;
import dev.thural.quietspace.auth.dto.AuthResponse;
import dev.thural.quietspace.auth.dto.RegistrationRequest;
import dev.thural.quietspace.security.JwtService;
import dev.thural.quietspace.security.Token;
import dev.thural.quietspace.security.TokenRepository;
import dev.thural.quietspace.shared.enums.Role;
import dev.thural.quietspace.shared.exception.ActivationTokenException;
import dev.thural.quietspace.shared.event.EmailEvent;
import dev.thural.quietspace.shared.exception.UserNotFoundException;
import dev.thural.quietspace.shared.service.SecurityAuditService;
import dev.thural.quietspace.shared.service.impl.EmailEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import dev.thural.quietspace.user.User;
import dev.thural.quietspace.user.UserRepository;
import dev.thural.quietspace.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailEventPublisher emailEventPublisher;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private SecurityAuditService auditService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @InjectMocks
    private AuthService authService;

    private RegistrationRequest registrationRequest;
    private AuthRequest authRequest;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .firstname("Test")
                .lastname("User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .accountLocked(false)
                .enabled(false)
                .build();

        registrationRequest = RegistrationRequest.builder()
                .username("testuser")
                .firstname("Test")
                .lastname("User")
                .email("test@example.com")
                .password("rawPassword")
                .build();

        authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("rawPassword")
                .build();

        ReflectionTestUtils.setField(authService, "activationUrl", "http://localhost:8080/activate");
    }

    @Test
    void register_givenValidRequest_shouldSaveUserAndSendEmail() {
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registrationRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getFirstname()).isEqualTo("Test");
        assertThat(savedUser.getLastname()).isEqualTo("User");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isAccountLocked()).isFalse();
        assertThat(savedUser.isEnabled()).isFalse();
        assertThat(savedUser.getProfileSettings()).isNotNull();

        verify(emailEventPublisher).publish(any(EmailEvent.class));
    }

    @Test
    void register_whenPublisherFails_shouldPropagate() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);
        doThrow(new RuntimeException("Broker error")).when(emailEventPublisher).publish(any(EmailEvent.class));

        assertThatThrownBy(() -> authService.register(registrationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Broker error");
    }

    @Test
    void authenticate_givenValidCredentials_shouldReturnAuthResponseWithTokens() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.authenticate(authRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(userId.toString());
        assertThat(response.getMessage()).contains("successful");
    }

    @Test
    void authenticate_givenBadCredentials_shouldThrow() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(authRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void activateAccount_givenValidToken_shouldNotThrow() {
        Token validToken = Token.builder()
                .token("valid-code")
                .expireDate(OffsetDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        when(tokenRepository.findByToken("valid-code")).thenReturn(Optional.of(validToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> authService.activateAccount("valid-code"));
    }

    @Test
    void activateAccount_givenExpiredToken_shouldResendEmailAndThrow() {
        Token expiredToken = Token.builder()
                .token("expired-code")
                .expireDate(OffsetDateTime.now().minusMinutes(5))
                .user(user)
                .build();
        when(tokenRepository.findByToken("expired-code")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.activateAccount("expired-code"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(emailEventPublisher).publish(any(EmailEvent.class));
    }

    @Test
    void activateAccount_givenNonExistentToken_shouldThrowActivationTokenException() {
        when(tokenRepository.findByToken("bad-code")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.activateAccount("bad-code"))
                .isInstanceOf(ActivationTokenException.class)
                .hasMessageContaining("invalid token");
    }

    @Test
    void signout_givenValidHeader_shouldBlacklistAndClearContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));
        when(tokenRepository.existsByToken(anyString())).thenReturn(false);
        when(userRepository.findUserByUsername("testuser")).thenReturn(Optional.of(user));
        when(userService.getSignedUser()).thenReturn(user);

        authService.signout("Bearer some.jwt.token");

        verify(tokenRepository).save(any(Token.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void signout_givenNullHeader_shouldDoNothing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));

        authService.signout(null);

        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    void signout_givenNonBearerHeader_shouldDoNothing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));

        authService.signout("Basic some.token");

        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    void refreshToken_givenValidToken_shouldReturnNewAccessToken() {
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(jwtService.isTokenValid("valid-refresh-token", user)).thenReturn(true);
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("new-access-token");
        when(tokenRepository.existsByToken("valid-refresh-token")).thenReturn(false);
        when(userRepository.findUserByUsername("testuser")).thenReturn(Optional.of(user));

        AuthResponse response = authService.refreshToken("Bearer valid-refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getMessage()).contains("refreshed");
    }

    @Test
    void refreshToken_givenBlacklistedToken_shouldReturnFailure() {
        when(tokenRepository.existsByToken("blacklisted-token")).thenReturn(true);

        AuthResponse response = authService.refreshToken("Bearer blacklisted-token");

        assertThat(response.getMessage()).contains("failed");
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    void refreshToken_givenNonBearerHeader_shouldReturnFailure() {
        AuthResponse response = authService.refreshToken("InvalidHeader");

        assertThat(response.getMessage()).contains("failed");
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    void addToBlacklist_givenNewToken_shouldSave() {
        when(tokenRepository.existsByToken("some.jwt")).thenReturn(false);
        when(userRepository.findUserByUsername("testuser")).thenReturn(Optional.of(user));

        authService.addToBlacklist("Bearer some.jwt", "testuser");

        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void addToBlacklist_givenAlreadyBlacklisted_shouldNotSave() {
        when(tokenRepository.existsByToken("some.jwt")).thenReturn(true);
        when(userRepository.findUserByUsername("testuser")).thenReturn(Optional.of(user));

        authService.addToBlacklist("Bearer some.jwt", "testuser");

        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    void resendActivationToken_givenEnabledAccount_shouldThrow() {
        user.setEnabled(true);
        when(userRepository.findUserEntityByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resendActivationToken("test@example.com"))
                .isInstanceOf(ActivationTokenException.class)
                .hasMessageContaining("already been activated");
    }

    @Test
    void resendActivationToken_givenDisabledAccount_shouldResendEmail() {
        when(userRepository.findUserEntityByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.resendActivationToken("test@example.com");

        verify(emailEventPublisher).publish(any(EmailEvent.class));
    }

    @Test
    void resendActivationToken_givenUnknownEmail_shouldThrow() {
        when(userRepository.findUserEntityByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendActivationToken("unknown@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}

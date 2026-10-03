package dev.thural.quietspace.domain.user.auth;

import dev.thural.quietspace.core.shared.enums.StatusType;
import dev.thural.quietspace.core.shared.event.EmailEvent;
import dev.thural.quietspace.core.shared.exception.ActivationTokenException;
import dev.thural.quietspace.core.shared.exception.CustomErrorException;
import dev.thural.quietspace.core.shared.exception.UserNotFoundException;
import dev.thural.quietspace.core.security.port.JwtTokenService;
import dev.thural.quietspace.domain.user.token.Token;
import dev.thural.quietspace.domain.user.token.TokenRepository;
import dev.thural.quietspace.core.shared.service.SecurityAuditService;
import dev.thural.quietspace.core.shared.service.impl.EmailEventPublisher;
import dev.thural.quietspace.domain.user.ProfileSettings;
import dev.thural.quietspace.domain.user.User;
import dev.thural.quietspace.domain.user.UserRepository;
import dev.thural.quietspace.domain.user.UserService;
import dev.thural.quietspace.domain.user.auth.dto.AuthRequest;
import dev.thural.quietspace.domain.user.auth.dto.AuthResponse;
import dev.thural.quietspace.domain.user.auth.dto.RegistrationRequest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static dev.thural.quietspace.core.shared.enums.Role.USER;
import static dev.thural.quietspace.core.shared.enums.StatusType.OFFLINE;
import static dev.thural.quietspace.core.shared.enums.StatusType.ONLINE;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;
    private final EmailEventPublisher emailEventPublisher;
    private final TokenRepository tokenRepository;
    private final SecurityAuditService auditService;
    private final MeterRegistry meterRegistry;

    @Value("${spring.application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest request) {
        log.info("registering new user with email: {}", request.getEmail());
        auditService.logRegistration(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByUsername(request.getUsername())) {
            auditService.logEvent("REGISTRATION_FAILED", request.getEmail(), "email or username already taken");
            throw new CustomErrorException(HttpStatus.BAD_REQUEST, "email or username is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .role(USER)
                .build();

        ProfileSettings settings = new ProfileSettings(user);
        user.setProfileSettings(settings);

        User savedUser = userRepository.save(user);
        sendValidationEmail(savedUser);
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        log.info("authenticating user by email: {}", request.getEmail());
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            var claims = new HashMap<String, Object>();
            User user = ((User) auth.getPrincipal());
            if (user == null) throw new IllegalStateException("authenticated principal cannot be null");
            claims.put("fullName", user.getFullName());

            String jwtAccessToken = jwtTokenService.generateToken(claims, user);
            String jwtRefreshToken = jwtTokenService.generateRefreshToken(claims, user);
            log.info("jwt token generated successfully for user: {}", user.getUsername());
            auditService.logLoginSuccess(user.getEmail());
            meterRegistry.counter("auth.login.success").increment();
            saveRefreshTokenJti(jwtRefreshToken, user);

            setOnlineStatus(user.getEmail(), ONLINE);

            return AuthResponse.builder()
                    .message("authentication was successful")
                    .userId(user.getId().toString())
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .refreshTokenId(jwtTokenService.extractJti(jwtRefreshToken))
                    .build();
        } catch (UsernameNotFoundException e) {
            auditService.logLoginFailure(request.getEmail());
            meterRegistry.counter("auth.login.failure").increment();
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }
    }

    @Transactional
    public void activateAccount(String token) {
        Token existingToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ActivationTokenException("invalid token, please try again"));

        if (OffsetDateTime.now().isAfter(existingToken.getExpireDate())) {
            User expiredUser = userRepository.findById(existingToken.getUserId())
                    .orElseThrow(UserNotFoundException::new);
            sendValidationEmail(expiredUser);
            throw new RuntimeException("activation token has expired... a new token has been sent");
        }

        User user = userRepository.findById(existingToken.getUserId()).orElseThrow(UserNotFoundException::new);
        user.setEnabled(true);
        existingToken.setValidateDate(OffsetDateTime.now());
        auditService.logAccountActivation(user.getEmail());
    }

    @Transactional
    public void signout(String authHeader) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication != null ? authentication.getName() : "unknown";
        auditService.logLogout(currentUserName);
        log.info("user signing out: {}", currentUserName);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            addToBlacklist(authHeader, currentUserName);
            setOnlineStatus(OFFLINE);
            SecurityContextHolder.clearContext();
        }
    }

    private String generateAndSaveActivationToken(User user) {
        String activationCode = generateActivationCode(6);

        Token token = Token.builder()
                .token(activationCode)
                .email(user.getEmail())
                .expireDate(OffsetDateTime.now().plusMinutes(15))
                .userId(user.getId())
                .build();

        tokenRepository.save(token);
        return activationCode;
    }

    public void sendValidationEmail(User user) {
        log.info("sending activation code to email address: {}", user.getEmail());
        String newActivationCode = generateAndSaveActivationToken(user);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getFullName());
        variables.put("confirmationUrl", activationUrl);
        variables.put("activationCode", newActivationCode);

        emailEventPublisher.publish(new EmailEvent(
                user.getEmail(),
                "account activation",
                "activate_account",
                variables
        ));
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder generatedCode = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(characters.length());
            generatedCode.append(characters.charAt(randomIndex));
        }

        log.info("generated activation code for user");
        return generatedCode.toString();
    }

    public void addToBlacklist(String authHeader, String username) {
        String jwtToken = authHeader.substring(7);
        boolean isBlacklisted = tokenRepository.existsByToken(jwtToken);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        if (!isBlacklisted) saveToken(jwtToken, user);
    }

    private void saveToken(String jwtToken, User user) {
        var jti = jwtTokenService.extractJti(jwtToken);
        meterRegistry.counter("auth.token.revoked").increment();
        tokenRepository.save(Token.builder()
                .token(jwtToken)
                .jti(jti)
                .email(user.getEmail())
                .userId(user.getId())
                .build());
    }

    public AuthResponse refreshToken(String authHeader) {
        String refreshToken = authHeader.substring(7);

        AuthResponse authResponse = AuthResponse.builder()
                .refreshToken(refreshToken)
                .message("token refresh was failed")
                .build();

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) return authResponse;
        if (tokenRepository.existsByToken(refreshToken)) return authResponse;

        String username = jwtTokenService.extractUsername(refreshToken);
        if (username == null) return authResponse;

        User user = userRepository.findUserByUsername(username).orElseThrow(UserNotFoundException::new);
        if (!jwtTokenService.isTokenValid(refreshToken, user)) {
            auditService.logEvent("TOKEN_REFRESH_FAILED", username, "invalid or expired refresh token");
            return authResponse;
        }

        String jti = jwtTokenService.extractJti(refreshToken);
        var storedToken = tokenRepository.findByJti(jti);

        if (storedToken.isPresent()) {
            if (storedToken.get().isUsed()) {
                auditService.logEvent("TOKEN_REPLAY_DETECTED", username, "refresh token replay attack detected");
                return authResponse;
            }
            storedToken.get().setUsed(true);
            tokenRepository.save(storedToken.get());
        }

        var claims = new HashMap<String, Object>();
        claims.put("fullName", user.getFullName());
        String newAccessToken = jwtTokenService.generateToken(claims, user);
        String newRefreshToken = jwtTokenService.generateRefreshToken(claims, user);
        saveRefreshTokenJti(newRefreshToken, user);

        auditService.logTokenRefresh(username);
        meterRegistry.counter("auth.token.refresh").increment();

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .refreshTokenId(jwtTokenService.extractJti(newRefreshToken))
                .message("token was refreshed")
                .userId(String.valueOf(user.getId()))
                .build();
    }

    private void saveRefreshTokenJti(String refreshToken, User user) {
        String jti = jwtTokenService.extractJti(refreshToken);
        tokenRepository.save(Token.builder()
                .token(jti)
                .jti(jti)
                .email(user.getEmail())
                .userId(user.getId())
                .used(false)
                .build());
    }

    public void resendActivationToken(String email) {
        User foundUser = userRepository
                .findUserEntityByEmail(email).orElseThrow(UserNotFoundException::new);
        if (foundUser.isEnabled())
            throw new ActivationTokenException("invalid request: account has already been activated");
        sendValidationEmail(foundUser);
    }

    private void setOnlineStatus(StatusType type) {
        // TODO: consider user settings after implementation
        User user = userService.getSignedUser();
        user.setStatusType(type);
    }

    private void setOnlineStatus(String email, StatusType type) {
        // TODO: consider user settings after implementation
        userRepository.findUserEntityByEmail(email)
                .ifPresent(user -> user.setStatusType(type));
    }
}
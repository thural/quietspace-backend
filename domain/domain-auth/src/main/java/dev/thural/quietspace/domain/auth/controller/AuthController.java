package dev.thural.quietspace.auth.controller;

import dev.thural.quietspace.auth.AuthService;
import dev.thural.quietspace.auth.dto.AuthRequest;
import dev.thural.quietspace.auth.dto.AuthResponse;
import dev.thural.quietspace.auth.dto.RegistrationRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
@SecurityRequirement(name = "bearerAuth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegistrationRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<Void> confirm(@RequestParam String token) {
        authService.activateAccount(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signout")
    ResponseEntity<Void> signout(@RequestHeader("Authorization") String authHeader) {
        authService.signout(authHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.signout(authHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-token")
    ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.refreshToken(authHeader));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.refreshToken(authHeader));
    }

    @PostMapping("/resend-code")
    ResponseEntity<Void> resendActivationEmail(@RequestParam String email) {
        authService.resendActivationToken(email);
        return ResponseEntity.ok().build();
    }

}

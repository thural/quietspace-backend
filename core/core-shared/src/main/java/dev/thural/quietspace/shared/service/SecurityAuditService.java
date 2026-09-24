package dev.thural.quietspace.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Slf4j
public class SecurityAuditService {

    public void logEvent(String eventType, String principal, String detail) {
        var event = Map.of(
                "event", eventType,
                "principal", principal != null ? maskEmail(principal) : "anonymous",
                "detail", detail,
                "timestamp", OffsetDateTime.now().toString()
        );
        log.info("SECURITY_AUDIT: {}", event);
    }

    public void logLoginSuccess(String email) {
        logEvent("LOGIN_SUCCESS", email, "authentication succeeded");
    }

    public void logLoginFailure(String email) {
        logEvent("LOGIN_FAILURE", email, "authentication failed");
    }

    public void logLogout(String username) {
        logEvent("LOGOUT", username, "user signed out");
    }

    public void logTokenRefresh(String username) {
        logEvent("TOKEN_REFRESH", username, "access token refreshed");
    }

    public void logRegistration(String email) {
        logEvent("REGISTRATION", email, "new user registered");
    }

    public void logAccountActivation(String email) {
        logEvent("ACCOUNT_ACTIVATION", email, "account activated");
    }

    public void logAccessDenied(String path, String username) {
        logEvent("ACCESS_DENIED", username, "access denied to " + path);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        var parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "***@" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}

package dev.thural.quietspace.security;

import dev.thural.quietspace.shared.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final SecurityAuditService auditService;

    public JwtAuthEntryPoint(ObjectMapper objectMapper, SecurityAuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void commence(
            @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable AuthenticationException authException
    ) throws IOException {
        if (request == null || response == null || authException == null) return;
        var path = request.getRequestURI();
        auditService.logEvent("AUTH_FAILURE", extractPrincipal(request), "unauthorized access to " + path);
        SecurityErrorHandler.handleSecurityError(
                request,
                response,
                authException,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Authentication failed",
                objectMapper
        );
    }

    private String extractPrincipal(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).substring(0, Math.min(20, authHeader.substring(7).length())) + "...";
        }
        return request.getRemoteAddr();
    }
}
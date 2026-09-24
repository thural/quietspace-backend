package dev.thural.quietspace.security;

import dev.thural.quietspace.shared.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityAuditService auditService;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper, SecurityAuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void handle(
            @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response,
            @Nullable AccessDeniedException accessDeniedException
    ) throws IOException {
        if (request == null || response == null || accessDeniedException == null) return;
        var path = request.getRequestURI();
        auditService.logAccessDenied(path, extractPrincipal(request));
        SecurityErrorHandler.handleSecurityError(
                request,
                response,
                accessDeniedException,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Access denied",
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
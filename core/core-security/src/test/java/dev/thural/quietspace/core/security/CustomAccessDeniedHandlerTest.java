package dev.thural.quietspace.security;

import dev.thural.quietspace.shared.service.SecurityAuditService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @Mock
    private SecurityAuditService auditService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AccessDeniedException accessDeniedException;

    private ObjectMapper objectMapper;
    private CustomAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new tools.jackson.databind.ObjectMapper();
        handler = new CustomAccessDeniedHandler(objectMapper, auditService);
    }

    @Test
    void handle_givenNullRequest_shouldReturnEarly() throws Exception {
        handler.handle(null, response, accessDeniedException);

        verifyNoInteractions(auditService);
    }

    @Test
    void handle_givenNullResponse_shouldReturnEarly() throws Exception {
        handler.handle(request, null, accessDeniedException);

        verifyNoInteractions(auditService);
    }

    @Test
    void handle_givenNullException_shouldReturnEarly() throws Exception {
        handler.handle(request, response, null);

        verifyNoInteractions(auditService);
    }

    @Test
    void handle_givenValidArguments_shouldLogAccessDenied() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/users");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(accessDeniedException.getMessage()).thenReturn("Access denied");

        handler.handle(request, response, accessDeniedException);

        verify(auditService).logAccessDenied("/api/v1/admin/users", request.getRemoteAddr());
    }

    @Test
    void handle_givenBearerToken_shouldExtractPrefix() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/settings");
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIn0.signature");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(accessDeniedException.getMessage()).thenReturn("Forbidden");

        handler.handle(request, response, accessDeniedException);

        verify(auditService).logAccessDenied(eq("/api/v1/admin/settings"), contains("eyJhbGciOiJIUzI1NiJ9"));
    }
}

package dev.thural.quietspace.security;

import dev.thural.quietspace.shared.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthEntryPointTest {

    @Mock
    private SecurityAuditService auditService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthenticationException authException;

    private ObjectMapper objectMapper;
    private JwtAuthEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        entryPoint = new JwtAuthEntryPoint(objectMapper, auditService);
    }

    @Test
    void commence_givenNullRequest_shouldReturnEarly() throws Exception {
        entryPoint.commence(null, response, authException);

        verifyNoInteractions(auditService);
    }

    @Test
    void commence_givenNullResponse_shouldReturnEarly() throws Exception {
        entryPoint.commence(request, null, authException);

        verifyNoInteractions(auditService);
    }

    @Test
    void commence_givenNullException_shouldReturnEarly() throws Exception {
        entryPoint.commence(request, response, null);

        verifyNoInteractions(auditService);
    }

    @Test
    void commence_givenValidArguments_shouldLogAuthFailure() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(authException.getMessage()).thenReturn("Unauthenticated");

        entryPoint.commence(request, response, authException);

        verify(auditService).logEvent(eq("AUTH_FAILURE"), eq(null), eq("unauthorized access to /api/v1/posts"));
    }

    @Test
    void commence_givenBearerToken_shouldExtractPrefix() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/settings");
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIn0.signature");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(authException.getMessage()).thenReturn("Unauthorized");

        entryPoint.commence(request, response, authException);

        verify(auditService).logEvent(eq("AUTH_FAILURE"), contains("eyJhbGciOiJIUzI1NiJ9"), eq("unauthorized access to /api/v1/admin/settings"));
    }
}

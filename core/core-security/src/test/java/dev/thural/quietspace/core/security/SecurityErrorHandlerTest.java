package dev.thural.quietspace.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityErrorHandlerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PrintWriter writer;

    @Test
    void handleSecurityError_shouldWriteErrorResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(response.getWriter()).thenReturn(writer);

        SecurityErrorHandler.handleSecurityError(request, response,
                new RuntimeException("boom"), 401, "Unauthorized", "fallback", objectMapper);

        verify(response).setStatus(401);
        verify(objectMapper).writeValue(any(PrintWriter.class), any());
    }

    @Test
    void handleSecurityError_givenNullMessage_shouldUseDefault() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(response.getWriter()).thenReturn(writer);

        SecurityErrorHandler.handleSecurityError(request, response,
                new RuntimeException(), 403, "Forbidden", "fallback", objectMapper);

        verify(objectMapper).writeValue(any(PrintWriter.class), any());
    }

    @Test
    void constructor_shouldThrowAssertionError() throws Exception {
        var constructor = SecurityErrorHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(() -> constructor.newInstance())
                .hasCauseInstanceOf(AssertionError.class);
    }

    @Test
    void handleSecurityError_givenWriteFailure_shouldRethrow() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/posts");
        when(response.getWriter()).thenThrow(new IOException("broken"));

        assertThatThrownBy(() -> SecurityErrorHandler.handleSecurityError(request, response,
                new RuntimeException("boom"), 500, "Error", "fallback", objectMapper))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to write error response");
    }
}

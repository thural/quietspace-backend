package dev.thural.quietspace.core.shared.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void of_createsResponseWithCurrentTimestamp() {
        var response = ErrorResponse.of(400, "Bad Request", "Invalid input", "/api/test");

        assertThat(response.timestamp()).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.message()).isEqualTo("Invalid input");
        assertThat(response.path()).isEqualTo("/api/test");
    }

    @Test
    void record_createsCompleteResponse() {
        var now = LocalDateTime.now();
        var response = new ErrorResponse(now, 500, "Internal Error", "Server error", "/api");

        assertThat(response.timestamp()).isEqualTo(now);
        assertThat(response.status()).isEqualTo(500);
        assertThat(response.error()).isEqualTo("Internal Error");
        assertThat(response.message()).isEqualTo("Server error");
        assertThat(response.path()).isEqualTo("/api");
    }
}
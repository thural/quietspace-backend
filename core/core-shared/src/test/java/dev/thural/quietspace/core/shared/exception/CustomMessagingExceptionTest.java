package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomMessagingExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new CustomMessagingException("messaging error");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("messaging error occurred: messaging error");
    }
}
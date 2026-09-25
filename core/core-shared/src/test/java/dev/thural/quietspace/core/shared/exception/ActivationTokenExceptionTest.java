package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivationTokenExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new ActivationTokenException("token invalid");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("token invalid");
    }
}
package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnauthorizedExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new UnauthorizedException("unauthorized");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("unauthorized");
    }
}
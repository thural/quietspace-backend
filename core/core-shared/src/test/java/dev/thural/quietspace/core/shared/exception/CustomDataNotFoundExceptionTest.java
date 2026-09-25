package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomDataNotFoundExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new CustomDataNotFoundException("not found");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("not found");
    }
}
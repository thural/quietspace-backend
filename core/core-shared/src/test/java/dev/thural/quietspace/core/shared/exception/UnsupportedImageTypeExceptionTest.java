package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnsupportedImageTypeExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new UnsupportedImageTypeException("unsupported");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("unsupported");
    }
}
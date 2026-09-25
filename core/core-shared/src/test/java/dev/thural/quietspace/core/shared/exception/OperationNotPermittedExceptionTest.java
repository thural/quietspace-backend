package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationNotPermittedExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new OperationNotPermittedException("not permitted");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("not permitted");
    }
}
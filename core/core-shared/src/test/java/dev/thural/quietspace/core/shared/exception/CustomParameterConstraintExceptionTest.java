package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomParameterConstraintExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new CustomParameterConstraintException("invalid param");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("A parameter constraint error occurred: invalid param");
    }
}
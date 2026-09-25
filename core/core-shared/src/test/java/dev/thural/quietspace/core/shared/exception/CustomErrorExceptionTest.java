package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomErrorExceptionTest {

    @Test
    void noArgsConstructor_createsExceptionWithDefaults() {
        var ex = new CustomErrorException();
        assertThat(ex.getStatus()).isNull();
        assertThat(ex.getData()).isNull();
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void messageConstructor_setsMessageAndDefaultStatus() {
        var ex = new CustomErrorException("test error");
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ex.getData()).isNull();
    }

    @Test
    void statusAndMessageConstructor_setsBoth() {
        var ex = new CustomErrorException(org.springframework.http.HttpStatus.BAD_REQUEST, "bad request");
        assertThat(ex.getMessage()).isEqualTo("bad request");
        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(ex.getData()).isNull();
    }

    @Test
    void fullConstructor_setsAllFields() {
        var data = new Object();
        var ex = new CustomErrorException(org.springframework.http.HttpStatus.NOT_FOUND, "not found", data);
        assertThat(ex.getMessage()).isEqualTo("not found");
        assertThat(ex.getStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
        assertThat(ex.getData()).isSameAs(data);
    }
}
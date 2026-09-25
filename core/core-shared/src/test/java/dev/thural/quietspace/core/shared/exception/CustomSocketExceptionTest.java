package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import java.net.SocketException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomSocketExceptionTest {

    @Test
    void extendsSocketException() {
        var ex = new CustomSocketException("socket error");
        assertThat(ex).isInstanceOf(SocketException.class);
        assertThat(ex.getMessage()).isEqualTo("socket exception occurred: socket error");
    }
}
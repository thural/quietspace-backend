package dev.thural.quietspace.core.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUploadExceptionTest {

    @Test
    void extendsRuntimeException() {
        var ex = new ImageUploadException("upload failed");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("upload failed");
    }
}
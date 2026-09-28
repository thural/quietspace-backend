package dev.thural.quietspace.core.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImageCompressionUtilTest {

    @Test
    void compressImage_givenValidImage_returnsCompressedBytes() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] input = baos.toByteArray();

        byte[] compressed = new ImageCompressionUtil().compressImage(input, 50 * 1024);

        assertThat(compressed).isNotNull();
        assertThat(compressed.length).isGreaterThan(0);
    }

    @Test
    void compressImage_givenValidInputStream_returnsCompressedBytes() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        ByteArrayInputStream input = new ByteArrayInputStream(baos.toByteArray());

        byte[] compressed = new ImageCompressionUtil().compressImage(input, 50 * 1024);

        assertThat(compressed).isNotNull();
        assertThat(compressed.length).isGreaterThan(0);
    }

    @Test
    void compressImage_givenInvalidBytes_throwsIOException() {
        assertThatThrownBy(() -> new ImageCompressionUtil().compressImage(new byte[]{1,2,3}, 100))
                .isInstanceOf(IOException.class);
    }

    @Test
    void compressImage_givenInvalidInputStream_throwsIOException() {
        assertThatThrownBy(() -> new ImageCompressionUtil().compressImage(new ByteArrayInputStream(new byte[]{1,2,3}), 100))
                .isInstanceOf(IOException.class);
    }

    @Test
    void decompressImage_returnsSameBytes() {
        byte[] input = new byte[]{1, 2, 3};
        byte[] output = new ImageCompressionUtil().decompressImage(input);
        assertThat(output).isSameAs(input);
    }

    @Test
    void decompressImage_givenInputStream_returnsBytes() throws IOException {
        byte[] input = new byte[]{1, 2, 3};
        ByteArrayInputStream is = new ByteArrayInputStream(input);
        byte[] output = new ImageCompressionUtil().decompressImage(is);
        assertThat(output).isEqualTo(input);
    }
}
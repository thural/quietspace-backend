package dev.thural.quietspace.shared.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageCompressionUtilTest {

    private final ImageCompressionUtil util = new ImageCompressionUtil();

    private byte[] createTestJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    void compressImage_givenValidJpeg_returnsSmallerBytes() throws IOException {
        byte[] original = createTestJpeg(200, 200);
        InputStream inputStream = new ByteArrayInputStream(original);

        byte[] compressed = util.compressImage(inputStream, 10 * 1024);

        assertThat(compressed).isNotEmpty();
        assertThat(compressed.length).isLessThanOrEqualTo(original.length);
    }

    @Test
    void compressImage_givenZeroTargetSize_usesDefaultAndCompresses() throws IOException {
        byte[] original = createTestJpeg(200, 200);
        InputStream inputStream = new ByteArrayInputStream(original);

        byte[] compressed = util.compressImage(inputStream, 0);

        assertThat(compressed).isNotEmpty();
    }

    @Test
    void compressImage_givenUnreadableStream_throwsIOException() {
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        assertThatThrownBy(() -> util.compressImage(emptyStream, 10 * 1024))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unable to read image");
    }

    @Test
    void decompressImage_givenByteArray_returnsSameBytes() {
        byte[] input = "test-image-data".getBytes();

        byte[] result = util.decompressImage(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void decompressImage_givenInputStream_returnsAllBytes() throws IOException {
        byte[] input = "test-stream-data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(input);

        byte[] result = util.decompressImage(inputStream);

        assertThat(result).isEqualTo(input);
    }
}

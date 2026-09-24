package dev.thural.quietspace.core.shared.util;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class ImageCompressionUtil {
    private static final int DEFAULT_TARGET_SIZE = 100 * 1024; // 100KB
    private static final double MIN_QUALITY = 0.1; // Minimum quality threshold
    private static final int MAX_ITERATIONS = 10; // Prevent infinite loops

    /**
     * Compress image with improved format-specific handling
     *
     * @param inputStream     Original image input stream
     * @param targetSizeBytes Target size in bytes
     * @return Compressed image bytes
     * @throws IOException If compression fails
     */
    public byte[] compressImage(InputStream inputStream, int targetSizeBytes) throws IOException {
        // Use default target size if not specified
        if (targetSizeBytes <= 0) {
            targetSizeBytes = DEFAULT_TARGET_SIZE;
        }

        // Read the original image to determine format and characteristics
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("Unable to read image");
        }

        // Determine image format and apply appropriate compression strategy
        return compressImageWithImprovedQuality(originalImage, targetSizeBytes);
    }

    /**
     * Compress image from byte array with improved format-specific handling
     *
     * @param imageBytes      Original image bytes
     * @param targetSizeBytes Target size in bytes
     * @return Compressed image bytes
     * @throws IOException If compression fails
     */
    public byte[] compressImage(byte[] imageBytes, int targetSizeBytes) throws IOException {
        return compressImage(new ByteArrayInputStream(imageBytes), targetSizeBytes);
    }

    /**
     * Enhanced compression method with improved quality preservation
     *
     * @param originalImage   Original BufferedImage
     * @param targetSizeBytes Target size in bytes
     * @return Compressed image bytes
     * @throws IOException If compression fails
     */
    private byte[] compressImageWithImprovedQuality(BufferedImage originalImage, int targetSizeBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] compressedImage;
        double quality = 0.9; // Start with high quality
        int iterations = 0;

        // Determine optimal scaling based on image size
        double scale = calculateOptimalScale(originalImage, targetSizeBytes);

        do {
            // Reset output stream
            outputStream.reset();

            // Compress with more nuanced approach
            Thumbnails.of(originalImage)
                    .scale(scale)
                    .outputQuality(quality)
                    .outputFormat("jpeg") // Convert to JPEG for consistent compression
                    .toOutputStream(outputStream);

            compressedImage = outputStream.toByteArray();

            // Logging for debugging
            log.info("Compression iteration {}: size={} bytes, quality={}, scale={}",
                    iterations, compressedImage.length, quality, scale);

            // Adaptive quality and scale reduction
            if (compressedImage.length > targetSizeBytes) {
                quality -= 0.05; // Smaller quality reduction steps

                // Further reduce scale if quality is getting too low
                if (quality < 0.5 && scale > 0.5) {
                    scale *= 0.9; // Gradually reduce scale
                }
            }

            iterations++;
        } while (compressedImage.length > targetSizeBytes
                && quality > MIN_QUALITY
                && iterations < MAX_ITERATIONS);

        // Final logging
        log.info("Final compression: size={} bytes, quality={}, iterations={}",
                compressedImage.length, quality, iterations);

        return compressedImage;
    }

    /**
     * Calculate optimal scale to reduce image size
     *
     * @param image           Original image
     * @param targetSizeBytes Target size in bytes
     * @return Optimal scale factor
     */
    private double calculateOptimalScale(BufferedImage image, int targetSizeBytes) {
        // Calculate initial scale based on image dimensions and target size
        double originalSize = image.getWidth() * image.getHeight();
        double targetSize = targetSizeBytes * 8.0; // Rough estimate

        // Calculate scale, ensuring it doesn't reduce below 0.1
        double scale = Math.sqrt(targetSize / originalSize);
        return Math.max(Math.min(scale, 1.0), 0.1);
    }

    /**
     * Decompress image (pass-through for Thumbnails compression)
     *
     * @param compressedImage Compressed image bytes
     * @return Decompressed image bytes
     */
    public byte[] decompressImage(byte[] compressedImage) {
        return compressedImage;
    }

    /**
     * Decompress image from input stream (pass-through)
     *
     * @param compressedInputStream Compressed image input stream
     * @return Decompressed image bytes
     * @throws IOException If reading the stream fails
     */
    public byte[] decompressImage(InputStream compressedInputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = compressedInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}
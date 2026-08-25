package com.alfy.api.service;

import com.alfy.api.entity.MediaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MediaThumbnailServiceTests {

    private final MediaThumbnailService service = new MediaThumbnailService();

    @TempDir
    Path storageRoot;

    @BeforeEach
    void configureStorageRoot() {
        ReflectionTestUtils.setField(service, "storageRoot", storageRoot.toString());
    }

    @Test
    void generatesAndReusesBoundedThumbnail() throws Exception {
        Path source = storageRoot.resolve("2026-08/source.jpg");
        Files.createDirectories(source.getParent());
        BufferedImage image = new BufferedImage(1_200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.CYAN);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ImageIO.write(image, "jpg", source.toFile());

        MediaAsset media = new MediaAsset();
        media.setId(42L);
        media.setMediaType("IMAGE");
        media.setStorageKey("2026-08/source.jpg");
        media.setMimeType("image/jpeg");
        media.setSha256("0123456789abcdef0123456789abcdef");

        var first = service.get(media);
        var second = service.get(media);
        BufferedImage thumbnail = ImageIO.read(first.path().toFile());

        assertThat(first.path()).isEqualTo(second.path());
        assertThat(first.path()).isRegularFile();
        assertThat(first.mimeType()).isEqualTo("image/jpeg");
        assertThat(first.etag()).isEqualTo("\"thumbnail-0123456789abcdef0123\"");
        assertThat(thumbnail.getWidth()).isLessThanOrEqualTo(480);
        assertThat(thumbnail.getHeight()).isLessThanOrEqualTo(360);
        assertThat(first.path()).isNotEqualTo(source);
    }
}

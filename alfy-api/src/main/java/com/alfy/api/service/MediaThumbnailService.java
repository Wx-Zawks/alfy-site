package com.alfy.api.service;

import com.alfy.api.entity.MediaAsset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Generates small, disk-cached previews so the admin never downloads every original image. */
@Service
public class MediaThumbnailService {

    private static final int MAX_WIDTH = 480;
    private static final int MAX_HEIGHT = 360;
    private final Map<String, Object> generationLocks = new ConcurrentHashMap<>();

    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}")
    private String storageRoot;

    public ThumbnailResource get(MediaAsset media) throws IOException {
        Path source = resolveSource(media);
        if (!"IMAGE".equals(media.getMediaType())) {
            return original(source, media);
        }

        String fingerprint = media.getSha256() == null || media.getSha256().isBlank()
                ? String.valueOf(media.getId())
                : media.getSha256().substring(0, Math.min(20, media.getSha256().length()));
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path thumbnail = root.resolve(".thumbnails")
                .resolve(media.getId() + "-" + fingerprint + ".jpg")
                .normalize();
        if (!thumbnail.startsWith(root)) {
            throw new IOException("缩略图路径不合法");
        }
        if (Files.isRegularFile(thumbnail)) {
            return thumbnail(thumbnail, fingerprint);
        }

        Object lock = generationLocks.computeIfAbsent(fingerprint, ignored -> new Object());
        try {
            synchronized (lock) {
                if (Files.isRegularFile(thumbnail)) {
                    return thumbnail(thumbnail, fingerprint);
                }
                BufferedImage input = ImageIO.read(source.toFile());
                if (input == null) {
                    // Standard ImageIO may not decode WebP. The original remains a safe fallback.
                    return original(source, media);
                }
                double scale = Math.min(
                        1D,
                        Math.min((double) MAX_WIDTH / input.getWidth(), (double) MAX_HEIGHT / input.getHeight())
                );
                int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
                BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = output.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics.drawImage(input, 0, 0, width, height, null);
                } finally {
                    graphics.dispose();
                }

                Files.createDirectories(thumbnail.getParent());
                Path temporary = Files.createTempFile(thumbnail.getParent(), "thumb-", ".jpg");
                try {
                    if (!ImageIO.write(output, "jpg", temporary.toFile())) {
                        throw new IOException("无法生成图片缩略图");
                    }
                    try {
                        Files.move(temporary, thumbnail, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException exception) {
                        Files.move(temporary, thumbnail, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
                return thumbnail(thumbnail, fingerprint);
            }
        } finally {
            generationLocks.remove(fingerprint, lock);
        }
    }

    private Path resolveSource(MediaAsset media) throws IOException {
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path source = root.resolve(media.getStorageKey()).normalize();
        if (!source.startsWith(root) || !Files.isRegularFile(source)) {
            throw new IOException("媒体文件不存在");
        }
        return source;
    }

    private static ThumbnailResource original(Path source, MediaAsset media) throws IOException {
        return new ThumbnailResource(
                source,
                media.getMimeType() == null ? "application/octet-stream" : media.getMimeType(),
                "\"media-" + media.getId() + "-" + Files.getLastModifiedTime(source).toMillis() + "\""
        );
    }

    private static ThumbnailResource thumbnail(Path path, String fingerprint) {
        return new ThumbnailResource(path, "image/jpeg", "\"thumbnail-" + fingerprint + "\"");
    }

    public record ThumbnailResource(Path path, String mimeType, String etag) { }
}

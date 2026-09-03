package com.alfy.api.service;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * 上传图片的服务端压缩：限制最大边长并按需重编码，避免原图（常见 5-7MB）
 * 直接进存储并原样吐给官网访客。仅处理 JPEG/PNG，GIF 动图与 WebP 保持原样。
 */
@Service
public class ImageOptimizationService {

    /** 超过该体积才压缩，小图保持原样避免无谓的画质损失。 */
    static final long MIN_BYTES_TO_OPTIMIZE = 500L * 1024;
    static final int MAX_EDGE = 1920;
    static final float JPEG_QUALITY = 0.85f;

    /**
     * 尝试压缩目标文件。返回 null 表示无需或无法优化，调用方保留原文件。
     * 成功时 target 已被压缩后的内容覆盖，返回结果携带新的 MIME 类型与扩展名
     * （无透明通道的 PNG 会转为 JPEG）。
     */
    public OptimizedImage optimize(Path target, String contentType) {
        if (contentType == null
                || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return null;
        }
        try {
            if (Files.size(target) <= MIN_BYTES_TO_OPTIMIZE) {
                return null;
            }
            BufferedImage input = ImageIO.read(target.toFile());
            if (input == null) {
                return null;
            }
            BufferedImage scaled = scaleDown(input);
            boolean hasAlpha = input.getColorModel().hasAlpha();

            Path temporary = Files.createTempFile(target.getParent(), "optimize-", ".tmp");
            String newContentType;
            try {
                if (hasAlpha) {
                    ImageIO.write(scaled, "png", temporary.toFile());
                    newContentType = "image/png";
                } else {
                    writeJpeg(scaled, temporary);
                    newContentType = "image/jpeg";
                }
                long originalSize = Files.size(target);
                long optimizedSize = Files.size(temporary);
                if (optimizedSize >= originalSize) {
                    return null;
                }
                Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return new OptimizedImage(newContentType, newContentType.equals("image/png") ? ".png" : ".jpg");
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException | RuntimeException exception) {
            // 压缩失败不应阻断上传，保留原图即可。
            return null;
        }
    }

    private static BufferedImage scaleDown(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        if (Math.max(width, height) <= MAX_EDGE) {
            return input;
        }
        double scale = (double) MAX_EDGE / Math.max(width, height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        int type = input.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(input, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static void writeJpeg(BufferedImage image, Path target) throws IOException {
        BufferedImage rgb = image.getType() == BufferedImage.TYPE_INT_RGB
                ? image
                : new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        if (rgb != image) {
            Graphics2D graphics = rgb.createGraphics();
            try {
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("当前运行环境不支持 JPEG 编码");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        try (OutputStream output = Files.newOutputStream(target);
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public record OptimizedImage(String contentType, String extension) { }
}

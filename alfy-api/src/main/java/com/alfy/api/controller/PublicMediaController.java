package com.alfy.api.controller;

import com.alfy.api.entity.MediaAsset;
import com.alfy.api.service.PublicMediaService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** 只输出已发布文章、产品等公开内容所关联的媒体文件。 */
@RestController
@RequestMapping("/api/v1/public/media")
@RequiredArgsConstructor
public class PublicMediaController {

    private final PublicMediaService publicMediaService;

    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}")
    private String storageRoot;

    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> getMedia(@PathVariable @Min(1) Long mediaId) throws IOException {
        MediaAsset media = publicMediaService.getPublicMedia(mediaId);
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path target = root.resolve(media.getStorageKey()).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new IOException("媒体文件不存在");
        }
        MediaType contentType = media.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(media.getMimeType());
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(new FileSystemResource(target));
    }
}

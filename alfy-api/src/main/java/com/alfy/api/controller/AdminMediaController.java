package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminMediaResponse;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminMediaService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {
    private final AdminMediaService service;
    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}") private String storageRoot;
    @GetMapping public ApiResponse<PageResponse<AdminMediaResponse>> list(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") @Min(1) long page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) { Page<AdminMediaResponse> result = service.list(keyword, page, size); return ApiResponse.success(PageResponse.from(result)); }
    @PostMapping(consumes = "multipart/form-data") public ApiResponse<AdminMediaResponse> upload(@RequestPart("file") MultipartFile file, @RequestParam(required = false) String altText, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.upload(file, altText, principal)); }
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponse<AdminMediaResponse> update(
            @PathVariable @Min(1) Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(required = false) String originalFilename,
            @RequestParam(required = false) String altText,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                service.update(id, file, originalFilename, altText, principal));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        service.delete(id, principal);
        return ApiResponse.success();
    }
    @GetMapping("/{id}/file") public ResponseEntity<Resource> file(@PathVariable @Min(1) Long id) throws IOException {
        MediaAsset media = service.getAsset(id);
        Path root = Path.of(storageRoot).toAbsolutePath().normalize(); Path target = root.resolve(media.getStorageKey()).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) throw new IOException("媒体文件不存在");
        MediaType type = media.getMimeType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(media.getMimeType());
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(target));
    }
}

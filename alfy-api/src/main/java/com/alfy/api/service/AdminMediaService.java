package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminMediaResponse;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.MediaAssetMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminMediaService {
    private static final long MAX_FILE_SIZE = 30L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "video/mp4", "application/pdf");
    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogService operationLogService;
    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}") private String storageRoot;

    @Transactional
    public AdminMediaResponse upload(MultipartFile file, String altText, AdminPrincipal principal) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(ErrorCode.BAD_REQUEST, "单个文件不能超过 30MB");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG、WebP、GIF、MP4 和 PDF 文件");
        String originalFilename = safeFilename(file.getOriginalFilename());
        String extension = extension(originalFilename);
        String storageKey = LocalDate.now() + "/" + UUID.randomUUID() + extension;
        Path root = Path.of(storageRoot).toAbsolutePath().normalize(); Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的文件路径");
        try { Files.createDirectories(target.getParent()); try (InputStream input = file.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); } }
        catch (IOException e) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "素材保存失败"); }
        MediaAsset media = new MediaAsset(); media.setMediaType(typeOf(contentType)); media.setStorageKey(storageKey.replace('\\', '/')); media.setOriginalFilename(originalFilename); media.setMimeType(contentType); media.setFileSize(file.getSize()); media.setSha256(sha256(target)); media.setAltText(trim(altText)); mediaAssetMapper.insert(media);
        operationLogService.record(principal.id(), "UPLOAD", "MEDIA", media.getId(), "上传素材 " + originalFilename); return toResponse(media);
    }
    public Page<AdminMediaResponse> list(String keyword, long page, long size) {
        Page<MediaAsset> assets = mediaAssetMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<MediaAsset>().and(keyword != null && !keyword.isBlank(), q -> q.like(MediaAsset::getOriginalFilename, keyword).or().like(MediaAsset::getAltText, keyword)).orderByDesc(MediaAsset::getCreatedAt));
        Page<AdminMediaResponse> result = new Page<>(assets.getCurrent(), assets.getSize(), assets.getTotal()); result.setRecords(assets.getRecords().stream().map(this::toResponse).toList()); return result;
    }
    public MediaAsset getAsset(Long id) {
        MediaAsset media = mediaAssetMapper.selectById(id);
        if (media == null) throw new BusinessException(ErrorCode.NOT_FOUND, "素材不存在");
        return media;
    }
    private AdminMediaResponse toResponse(MediaAsset media) { return new AdminMediaResponse(media.getId(), media.getMediaType(), media.getOriginalFilename(), media.getMimeType(), media.getFileSize(), media.getWidth(), media.getHeight(), media.getAltText(), "/admin/media/" + media.getId() + "/file", media.getCreatedAt()); }
    private static String typeOf(String type) { return type.startsWith("image/") ? "IMAGE" : type.startsWith("video/") ? "VIDEO" : "DOCUMENT"; }
    private static String safeFilename(String name) { String safe = name == null ? "upload" : Path.of(name).getFileName().toString().replaceAll("[\\r\\n]", "_"); return safe.isBlank() ? "upload" : safe; }
    private static String extension(String name) { int dot = name.lastIndexOf('.'); return dot < 0 ? "" : name.substring(dot).toLowerCase(); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String sha256(Path path) { try (InputStream input = Files.newInputStream(path)) { MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[8192]; for (int read; (read = input.read(buffer)) > 0;) digest.update(buffer, 0, read); return HexFormat.of().formatHex(digest.digest()); } catch (IOException | NoSuchAlgorithmException e) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法校验上传文件"); } }
}

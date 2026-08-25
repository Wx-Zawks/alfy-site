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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminMediaService {
    private static final long MAX_FILE_SIZE = 30L * 1024 * 1024;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_ALT_TEXT_LENGTH = 255;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "video/mp4",
            "application/pdf"
    );

    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogService operationLogService;

    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}")
    private String storageRoot;

    @Transactional
    public AdminMediaResponse upload(MultipartFile file, String altText, AdminPrincipal principal) {
        StoredFile stored = store(file);
        try {
            MediaAsset media = new MediaAsset();
            applyStoredFile(media, stored);
            media.setOriginalFilename(stored.originalFilename());
            media.setAltText(normalizeAltText(altText));
            mediaAssetMapper.insert(media);
            operationLogService.record(
                    principal.id(),
                    "UPLOAD",
                    "MEDIA",
                    media.getId(),
                    "上传素材 " + media.getOriginalFilename()
            );
            deleteCreatedFileIfTransactionRollsBack(stored.target());
            return toResponse(media);
        } catch (RuntimeException exception) {
            deleteQuietly(stored.target());
            throw exception;
        }
    }

    @Transactional
    public AdminMediaResponse update(
            Long id,
            MultipartFile replacementFile,
            String originalFilename,
            String altText,
            AdminPrincipal principal
    ) {
        MediaAsset media = getAsset(id);
        StoredFile replacement = null;
        Path oldTarget = resolveStoragePath(media.getStorageKey());
        try {
            if (replacementFile != null && !replacementFile.isEmpty()) {
                replacement = store(replacementFile);
                if (!replacement.mediaType().equals(media.getMediaType())) {
                    throw new BusinessException(
                            ErrorCode.BAD_REQUEST,
                            "替换文件必须与原素材保持相同类型"
                    );
                }
                applyStoredFile(media, replacement);
                media.setWidth(null);
                media.setHeight(null);
                media.setDurationSeconds(null);
                if (originalFilename == null) {
                    media.setOriginalFilename(replacement.originalFilename());
                }
            }
            if (originalFilename != null) {
                media.setOriginalFilename(normalizeDisplayFilename(originalFilename));
            }
            if (altText != null) {
                media.setAltText(normalizeAltText(altText));
            }

            if (mediaAssetMapper.updateById(media) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "素材已被其他操作修改，请刷新后重试");
            }
            operationLogService.record(
                    principal.id(),
                    "UPDATE",
                    "MEDIA",
                    media.getId(),
                    "修改素材 " + media.getOriginalFilename()
            );
            if (replacement != null) {
                finishReplacementAfterTransaction(replacement.target(), oldTarget);
            }
            return toResponse(media);
        } catch (RuntimeException exception) {
            if (replacement != null) {
                deleteQuietly(replacement.target());
            }
            throw exception;
        }
    }

    @Transactional
    public void delete(Long id, AdminPrincipal principal) {
        MediaAsset media = getAsset(id);
        long references = mediaAssetMapper.countActiveReferences(id);
        if (references > 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "该素材正在被 " + references + " 处内容使用，请先在对应页面中更换或移除图片"
            );
        }

        Path target = resolveStoragePath(media.getStorageKey());
        if (mediaAssetMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "素材已被删除，请刷新列表");
        }
        operationLogService.record(
                principal.id(),
                "DELETE",
                "MEDIA",
                media.getId(),
                "删除素材 " + media.getOriginalFilename()
        );
        deleteFileAfterCommit(target);
    }

    public Page<AdminMediaResponse> list(String keyword, long page, long size) {
        Page<MediaAsset> assets = mediaAssetMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<MediaAsset>()
                        .and(
                                keyword != null && !keyword.isBlank(),
                                query -> query
                                        .like(MediaAsset::getOriginalFilename, keyword.trim())
                                        .or()
                                        .like(MediaAsset::getAltText, keyword.trim())
                        )
                        .orderByDesc(MediaAsset::getCreatedAt)
        );
        Page<AdminMediaResponse> result =
                new Page<>(assets.getCurrent(), assets.getSize(), assets.getTotal());
        result.setRecords(assets.getRecords().stream().map(this::toResponse).toList());
        return result;
    }

    public MediaAsset getAsset(Long id) {
        MediaAsset media = mediaAssetMapper.selectById(id);
        if (media == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "素材不存在");
        }
        return media;
    }

    private StoredFile store(MultipartFile file) {
        validateUpload(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String originalFilename = normalizeUploadFilename(file.getOriginalFilename());
        String storageKey =
                LocalDate.now() + "/" + UUID.randomUUID() + extension(originalFilename);
        Path target = resolveStoragePath(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(
                    target,
                    storageKey.replace('\\', '/'),
                    originalFilename,
                    contentType,
                    typeOf(contentType),
                    file.getSize(),
                    sha256(target)
            );
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "素材保存失败");
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单个文件不能超过 30MB");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "仅支持 JPG、PNG、WebP、GIF、MP4 和 PDF 文件"
            );
        }
    }

    private void applyStoredFile(MediaAsset media, StoredFile stored) {
        media.setMediaType(stored.mediaType());
        media.setStorageKey(stored.storageKey());
        media.setMimeType(stored.contentType());
        media.setFileSize(stored.size());
        media.setSha256(stored.sha256());
    }

    private Path resolveStoragePath(String storageKey) {
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的文件路径");
        }
        return target;
    }

    private void deleteCreatedFileIfTransactionRollsBack(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            deleteQuietly(target);
                        }
                    }
                }
        );
    }

    private void finishReplacementAfterTransaction(Path replacement, Path oldTarget) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(oldTarget);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            deleteQuietly(oldTarget);
                        } else {
                            deleteQuietly(replacement);
                        }
                    }
                }
        );
    }

    private void deleteFileAfterCommit(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(target);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteQuietly(target);
                    }
                }
        );
    }

    private AdminMediaResponse toResponse(MediaAsset media) {
        String cacheVersion = media.getSha256() == null || media.getSha256().isBlank()
                ? String.valueOf(media.getId())
                : media.getSha256().substring(0, Math.min(16, media.getSha256().length()));
        return new AdminMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getOriginalFilename(),
                media.getMimeType(),
                media.getFileSize(),
                media.getWidth(),
                media.getHeight(),
                media.getAltText(),
                "/admin/media/" + media.getId() + "/file",
                "/admin/media/" + media.getId() + "/thumbnail?v=" + cacheVersion,
                media.getCreatedAt()
        );
    }

    private static String typeOf(String type) {
        return type.startsWith("image/")
                ? "IMAGE"
                : type.startsWith("video/") ? "VIDEO" : "DOCUMENT";
    }

    private static String normalizeUploadFilename(String name) {
        String normalized = filenameOnly(name);
        if (normalized.isBlank()) {
            normalized = "upload";
        }
        validateFilenameLength(normalized);
        return normalized;
    }

    private static String normalizeDisplayFilename(String name) {
        String normalized = filenameOnly(name);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材名称不能为空");
        }
        validateFilenameLength(normalized);
        return normalized;
    }

    private static String filenameOnly(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized.replaceAll("[\\r\\n\\u0000]", "_").trim();
    }

    private static void validateFilenameLength(String filename) {
        if (filename.length() > MAX_FILENAME_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材名称不能超过 255 个字符");
        }
    }

    private static String normalizeAltText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_ALT_TEXT_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材说明不能超过 255 个字符");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) > 0; ) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法校验上传文件");
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 数据库状态优先；清理失败时保留孤立文件，避免回滚已提交的业务操作。
        }
    }

    private record StoredFile(
            Path target,
            String storageKey,
            String originalFilename,
            String contentType,
            String mediaType,
            long size,
            String sha256
    ) {
    }
}

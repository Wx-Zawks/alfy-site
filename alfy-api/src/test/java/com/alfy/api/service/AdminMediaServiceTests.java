package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.MediaAssetMapper;
import com.alfy.api.security.AdminPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminMediaServiceTests {

    private static final AdminPrincipal PRINCIPAL =
            new AdminPrincipal(1L, "admin", "content_admin");

    private final MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
    private final AdminOperationLogService operationLogService =
            mock(AdminOperationLogService.class);
    private final AdminMediaService service =
            new AdminMediaService(mediaAssetMapper, operationLogService);

    @TempDir
    Path storageRoot;

    @BeforeEach
    void configureStorageRoot() {
        ReflectionTestUtils.setField(service, "storageRoot", storageRoot.toString());
    }

    @Test
    void updatesFilenameAndAltTextWithoutReplacingFile() {
        MediaAsset media = image(10L, "2026-07-29/original.jpg");
        when(mediaAssetMapper.selectById(10L)).thenReturn(media);
        when(mediaAssetMapper.updateById(any(MediaAsset.class))).thenReturn(1);

        var response = service.update(
                10L,
                null,
                "renamed.jpg",
                "  新的图片说明  ",
                PRINCIPAL
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.originalFilename()).isEqualTo("renamed.jpg");
        assertThat(response.altText()).isEqualTo("新的图片说明");
        assertThat(response.thumbnailUrl()).isEqualTo(
                "/admin/media/10/thumbnail?v=hash");
        assertThat(media.getStorageKey()).isEqualTo("2026-07-29/original.jpg");
        verify(operationLogService)
                .record(1L, "UPDATE", "MEDIA", 10L, "修改素材 renamed.jpg");
    }

    @Test
    void replacesFileWhileKeepingMediaIdAndRemovesOldFile() throws Exception {
        Path oldFile = storageRoot.resolve("2026-07-29/original.jpg");
        Files.createDirectories(oldFile.getParent());
        Files.writeString(oldFile, "old");
        MediaAsset media = image(11L, "2026-07-29/original.jpg");
        when(mediaAssetMapper.selectById(11L)).thenReturn(media);
        when(mediaAssetMapper.updateById(any(MediaAsset.class))).thenReturn(1);
        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "replacement.png",
                "image/png",
                "new image".getBytes()
        );

        var response = service.update(11L, replacement, null, "", PRINCIPAL);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.originalFilename()).isEqualTo("replacement.png");
        assertThat(response.mimeType()).isEqualTo("image/png");
        assertThat(oldFile).doesNotExist();
        assertThat(storageRoot.resolve(media.getStorageKey()))
                .exists()
                .hasBinaryContent("new image".getBytes());
    }

    @Test
    void rejectsReplacementWithDifferentMediaType() {
        MediaAsset media = image(12L, "2026-07-29/original.jpg");
        when(mediaAssetMapper.selectById(12L)).thenReturn(media);
        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "pdf".getBytes()
        );

        assertThatThrownBy(
                () -> service.update(12L, replacement, null, null, PRINCIPAL)
        )
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.BAD_REQUEST)
                )
                .hasMessageContaining("相同类型");
        verify(mediaAssetMapper, never()).updateById(any(MediaAsset.class));
    }

    @Test
    void refusesToDeleteReferencedMedia() {
        MediaAsset media = image(13L, "2026-07-29/referenced.jpg");
        when(mediaAssetMapper.selectById(13L)).thenReturn(media);
        when(mediaAssetMapper.countActiveReferences(13L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(13L, PRINCIPAL))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT)
                )
                .hasMessageContaining("2 处内容使用");
        verify(mediaAssetMapper, never()).deleteById(13L);
    }

    @Test
    void deletesUnreferencedMediaAndStoredFile() throws Exception {
        Path storedFile = storageRoot.resolve("2026-07-29/unused.jpg");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "unused");
        MediaAsset media = image(14L, "2026-07-29/unused.jpg");
        when(mediaAssetMapper.selectById(14L)).thenReturn(media);
        when(mediaAssetMapper.countActiveReferences(14L)).thenReturn(0L);
        when(mediaAssetMapper.deleteById(14L)).thenReturn(1);

        service.delete(14L, PRINCIPAL);

        assertThat(storedFile).doesNotExist();
        verify(operationLogService)
                .record(1L, "DELETE", "MEDIA", 14L, "删除素材 image.jpg");
    }

    private MediaAsset image(Long id, String storageKey) {
        MediaAsset media = new MediaAsset();
        media.setId(id);
        media.setMediaType("IMAGE");
        media.setStorageKey(storageKey);
        media.setOriginalFilename("image.jpg");
        media.setMimeType("image/jpeg");
        media.setFileSize(3L);
        media.setSha256("hash");
        return media;
    }
}

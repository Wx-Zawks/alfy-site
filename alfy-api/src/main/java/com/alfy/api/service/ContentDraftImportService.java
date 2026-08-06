package com.alfy.api.service;

import com.alfy.api.entity.Article;
import com.alfy.api.entity.ArticleCategory;
import com.alfy.api.entity.ArticleCategoryRelation;
import com.alfy.api.entity.ArticleMedia;
import com.alfy.api.entity.ContentImportRecord;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.mapper.ArticleCategoryMapper;
import com.alfy.api.mapper.ArticleCategoryRelationMapper;
import com.alfy.api.mapper.ArticleMapper;
import com.alfy.api.mapper.ArticleMediaMapper;
import com.alfy.api.mapper.ContentImportRecordMapper;
import com.alfy.api.mapper.MediaAssetMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 将已审核的 DOCX 草稿包写入内容表。
 *
 * <p>该服务只接受 {@code build_content_draft.py} 生成的清单，所有文章均以草稿状态导入。
 * 以源 DOCX 哈希建立导入记录，因此重复执行不会重复创建文章。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDraftImportService {

    private final ObjectMapper objectMapper;
    private final ArticleMapper articleMapper;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleCategoryRelationMapper articleCategoryRelationMapper;
    private final ArticleMediaMapper articleMediaMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final ContentImportRecordMapper contentImportRecordMapper;

    @Value("${alfy.content-import.package-path:../docs/content-import-draft}")
    private String packagePath;

    @Value("${alfy.content-import.source-root:../docs/奥飞网站}")
    private String sourceRoot;

    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}")
    private String storageRoot;

    @Transactional(rollbackFor = Exception.class)
    public ImportSummary importDraftPackage() throws IOException {
        Path packageRoot = Path.of(packagePath).toAbsolutePath().normalize();
        Path sourceContentRoot = Path.of(sourceRoot).toAbsolutePath().normalize();
        Path localStorageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        JsonNode manifest = objectMapper.readTree(Files.readString(packageRoot.resolve("manifest.json")));
        Map<String, Long> categoryIds = loadCategoryIds();
        Map<String, MediaAsset> mediaByHash = new HashMap<>();

        for (JsonNode asset : manifest.path("assets")) {
            Path source = safeResolve(packageRoot, asset.path("storage_key").asText());
            MediaAsset media = importMedia(asset, source, localStorageRoot);
            mediaByHash.put(media.getSha256(), media);
        }
        Map<String, MediaAsset> videosByFilename = new HashMap<>();
        for (JsonNode asset : manifest.path("video_assets")) {
            Path source = safeResolve(sourceContentRoot, asset.path("source_file").asText());
            MediaAsset media = importMedia(asset, source, localStorageRoot);
            mediaByHash.put(media.getSha256(), media);
            videosByFilename.put(asset.path("original_filename").asText(), media);
        }

        int imported = 0;
        int skipped = 0;
        for (JsonNode draft : manifest.path("articles")) {
            String sourceHash = draft.path("docx_sha256").asText();
            if (hasImportedSource(sourceHash)) {
                skipped++;
                continue;
            }
            importArticle(draft, categoryIds, mediaByHash, videosByFilename);
            imported++;
        }
        log.info("内容草稿导入完成：新增文章 {} 篇，跳过已导入文章 {} 篇，媒体资源 {} 个", imported, skipped, mediaByHash.size());
        return new ImportSummary(imported, skipped, mediaByHash.size());
    }

    private Map<String, Long> loadCategoryIds() {
        Map<String, Long> result = new HashMap<>();
        for (ArticleCategory category : articleCategoryMapper.selectList(null)) {
            result.put(category.getCode(), category.getId());
        }
        return result;
    }

    private boolean hasImportedSource(String sourceHash) {
        return contentImportRecordMapper.selectCount(
                new LambdaQueryWrapper<ContentImportRecord>().eq(ContentImportRecord::getSourceHash, sourceHash)
        ) > 0;
    }

    private MediaAsset importMedia(JsonNode asset, Path source, Path localStorageRoot) throws IOException {
        String sha256 = asset.path("sha256").asText();
        MediaAsset existing = mediaAssetMapper.selectOne(
                new LambdaQueryWrapper<MediaAsset>().eq(MediaAsset::getSha256, sha256)
        );
        if (existing != null) {
            return existing;
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("媒体源文件不存在：" + source);
        }
        String storageKey = asset.path("storage_key").asText();
        Path target = safeResolve(localStorageRoot, storageKey);
        Files.createDirectories(target.getParent());
        if (!Files.exists(target)) {
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
        MediaAsset media = new MediaAsset();
        media.setMediaType(asset.path("media_type").asText());
        media.setStorageKey(storageKey);
        media.setOriginalFilename(asset.path("original_filename").asText());
        media.setMimeType(asset.path("mime_type").asText(null));
        media.setFileSize(asset.path("file_size").asLong());
        media.setSha256(sha256);
        mediaAssetMapper.insert(media);
        return media;
    }

    private void importArticle(
            JsonNode draft,
            Map<String, Long> categoryIds,
            Map<String, MediaAsset> mediaByHash,
            Map<String, MediaAsset> videosByFilename
    ) {
        String primaryCategoryCode = draft.path("primary_category_code").asText();
        Long primaryCategoryId = categoryIds.get(primaryCategoryCode);
        if (primaryCategoryId == null) {
            throw new IllegalStateException("找不到文章主分类：" + primaryCategoryCode);
        }
        Article article = new Article();
        article.setCategoryId(primaryCategoryId);
        article.setTitle(draft.path("title").asText());
        article.setSummary(summaryOf(draft.path("content_text").asText()));
        article.setContentHtml(draft.path("content_html").asText());
        article.setContentText(draft.path("content_text").asText());
        article.setSourceUrl(draft.path("source_url").asText(null));
        article.setSourceFile(draft.path("source_file").asText());
        article.setContentHash(draft.path("content_hash").asText());
        article.setSourcePublishedAt(parseOffsetDateTime(draft.path("source_published_at").asText(null)));
        article.setStatus("DRAFT");
        articleMapper.insert(article);

        for (JsonNode categoryCode : draft.path("category_codes")) {
            Long categoryId = categoryIds.get(categoryCode.asText());
            if (categoryId == null) {
                throw new IllegalStateException("找不到文章分类：" + categoryCode.asText());
            }
            ArticleCategoryRelation relation = new ArticleCategoryRelation();
            relation.setArticleId(article.getId());
            relation.setCategoryId(categoryId);
            relation.setSortOrder(0);
            articleCategoryRelationMapper.insert(relation);
        }
        Set<Long> attachedImageIds = new HashSet<>();
        for (JsonNode media : draft.path("media")) {
            MediaAsset asset = mediaByHash.get(media.path("sha256").asText());
            if (asset == null) {
                throw new IllegalStateException("找不到文章图片资源：" + media.path("sha256").asText());
            }
            // 正文 HTML 可多次引用同一去重图片；关联表只保留一次资源归属，避免触发唯一约束。
            if (attachedImageIds.add(asset.getId())) {
                createArticleMedia(article.getId(), asset.getId(), "INLINE", media.path("sort_order").asInt());
            }
        }
        for (JsonNode filename : draft.path("video_filenames")) {
            MediaAsset video = videosByFilename.get(filename.asText());
            if (video == null) {
                throw new IllegalStateException("找不到文章视频资源：" + filename.asText());
            }
            createArticleMedia(article.getId(), video.getId(), "VIDEO", 0);
        }
        for (JsonNode source : draft.path("source_records")) {
            ContentImportRecord record = new ContentImportRecord();
            record.setArticleId(article.getId());
            record.setCategoryCode(source.path("category_code").asText());
            record.setSourceFile(source.path("source_file").asText());
            record.setSourceUrl(source.path("source_url").asText(null));
            record.setSourceHash(source.path("docx_sha256").asText());
            record.setContentHash(source.path("content_sha256").asText());
            record.setImportStatus("IMPORTED");
            record.setImportedAt(LocalDateTime.now());
            contentImportRecordMapper.insert(record);
        }
    }

    private void createArticleMedia(Long articleId, Long mediaId, String usageType, int sortOrder) {
        ArticleMedia relation = new ArticleMedia();
        relation.setArticleId(articleId);
        relation.setMediaId(mediaId);
        relation.setUsageType(usageType);
        relation.setSortOrder(sortOrder);
        articleMediaMapper.insert(relation);
    }

    private static Path safeResolve(Path root, String relativePath) throws IOException {
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IOException("拒绝访问存储根目录外的路径：" + relativePath);
        }
        return candidate;
    }

    private static LocalDateTime parseOffsetDateTime(String value) {
        return value == null || value.isBlank() ? null : OffsetDateTime.parse(value).toLocalDateTime();
    }

    private static String summaryOf(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }

    public record ImportSummary(int importedArticles, int skippedArticles, int mediaAssets) {
    }
}

package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminArticleListItemResponse;
import com.alfy.api.dto.AdminArticleResponse;
import com.alfy.api.dto.AdminArticleUpsertRequest;
import com.alfy.api.dto.ArticleCategoryResponse;
import com.alfy.api.entity.Article;
import com.alfy.api.entity.ArticleCategory;
import com.alfy.api.entity.ArticleCategoryRelation;
import com.alfy.api.entity.ArticleMedia;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ArticleCategoryMapper;
import com.alfy.api.mapper.ArticleCategoryRelationMapper;
import com.alfy.api.mapper.ArticleMapper;
import com.alfy.api.mapper.ArticleMediaMapper;
import com.alfy.api.mapper.MediaAssetMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 后台文章编辑、发布、下线服务。 */
@Service
@RequiredArgsConstructor
public class AdminArticleService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Set<String> HOME_SLOTS = Set.of("NEWS_PRIMARY", "NEWS_SECONDARY", "NEWS_LIST");

    private final ArticleMapper articleMapper;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleCategoryRelationMapper articleCategoryRelationMapper;
    private final ArticleMediaMapper articleMediaMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogService operationLogService;
    private final HtmlSanitizer htmlSanitizer;

    public Page<AdminArticleListItemResponse> list(String status, Long categoryId, String keyword, long page, long size) {
        Set<Long> articleIds = findArticleIds(categoryId);
        if (categoryId != null && articleIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Article>()
                .eq(status != null && !status.isBlank(), Article::getStatus, status)
                .in(categoryId != null, Article::getId, articleIds)
                .like(keyword != null && !keyword.isBlank(), Article::getTitle, keyword)
                .orderByDesc(Article::getUpdatedAt)
                .orderByDesc(Article::getId));
        Map<Long, List<ArticleCategoryResponse>> categories = categoriesByArticle(result.getRecords());
        List<AdminArticleListItemResponse> records = result.getRecords().stream()
                .map(article -> new AdminArticleListItemResponse(article.getId(), article.getTitle(), article.getSlug(),
                        article.getStatus(), article.getIsFeatured() != null && article.getIsFeatured() == 1,
                        article.getHomeSlot(), article.getPublishedAt(), article.getUpdatedAt(), article.getVersion(),
                        categories.getOrDefault(article.getId(), List.of())))
                .toList();
        Page<AdminArticleListItemResponse> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(records);
        return response;
    }

    public AdminArticleResponse get(Long id) {
        return toResponse(requireArticle(id));
    }

    @Transactional
    public AdminArticleResponse create(AdminArticleUpsertRequest request, AdminPrincipal principal) {
        validateRequest(request, null);
        Article article = new Article();
        applyRequest(article, request);
        article.setStatus("DRAFT");
        article.setPublishedAt(null);
        articleMapper.insert(article);
        replaceCategories(article.getId(), request.categoryIds());
        replaceInlineMedia(article.getId(), article.getContentHtml());
        operationLogService.record(principal.id(), "CREATE", "ARTICLE", article.getId(), "创建文章草稿");
        return toResponse(requireArticle(article.getId()));
    }

    @Transactional
    public AdminArticleResponse update(Long id, AdminArticleUpsertRequest request, AdminPrincipal principal) {
        Article article = requireArticle(id);
        if (request.version() == null || !request.version().equals(article.getVersion())) {
            throw new BusinessException(ErrorCode.CONFLICT, "文章已被其他管理员修改，请刷新后重试");
        }
        validateRequest(request, id);
        applyRequest(article, request);
        int updated = articleMapper.updateById(article);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "文章已被其他管理员修改，请刷新后重试");
        }
        replaceCategories(id, request.categoryIds());
        replaceInlineMedia(id, article.getContentHtml());
        operationLogService.record(principal.id(), "UPDATE", "ARTICLE", id, "更新文章内容");
        return toResponse(requireArticle(id));
    }

    @Transactional
    public AdminArticleResponse publish(Long id, AdminPrincipal principal) {
        Article article = requireArticle(id);
        if (article.getTitle() == null || article.getTitle().isBlank() || article.getSlug() == null || article.getSlug().isBlank()
                || article.getContentHtml() == null || article.getContentHtml().isBlank()
                || findArticleIdsForArticle(id).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题、slug、正文并关联至少一个分类");
        }
        article.setStatus("PUBLISHED");
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(article);
        operationLogService.record(principal.id(), "PUBLISH", "ARTICLE", id, "发布文章");
        return toResponse(requireArticle(id));
    }

    @Transactional
    public AdminArticleResponse offline(Long id, AdminPrincipal principal) {
        Article article = requireArticle(id);
        article.setStatus("OFFLINE");
        articleMapper.updateById(article);
        operationLogService.record(principal.id(), "OFFLINE", "ARTICLE", id, "下线文章");
        return toResponse(requireArticle(id));
    }

    @Transactional
    public void delete(Long id, AdminPrincipal principal) {
        requireArticle(id);
        articleMapper.deleteById(id);
        operationLogService.record(principal.id(), "DELETE", "ARTICLE", id, "软删除文章");
    }

    private void validateRequest(AdminArticleUpsertRequest request, Long currentId) {
        String slug = request.slug().trim();
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "slug 仅支持小写字母、数字和单个连字符");
        }
        if (request.homeSlot() != null && !request.homeSlot().isBlank() && !HOME_SLOTS.contains(request.homeSlot())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "首页展示位不合法");
        }
        Article sameSlug = articleMapper.selectOne(new LambdaQueryWrapper<Article>().eq(Article::getSlug, slug));
        if (sameSlug != null && !sameSlug.getId().equals(currentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "slug 已被其他文章使用");
        }
        Set<Long> categoryIds = new LinkedHashSet<>(request.categoryIds());
        if (categoryIds.size() != request.categoryIds().size()
                || articleCategoryMapper.selectCount(new LambdaQueryWrapper<ArticleCategory>()
                .in(ArticleCategory::getId, categoryIds).eq(ArticleCategory::getStatus, 1)) != categoryIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章分类不存在或已停用");
        }
    }

    private void applyRequest(Article article, AdminArticleUpsertRequest request) {
        article.setCategoryId(request.categoryIds().get(0));
        article.setTitle(request.title().trim());
        article.setSlug(request.slug().trim());
        article.setSummary(request.summary());
        String contentHtml = htmlSanitizer.clean(request.contentHtml());
        article.setContentHtml(contentHtml);
        article.setContentText(request.contentText() == null ? htmlSanitizer.toPlainText(contentHtml) : request.contentText());
        article.setCoverMediaId(request.coverMediaId());
        article.setAuthorName(request.authorName());
        article.setSourceUrl(request.sourceUrl());
        article.setSourcePublishedAt(request.sourcePublishedAt());
        article.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        article.setIsFeatured(Boolean.TRUE.equals(request.featured()) ? 1 : 0);
        article.setHomeSlot(request.homeSlot() == null || request.homeSlot().isBlank() ? null : request.homeSlot());
        article.setHomeSortOrder(request.homeSortOrder() == null ? 0 : request.homeSortOrder());
        article.setSeoTitle(request.seoTitle());
        article.setSeoDescription(request.seoDescription());
        article.setSeoKeywords(request.seoKeywords());
    }

    private void replaceCategories(Long articleId, List<Long> categoryIds) {
        articleCategoryRelationMapper.deleteByArticleId(articleId);
        for (Long categoryId : new LinkedHashSet<>(categoryIds)) {
            ArticleCategoryRelation relation = new ArticleCategoryRelation();
            relation.setArticleId(articleId);
            relation.setCategoryId(categoryId);
            relation.setSortOrder(0);
            articleCategoryRelationMapper.insert(relation);
        }
    }

    private void replaceInlineMedia(Long articleId, String contentHtml) {
        LinkedHashSet<Long> mediaIds = extractInlineMediaIds(contentHtml);
        if (!mediaIds.isEmpty()) {
            Map<Long, MediaAsset> mediaById = mediaAssetMapper.selectBatchIds(mediaIds).stream()
                    .collect(Collectors.toMap(MediaAsset::getId, media -> media));
            boolean containsInvalidMedia = mediaById.size() != mediaIds.size()
                    || mediaById.values().stream().anyMatch(media -> !"IMAGE".equals(media.getMediaType()));
            if (containsInvalidMedia) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "正文中包含不存在或非图片类型的素材");
            }
        }

        articleMediaMapper.deleteInlineByArticleId(articleId);
        int sortOrder = 0;
        for (Long mediaId : mediaIds) {
            ArticleMedia relation = new ArticleMedia();
            relation.setArticleId(articleId);
            relation.setMediaId(mediaId);
            relation.setUsageType("INLINE");
            relation.setSortOrder(sortOrder++);
            articleMediaMapper.insert(relation);
        }
    }

    private LinkedHashSet<Long> extractInlineMediaIds(String contentHtml) {
        LinkedHashSet<Long> mediaIds = new LinkedHashSet<>();
        if (contentHtml == null || contentHtml.isBlank()) {
            return mediaIds;
        }
        for (Element image : Jsoup.parseBodyFragment(contentHtml).select("img[src]")) {
            String source = image.attr("src").trim();
            if (!source.regionMatches(true, 0, "alfy-media:", 0, "alfy-media:".length())) {
                continue;
            }
            String rawId = source.substring("alfy-media:".length());
            if (!rawId.matches("[1-9]\\d*")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "正文图片素材标识不合法");
            }
            try {
                mediaIds.add(Long.parseLong(rawId));
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "正文图片素材标识不合法");
            }
        }
        return mediaIds;
    }

    private Article requireArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        return article;
    }

    private Set<Long> findArticleIds(Long categoryId) {
        if (categoryId == null) {
            return Set.of();
        }
        return articleCategoryRelationMapper.selectList(new LambdaQueryWrapper<ArticleCategoryRelation>()
                        .eq(ArticleCategoryRelation::getCategoryId, categoryId))
                .stream().map(ArticleCategoryRelation::getArticleId).collect(Collectors.toSet());
    }

    private Set<Long> findArticleIdsForArticle(Long articleId) {
        return articleCategoryRelationMapper.selectList(new LambdaQueryWrapper<ArticleCategoryRelation>()
                        .eq(ArticleCategoryRelation::getArticleId, articleId))
                .stream().map(ArticleCategoryRelation::getArticleId).collect(Collectors.toSet());
    }

    private AdminArticleResponse toResponse(Article article) {
        return new AdminArticleResponse(article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                toEditableContentHtml(article), article.getContentText(), article.getCoverMediaId(), article.getAuthorName(),
                article.getSourceUrl(), article.getSourcePublishedAt(), article.getPublishedAt(), article.getStatus(),
                article.getSortOrder(), article.getIsFeatured() != null && article.getIsFeatured() == 1, article.getHomeSlot(),
                article.getHomeSortOrder(), article.getSeoTitle(), article.getSeoDescription(), article.getSeoKeywords(),
                article.getVersion(), categoriesByArticle(List.of(article)).getOrDefault(article.getId(), List.of()));
    }

    private String toEditableContentHtml(Article article) {
        String contentHtml = article.getContentHtml();
        if (contentHtml == null || contentHtml.isBlank()) {
            return contentHtml;
        }
        List<ArticleMedia> relations = articleMediaMapper.selectList(new LambdaQueryWrapper<ArticleMedia>()
                .eq(ArticleMedia::getArticleId, article.getId())
                .eq(ArticleMedia::getUsageType, "INLINE"));
        if (relations.isEmpty()) {
            return contentHtml;
        }
        Map<Long, MediaAsset> mediaById = mediaAssetMapper.selectBatchIds(
                        relations.stream().map(ArticleMedia::getMediaId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, media -> media));
        String editableHtml = contentHtml;
        for (ArticleMedia relation : relations) {
            MediaAsset media = mediaById.get(relation.getMediaId());
            if (media == null || media.getStorageKey() == null || media.getStorageKey().isBlank()) {
                continue;
            }
            String placeholder = "alfy-media:" + media.getId();
            editableHtml = editableHtml
                    .replace("src=\"" + media.getStorageKey() + "\"", "src=\"" + placeholder + "\"")
                    .replace("src='" + media.getStorageKey() + "'", "src='" + placeholder + "'");
        }
        return editableHtml;
    }

    private Map<Long, List<ArticleCategoryResponse>> categoriesByArticle(Collection<Article> articles) {
        if (articles.isEmpty()) {
            return Map.of();
        }
        Set<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toSet());
        List<ArticleCategoryRelation> relations = articleCategoryRelationMapper.selectList(
                new LambdaQueryWrapper<ArticleCategoryRelation>().in(ArticleCategoryRelation::getArticleId, articleIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, ArticleCategory> categories = articleCategoryMapper.selectBatchIds(
                        relations.stream().map(ArticleCategoryRelation::getCategoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ArticleCategory::getId, item -> item));
        Map<Long, List<ArticleCategoryResponse>> result = new HashMap<>();
        for (ArticleCategoryRelation relation : relations) {
            ArticleCategory category = categories.get(relation.getCategoryId());
            if (category != null) {
                result.computeIfAbsent(relation.getArticleId(), ignored -> new java.util.ArrayList<>())
                        .add(new ArticleCategoryResponse(category.getId(), category.getCode(), category.getName(),
                                category.getSortOrder(), List.of()));
            }
        }
        result.values().forEach(items -> items.sort(Comparator.comparingInt(ArticleCategoryResponse::sortOrder)));
        return result;
    }

}

package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.ArticleCategoryResponse;
import com.alfy.api.dto.ArticleDetailResponse;
import com.alfy.api.dto.ArticleListItemResponse;
import com.alfy.api.dto.ArticleMediaResponse;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 前台文章读取服务。
 *
 * <p>所有查询都固定限制为 PUBLISHED，导入的草稿不会通过前台接口或媒体接口泄露。</p>
 */
@Service
@RequiredArgsConstructor
public class PublicArticleService {

    private static final String PUBLISHED = "PUBLISHED";

    private final ArticleMapper articleMapper;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleCategoryRelationMapper articleCategoryRelationMapper;
    private final ArticleMediaMapper articleMediaMapper;
    private final MediaAssetMapper mediaAssetMapper;

    public List<ArticleCategoryResponse> listCategories() {
        List<ArticleCategory> categories = articleCategoryMapper.selectList(
                new LambdaQueryWrapper<ArticleCategory>()
                        .eq(ArticleCategory::getStatus, 1)
                        .orderByAsc(ArticleCategory::getSortOrder)
                        .orderByAsc(ArticleCategory::getId)
        );
        return toCategoryTree(categories);
    }

    public Page<ArticleListItemResponse> listArticles(String categoryCode, long pageNumber, long pageSize) {
        Set<Long> categoryIds = categoryCode == null || categoryCode.isBlank()
                ? Set.of()
                : categoryAndDescendantIds(categoryCode);

        Set<Long> articleIds = categoryIds.isEmpty() && categoryCode != null && !categoryCode.isBlank()
                ? Set.of()
                : findArticleIdsByCategoryIds(categoryIds);
        if (categoryCode != null && !categoryCode.isBlank() && articleIds.isEmpty()) {
            return new Page<>(pageNumber, pageSize, 0);
        }

        Page<Article> articlePage = articleMapper.selectPage(
                new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, PUBLISHED)
                        .in(!articleIds.isEmpty(), Article::getId, articleIds)
                        .orderByDesc(Article::getPublishedAt)
                        .orderByDesc(Article::getId)
        );
        Map<Long, List<ArticleCategoryResponse>> categoriesByArticle = categoriesByArticle(articlePage.getRecords());
        Map<Long, String> coverUrls = coverUrls(articlePage.getRecords());
        List<ArticleListItemResponse> records = articlePage.getRecords().stream()
                .map(article -> new ArticleListItemResponse(
                        article.getId(), article.getTitle(), article.getSummary(), coverUrls.get(article.getId()),
                        article.getSourcePublishedAt(), article.getPublishedAt(),
                        categoriesByArticle.getOrDefault(article.getId(), List.of())
                ))
                .toList();
        Page<ArticleListItemResponse> responsePage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        responsePage.setRecords(records);
        return responsePage;
    }

    public ArticleDetailResponse getArticle(Long articleId) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getId, articleId)
                .eq(Article::getStatus, PUBLISHED));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在或尚未发布");
        }
        List<ArticleMediaResponse> media = mediaByArticle(article.getId());
        Map<String, String> urlByStorageKey = media.stream().collect(Collectors.toMap(
                mediaItem -> mediaStorageKey(mediaItem.id()),
                ArticleMediaResponse::url,
                (first, ignored) -> first
        ));
        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getSummary(),
                replaceInlineMediaUrls(article.getContentHtml(), urlByStorageKey), article.getSourceUrl(),
                article.getSourcePublishedAt(), article.getPublishedAt(),
                categoriesByArticle(List.of(article)).getOrDefault(article.getId(), List.of()), media
        );
    }

    /** 仅在媒体确实属于至少一篇已发布文章时返回，供 Controller 下载文件。 */
    public MediaAsset getPublicMedia(Long mediaId) {
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null || !belongsToPublishedArticle(mediaId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "媒体资源不存在或尚未公开");
        }
        return media;
    }

    private boolean belongsToPublishedArticle(Long mediaId) {
        List<ArticleMedia> relations = articleMediaMapper.selectList(
                new LambdaQueryWrapper<ArticleMedia>().eq(ArticleMedia::getMediaId, mediaId));
        if (relations.isEmpty()) {
            return false;
        }
        Set<Long> articleIds = relations.stream().map(ArticleMedia::getArticleId).collect(Collectors.toSet());
        return articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .in(Article::getId, articleIds)
                .eq(Article::getStatus, PUBLISHED)) > 0;
    }

    private Set<Long> categoryAndDescendantIds(String categoryCode) {
        List<ArticleCategory> allCategories = articleCategoryMapper.selectList(
                new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getStatus, 1));
        ArticleCategory target = allCategories.stream()
                .filter(category -> categoryCode.equals(category.getCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章分类不存在或已停用"));
        Map<Long, List<Long>> childrenByParent = allCategories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(ArticleCategory::getParentId,
                        Collectors.mapping(ArticleCategory::getId, Collectors.toList())));
        Set<Long> ids = new HashSet<>();
        collectCategoryAndChildren(target.getId(), childrenByParent, ids);
        return ids;
    }

    private void collectCategoryAndChildren(Long categoryId, Map<Long, List<Long>> childrenByParent, Set<Long> ids) {
        if (!ids.add(categoryId)) {
            return;
        }
        for (Long childId : childrenByParent.getOrDefault(categoryId, List.of())) {
            collectCategoryAndChildren(childId, childrenByParent, ids);
        }
    }

    private Set<Long> findArticleIdsByCategoryIds(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Set.of();
        }
        return articleCategoryRelationMapper.selectList(new LambdaQueryWrapper<ArticleCategoryRelation>()
                        .in(ArticleCategoryRelation::getCategoryId, categoryIds))
                .stream()
                .map(ArticleCategoryRelation::getArticleId)
                .collect(Collectors.toSet());
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
        Set<Long> categoryIds = relations.stream().map(ArticleCategoryRelation::getCategoryId).collect(Collectors.toSet());
        Map<Long, ArticleCategory> categoryById = articleCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ArticleCategory::getId, category -> category));
        Map<Long, List<ArticleCategoryResponse>> result = new HashMap<>();
        for (ArticleCategoryRelation relation : relations) {
            ArticleCategory category = categoryById.get(relation.getCategoryId());
            if (category == null) {
                continue;
            }
            result.computeIfAbsent(relation.getArticleId(), ignored -> new ArrayList<>())
                    .add(toCategoryResponse(category));
        }
        result.values().forEach(items -> items.sort(Comparator.comparingInt(ArticleCategoryResponse::sortOrder)));
        return result;
    }

    private Map<Long, String> coverUrls(Collection<Article> articles) {
        Set<Long> coverMediaIds = articles.stream()
                .map(Article::getCoverMediaId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (coverMediaIds.isEmpty()) {
            return Map.of();
        }
        return articles.stream()
                .filter(article -> article.getCoverMediaId() != null && coverMediaIds.contains(article.getCoverMediaId()))
                .collect(Collectors.toMap(Article::getId,
                        article -> mediaUrl(article.getCoverMediaId())));
    }

    private List<ArticleMediaResponse> mediaByArticle(Long articleId) {
        List<ArticleMedia> relations = articleMediaMapper.selectList(new LambdaQueryWrapper<ArticleMedia>()
                .eq(ArticleMedia::getArticleId, articleId)
                .orderByAsc(ArticleMedia::getSortOrder)
                .orderByAsc(ArticleMedia::getId));
        if (relations.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaAsset> mediaById = mediaAssetMapper.selectBatchIds(
                        relations.stream().map(ArticleMedia::getMediaId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, media -> media));
        return relations.stream()
                .map(relation -> toMediaResponse(mediaById.get(relation.getMediaId()), relation))
                .filter(media -> media != null)
                .toList();
    }

    private List<ArticleCategoryResponse> toCategoryTree(List<ArticleCategory> categories) {
        Map<Long, List<ArticleCategory>> childrenByParent = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(ArticleCategory::getParentId));
        return categories.stream()
                .filter(category -> category.getParentId() == null)
                .sorted(Comparator.comparingInt(ArticleCategory::getSortOrder))
                .map(category -> toCategoryTreeNode(category, childrenByParent))
                .toList();
    }

    private ArticleCategoryResponse toCategoryTreeNode(
            ArticleCategory category, Map<Long, List<ArticleCategory>> childrenByParent) {
        List<ArticleCategoryResponse> children = childrenByParent.getOrDefault(category.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(ArticleCategory::getSortOrder))
                .map(child -> toCategoryTreeNode(child, childrenByParent))
                .toList();
        return new ArticleCategoryResponse(category.getId(), category.getCode(), category.getName(),
                category.getSortOrder(), children);
    }

    private ArticleCategoryResponse toCategoryResponse(ArticleCategory category) {
        return new ArticleCategoryResponse(category.getId(), category.getCode(), category.getName(),
                category.getSortOrder(), List.of());
    }

    private ArticleMediaResponse toMediaResponse(MediaAsset media, ArticleMedia relation) {
        if (media == null) {
            return null;
        }
        return new ArticleMediaResponse(media.getId(), media.getMediaType(), media.getOriginalFilename(),
                media.getMimeType(), relation.getUsageType(), mediaUrl(media.getId()), relation.getCaption(),
                relation.getSortOrder());
    }

    private String replaceInlineMediaUrls(String contentHtml, Map<String, String> urlByStorageKey) {
        if (contentHtml == null || contentHtml.isBlank() || urlByStorageKey.isEmpty()) {
            return contentHtml;
        }
        String result = contentHtml;
        for (Map.Entry<String, String> entry : urlByStorageKey.entrySet()) {
            result = result.replace("src=\"" + entry.getKey() + "\"", "src=\"" + entry.getValue() + "\"")
                    .replace("src='" + entry.getKey() + "'", "src='" + entry.getValue() + "'");
        }
        return result;
    }

    private String mediaStorageKey(Long mediaId) {
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        return media == null ? "" : media.getStorageKey();
    }

    private static String mediaUrl(Long mediaId) {
        return "/api/v1/public/media/" + mediaId;
    }
}

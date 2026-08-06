package com.alfy.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 前台已发布文章详情。 */
public record ArticleDetailResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String coverUrl,
        String contentHtml,
        String sourceUrl,
        LocalDateTime sourcePublishedAt,
        LocalDateTime publishedAt,
        List<ArticleCategoryResponse> categories,
        List<ArticleMediaResponse> media,
        SeoMetaResponse seo
) {
}

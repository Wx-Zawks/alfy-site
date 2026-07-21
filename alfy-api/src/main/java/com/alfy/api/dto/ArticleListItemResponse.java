package com.alfy.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 前台文章列表项；不包含正文，避免列表请求传输大量 HTML。 */
public record ArticleListItemResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String coverUrl,
        LocalDateTime sourcePublishedAt,
        LocalDateTime publishedAt,
        List<ArticleCategoryResponse> categories
) {
}

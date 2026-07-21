package com.alfy.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminArticleResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String contentHtml,
        String contentText,
        Long coverMediaId,
        String authorName,
        String sourceUrl,
        LocalDateTime sourcePublishedAt,
        LocalDateTime publishedAt,
        String status,
        Integer sortOrder,
        Boolean featured,
        String homeSlot,
        Integer homeSortOrder,
        String seoTitle,
        String seoDescription,
        String seoKeywords,
        Long version,
        List<ArticleCategoryResponse> categories
) {
}

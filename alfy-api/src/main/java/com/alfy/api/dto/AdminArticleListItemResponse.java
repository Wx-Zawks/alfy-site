package com.alfy.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminArticleListItemResponse(
        Long id,
        String title,
        String slug,
        String status,
        Boolean featured,
        String homeSlot,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        Long version,
        List<ArticleCategoryResponse> categories
) {
}

package com.alfy.api.dto;

public record PublicProductCategoryResponse(
        Long id,
        String name,
        String slug,
        String summary,
        String coverImageUrl,
        Integer sortOrder
) {
}

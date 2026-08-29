package com.alfy.api.dto;

public record PublicCaseCategoryResponse(
        Long id,
        String slug,
        String name,
        String summary,
        Integer sortOrder
) {
}

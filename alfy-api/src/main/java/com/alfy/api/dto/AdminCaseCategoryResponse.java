package com.alfy.api.dto;

public record AdminCaseCategoryResponse(
        Long id,
        String name,
        String slug,
        String summary,
        Integer sortOrder,
        boolean enabled,
        Long version
) {
}

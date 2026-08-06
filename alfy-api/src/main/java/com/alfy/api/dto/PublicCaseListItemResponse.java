package com.alfy.api.dto;

public record PublicCaseListItemResponse(
        Long id,
        String slug,
        String title,
        String scene,
        String sceneSlug,
        String location,
        String summary,
        String coverImageUrl,
        boolean featured,
        Integer sortOrder
) {
}

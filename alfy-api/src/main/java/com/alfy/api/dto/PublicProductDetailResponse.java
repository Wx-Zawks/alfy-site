package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record PublicProductDetailResponse(
        Long id,
        String name,
        String slug,
        String category,
        String categoryName,
        String summary,
        String contentHtml,
        String coverImageUrl,
        List<String> features,
        JsonNode specifications,
        SeoMetaResponse seo
) {
}

package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record TechnologyPageResponse(
        Long id, String pageKey, String eyebrow, String title, String highlightText, String summary,
        Long heroMediaId, String heroImageUrl,
        ActionResponse action, JsonNode capabilityRows, JsonNode pillars, String contentHtml,
        String seoTitle, String seoDescription, String seoKeywords, Integer sortOrder,
        String status, LocalDateTime publishedAt, LocalDateTime updatedAt, Long version
) { }

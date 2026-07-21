package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record TechnologyPageResponse(
        Long id, String eyebrow, String title, String highlightText, String summary,
        ActionResponse action, JsonNode capabilityRows, JsonNode pillars, String contentHtml,
        String status, LocalDateTime publishedAt, Long version
) { }

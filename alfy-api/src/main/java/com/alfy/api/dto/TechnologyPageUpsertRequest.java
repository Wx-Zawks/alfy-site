package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TechnologyPageUpsertRequest(
        @Size(max = 255) String eyebrow,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String highlightText,
        @Size(max = 1000) String summary,
        @Size(max = 100) String ctaLabel,
        @Size(max = 500) String ctaTarget,
        JsonNode capabilityRows,
        JsonNode pillars,
        String contentHtml,
        Long version
) { }

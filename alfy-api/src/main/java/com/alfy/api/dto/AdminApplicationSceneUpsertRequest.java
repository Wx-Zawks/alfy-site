package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminApplicationSceneUpsertRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String slug,
        @Size(max = 255) String slogan,
        @Size(max = 1000) String summary,
        String painPoint,
        String solution,
        String contentHtml,
        Long coverMediaId,
        Boolean featured,
        Integer sortOrder,
        @Size(max = 255) String seoTitle,
        @Size(max = 500) String seoDescription,
        @Size(max = 500) String seoKeywords,
        Long version
) { }

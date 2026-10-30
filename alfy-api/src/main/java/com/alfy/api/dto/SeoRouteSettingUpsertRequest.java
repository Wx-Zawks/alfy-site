package com.alfy.api.dto;

import jakarta.validation.constraints.Size;

public record SeoRouteSettingUpsertRequest(
        @Size(max = 255) String seoTitle,
        @Size(max = 500) String seoDescription,
        @Size(max = 500) String seoKeywords,
        Long ogMediaId,
        Boolean indexEnabled,
        Long version
) { }

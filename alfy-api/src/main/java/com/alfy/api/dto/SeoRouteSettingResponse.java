package com.alfy.api.dto;

import java.time.LocalDateTime;

public record SeoRouteSettingResponse(
        Long id,
        String routeKey,
        String routePath,
        String displayName,
        String seoTitle,
        String seoDescription,
        String seoKeywords,
        Long ogMediaId,
        String ogImageUrl,
        boolean indexEnabled,
        Long version,
        LocalDateTime updatedAt
) { }

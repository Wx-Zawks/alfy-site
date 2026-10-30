package com.alfy.api.dto;
import java.time.LocalDateTime;
public record RedirectRuleResponse(
        Long id,
        String sourceHost,
        String sourcePath,
        String targetUrl,
        int statusCode,
        boolean preserveQuery,
        String note,
        boolean enabled,
        long hitCount,
        LocalDateTime lastHitAt,
        Long version,
        LocalDateTime updatedAt
) { }

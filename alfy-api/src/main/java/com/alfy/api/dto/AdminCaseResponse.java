package com.alfy.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCaseResponse(
        Long id, Long sceneId, String title, String slug, String clientName, String location, String summary,
        String background, String customerNeed, String solution, String implementation, String resultSummary,
        String contentHtml, Long coverMediaId, boolean featured, Integer sortOrder, String status,
        String seoTitle, String seoDescription, String seoKeywords, LocalDateTime publishedAt,
        Long version, List<Long> productIds
) { }

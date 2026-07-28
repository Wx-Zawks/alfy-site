package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record ContentPageResponse(Long id, String pageKey, String title, String category, String summary,
        String contentHtml, JsonNode contentData, Long coverMediaId, String coverImageUrl, boolean featured,
        Integer sortOrder, String status, String seoTitle, String seoDescription, String seoKeywords,
        LocalDateTime publishedAt, Long version) { }

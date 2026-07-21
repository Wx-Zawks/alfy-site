package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

public record AdminProductResponse(Long id, Long categoryId, String name, String slug, String summary, String contentHtml,
                                   String contentText, Long coverMediaId, List<String> features, JsonNode specifications,
                                   boolean featured, Integer sortOrder, String status, String seoTitle, String seoDescription,
                                   String seoKeywords, LocalDateTime publishedAt, Long version, List<Long> sceneIds) { }

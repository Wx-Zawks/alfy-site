package com.alfy.api.dto;

import java.time.LocalDateTime;

public record AdminApplicationSceneResponse(
        Long id, String name, String slug, String slogan, String summary, String painPoint, String solution,
        String contentHtml, Long coverMediaId, boolean featured, Integer sortOrder, String status,
        String seoTitle, String seoDescription, String seoKeywords, LocalDateTime publishedAt, Long version
) { }

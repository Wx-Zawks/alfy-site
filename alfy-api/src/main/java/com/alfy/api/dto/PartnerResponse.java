package com.alfy.api.dto;
import java.time.LocalDateTime;
public record PartnerResponse(Long id, String name, String slug, String category, String summary, Long logoMediaId,
        String logoUrl, String websiteUrl, boolean featured, Integer sortOrder, String status, String seoTitle,
        String seoDescription, String seoKeywords, LocalDateTime publishedAt, Long version) { }

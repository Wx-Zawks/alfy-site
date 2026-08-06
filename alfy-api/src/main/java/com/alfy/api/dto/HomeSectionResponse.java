package com.alfy.api.dto;
import java.time.LocalDateTime;
public record HomeSectionResponse(Long id, String sectionKey, String label, String eyebrow, String title, String highlightText, String description, Long imageMediaId, String imageUrl, Long mobileMediaId, String mobileImageUrl, String buttonLabel, String buttonTarget, boolean enabled, Integer sortOrder, Long version, LocalDateTime updatedAt) { }

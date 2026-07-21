package com.alfy.api.dto;

import java.time.LocalDateTime;

public record HeroSlideResponse(
        Long id, String eyebrow, String title, String highlightText, String summary,
        String desktopImageUrl, String mobileImageUrl, ActionResponse primaryAction,
        ActionResponse secondaryAction, Integer sortOrder, String status, LocalDateTime startsAt,
        LocalDateTime endsAt, LocalDateTime publishedAt, Long version
) { }

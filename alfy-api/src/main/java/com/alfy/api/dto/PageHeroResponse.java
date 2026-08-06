package com.alfy.api.dto;

import java.time.LocalDateTime;

/** 单页顶部首屏栏目：文案、背景图和按钮均由后台维护。 */
public record PageHeroResponse(
        Long id, String pageKey, String eyebrow, String title, String highlightText, String summary,
        String backgroundImageUrl, String mobileBackgroundImageUrl, ActionResponse primaryAction,
        ActionResponse secondaryAction, String status, LocalDateTime publishedAt, Long version
) { }

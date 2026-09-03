package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record HeroSlideUpsertRequest(
        @Size(max = 255) String eyebrow,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String highlightText,
        @Size(max = 1000) String summary,
        Long desktopMediaId, Long mobileMediaId, @Size(max = 500) String backgroundActionTarget,
        @Size(max = 100) String primaryActionLabel, @Size(max = 500) String primaryActionTarget,
        @Size(max = 100) String secondaryActionLabel, @Size(max = 500) String secondaryActionTarget,
        Integer sortOrder, LocalDateTime startsAt, LocalDateTime endsAt, Long version
) { }

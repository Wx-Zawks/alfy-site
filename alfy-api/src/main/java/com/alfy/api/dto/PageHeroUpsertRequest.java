package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PageHeroUpsertRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "pageKey 仅支持小写字母、数字和连字符") @Size(max = 64) String pageKey,
        @Size(max = 255) String eyebrow, @NotBlank @Size(max = 255) String title,
        @Size(max = 255) String highlightText, @Size(max = 1000) String summary,
        Long backgroundMediaId, Long mobileBackgroundMediaId,
        @Size(max = 100) String primaryActionLabel, @Size(max = 500) String primaryActionTarget,
        @Size(max = 100) String secondaryActionLabel, @Size(max = 500) String secondaryActionTarget,
        Long version
) { }

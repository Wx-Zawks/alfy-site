package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCaseCategoryUpsertRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String slug,
        @Size(max = 1000) String summary,
        Integer sortOrder,
        Boolean enabled,
        Long version
) {
}

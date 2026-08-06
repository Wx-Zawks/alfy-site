package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminProductCategoryUpsertRequest(
        @NotBlank @Size(max = 100) String name, @NotBlank @Size(max = 100) String slug,
        @Size(max = 1000) String summary, Long coverMediaId, Integer sortOrder, Boolean enabled, Long version
) { }

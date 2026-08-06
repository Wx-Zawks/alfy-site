package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NavigationItemUpsertRequest(
        @NotBlank @Pattern(regexp = "HEADER|FOOTER", message = "导航区域仅支持 HEADER 或 FOOTER") String navigationArea,
        Long parentId, @NotBlank @Size(max = 100) String label, @NotBlank @Size(max = 500) String target,
        Integer sortOrder, Boolean highlighted, Boolean enabled, Long version
) { }

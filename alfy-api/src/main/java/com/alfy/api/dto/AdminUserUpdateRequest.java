package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminUserUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^(super_admin|content_admin)$", message = "角色不受支持")
        String role,
        @NotNull Boolean enabled
) {
}

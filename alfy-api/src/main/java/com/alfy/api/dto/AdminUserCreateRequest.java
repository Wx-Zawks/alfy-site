package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank
        @Pattern(regexp = "^(super_admin|content_admin)$", message = "角色不受支持")
        String role
) {
}

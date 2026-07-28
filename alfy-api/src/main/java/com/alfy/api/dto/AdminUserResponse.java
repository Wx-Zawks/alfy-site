package com.alfy.api.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String role,
        boolean enabled,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

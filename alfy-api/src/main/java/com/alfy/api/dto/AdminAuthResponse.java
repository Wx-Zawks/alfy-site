package com.alfy.api.dto;

public record AdminAuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AdminProfileResponse user
) {
}

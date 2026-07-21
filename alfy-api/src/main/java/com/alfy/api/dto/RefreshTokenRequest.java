package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "刷新令牌不能为空") @Size(max = 512, message = "刷新令牌格式不正确") String refreshToken
) {
}

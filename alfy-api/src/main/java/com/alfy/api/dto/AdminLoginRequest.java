package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 50, message = "用户名长度不能超过 50") String username,
        @NotBlank(message = "密码不能为空") @Size(max = 256, message = "密码长度不能超过 256") String password
) {
}

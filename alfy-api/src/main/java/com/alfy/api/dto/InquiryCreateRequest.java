package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String company,
        @Pattern(regexp = "^$|^[+0-9() -]{6,32}$", message = "手机号格式不正确") String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 32) String inquiryType,
        @Size(max = 5000) String message,
        Long productId,
        @Size(max = 1024) String sourceUrl,
        JsonNode utm,
        @AssertTrue(message = "请先阅读并同意隐私政策") Boolean privacyAccepted
) { }

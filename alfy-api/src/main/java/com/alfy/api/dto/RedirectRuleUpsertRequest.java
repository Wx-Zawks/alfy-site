package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record RedirectRuleUpsertRequest(
        @Size(max = 255) @Pattern(regexp = "^$|^[A-Za-z0-9.-]+$", message = "来源主机格式不正确") String sourceHost,
        @NotBlank @Size(max = 700) String sourcePath,
        @NotBlank @Size(max = 1024) String targetUrl,
        Integer statusCode,
        Boolean preserveQuery,
        @Size(max = 500) String note,
        Boolean enabled,
        Long version
) { }

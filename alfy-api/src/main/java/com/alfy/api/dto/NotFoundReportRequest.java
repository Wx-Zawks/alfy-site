package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotFoundReportRequest(
        @NotBlank
        @Size(max = 1024)
        @Pattern(regexp = "^/.*", message = "访问路径必须以 / 开头")
        String path,
        @Size(max = 1024) String referer
) {
}

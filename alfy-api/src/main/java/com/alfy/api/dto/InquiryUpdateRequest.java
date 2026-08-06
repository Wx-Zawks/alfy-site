package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryUpdateRequest(
        @NotBlank @Size(max = 16) String status,
        @Size(max = 5000) String adminNote
) { }

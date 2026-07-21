package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record AdminInquiryResponse(
        Long id, String inquiryNo, String name, String company, String phone, String email,
        String inquiryType, String message, Long productId, String sourceUrl, JsonNode utm,
        String status, String adminNote, LocalDateTime handledAt, LocalDateTime createdAt
) { }

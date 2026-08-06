package com.alfy.api.dto;

import java.time.LocalDateTime;

public record AdminOperationLogResponse(
        Long id,
        Long operatorId,
        String operator,
        String action,
        String objectType,
        Long objectId,
        String detail,
        LocalDateTime createdAt
) {
}

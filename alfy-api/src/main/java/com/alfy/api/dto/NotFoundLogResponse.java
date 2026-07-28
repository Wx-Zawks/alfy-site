package com.alfy.api.dto;

import java.time.LocalDateTime;

public record NotFoundLogResponse(
        Long id,
        String path,
        String referer,
        long count,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen
) {
}

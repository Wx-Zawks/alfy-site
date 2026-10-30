package com.alfy.api.dto;

import java.time.LocalDateTime;

public record SeoUrlResponse(
        String loc,
        LocalDateTime lastModified,
        String type
) { }

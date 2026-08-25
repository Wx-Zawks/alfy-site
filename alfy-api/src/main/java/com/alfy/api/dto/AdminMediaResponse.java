package com.alfy.api.dto;

import java.time.LocalDateTime;

public record AdminMediaResponse(
        Long id, String mediaType, String originalFilename, String mimeType, Long fileSize,
        Integer width, Integer height, String altText, String adminUrl, String thumbnailUrl,
        LocalDateTime createdAt
) { }

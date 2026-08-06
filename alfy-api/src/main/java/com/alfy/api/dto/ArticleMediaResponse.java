package com.alfy.api.dto;

/** 前台文章中可访问的媒体资源。 */
public record ArticleMediaResponse(
        Long id,
        String mediaType,
        String originalFilename,
        String mimeType,
        String usageType,
        String url,
        String caption,
        int sortOrder
) {
}

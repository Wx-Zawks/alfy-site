package com.alfy.api.dto;

import java.util.List;

/** 与 alfy-web 产品卡片所需字段对齐的公开产品摘要。 */
public record PublicProductListItemResponse(
        Long id,
        String name,
        String slug,
        String category,
        String categoryName,
        String summary,
        String coverImageUrl,
        List<String> features
) {
}

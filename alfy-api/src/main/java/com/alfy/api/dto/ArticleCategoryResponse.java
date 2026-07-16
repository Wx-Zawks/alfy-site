package com.alfy.api.dto;

import java.util.List;

/** 前台展示用的文章分类树节点。 */
public record ArticleCategoryResponse(
        Long id,
        String code,
        String name,
        int sortOrder,
        List<ArticleCategoryResponse> children
) {
}

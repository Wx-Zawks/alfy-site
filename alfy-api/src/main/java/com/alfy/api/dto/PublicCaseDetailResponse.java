package com.alfy.api.dto;

import java.util.List;

public record PublicCaseDetailResponse(
        Long id,
        String slug,
        String title,
        String scene,
        String sceneSlug,
        String location,
        String summary,
        String background,
        String customerNeed,
        String solution,
        String implementation,
        String resultSummary,
        String contentHtml,
        String coverImageUrl,
        List<PublicRelatedProductResponse> relatedProducts,
        SeoMetaResponse seo
) {
}

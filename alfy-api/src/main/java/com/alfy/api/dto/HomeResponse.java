package com.alfy.api.dto;

import java.util.List;

/** 首页只返回已发布、并被首页模块消费的内容。 */
public record HomeResponse(
        List<HeroSlideResponse> banners,
        List<PublicApplicationSceneResponse> applicationScenes,
        List<PublicCaseCategoryResponse> caseCategories,
        List<PublicCaseListItemResponse> featuredCases,
        TechnologyPageResponse technologyOverview,
        List<ArticleListItemResponse> featuredArticles
) { }

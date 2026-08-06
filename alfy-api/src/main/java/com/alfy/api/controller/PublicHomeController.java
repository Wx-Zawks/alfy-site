package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.HeroSlideResponse;
import com.alfy.api.dto.HomeResponse;
import com.alfy.api.dto.TechnologyPageResponse;
import com.alfy.api.service.HeroSlideService;
import com.alfy.api.service.PublicApplicationCaseService;
import com.alfy.api.service.PublicArticleService;
import com.alfy.api.service.TechnologyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicHomeController {
    private final HeroSlideService heroSlideService;
    private final TechnologyPageService technologyPageService;
    private final PublicApplicationCaseService applicationCaseService;
    private final PublicArticleService articleService;

    @GetMapping("/hero-slides") public ApiResponse<List<HeroSlideResponse>> heroSlides() { return ApiResponse.success(heroSlideService.listPublic()); }
    @GetMapping("/pages/technology") public ApiResponse<TechnologyPageResponse> technology() { return ApiResponse.success(technologyPageService.getPublic()); }
    @GetMapping("/technologies") public ApiResponse<List<TechnologyPageResponse>> technologies() { return ApiResponse.success(technologyPageService.listPublicDetails()); }
    @GetMapping("/technologies/{pageKey}") public ApiResponse<TechnologyPageResponse> technologyPage(@org.springframework.web.bind.annotation.PathVariable String pageKey) { return ApiResponse.success(technologyPageService.getPublic(pageKey)); }
    @GetMapping("/home") public ApiResponse<HomeResponse> home() {
        return ApiResponse.success(new HomeResponse(heroSlideService.listPublic(), applicationCaseService.listScenes(),
                applicationCaseService.listCases(null, null, true, 1, 4).getRecords(), technologyPageService.findPublic(),
                articleService.listHomeArticles(4)));
    }
}

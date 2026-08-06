package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.NavigationItemResponse;
import com.alfy.api.dto.PageHeroResponse;
import com.alfy.api.dto.SiteSettingResponse;
import com.alfy.api.service.PageHeroService;
import com.alfy.api.service.SiteNavigationService;
import com.alfy.api.service.SiteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSiteController {
    private final SiteSettingService siteSettingService;
    private final SiteNavigationService navigationService;
    private final PageHeroService pageHeroService;

    @GetMapping("/site") public ApiResponse<SiteSettingResponse> site() { return ApiResponse.success(siteSettingService.getPublic()); }
    @GetMapping("/navigation") public ApiResponse<Map<String, List<NavigationItemResponse>>> navigation() { return ApiResponse.success(Map.of("header", navigationService.listPublic("HEADER"), "footer", navigationService.listPublic("FOOTER"))); }
    @GetMapping("/pages/{pageKey}/hero") public ApiResponse<PageHeroResponse> pageHero(@PathVariable String pageKey) { return ApiResponse.success(pageHeroService.getPublic(pageKey)); }
}

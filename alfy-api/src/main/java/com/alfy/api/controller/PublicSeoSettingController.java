package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.SeoGlobalSettingResponse;
import com.alfy.api.dto.SeoRouteSettingResponse;
import com.alfy.api.dto.SeoUrlResponse;
import com.alfy.api.service.SeoSettingService;
import com.alfy.api.service.SeoUrlInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/seo")
@RequiredArgsConstructor
public class PublicSeoSettingController {
    private final SeoSettingService service;
    private final SeoUrlInventoryService urlInventoryService;

    @GetMapping("/settings")
    public ApiResponse<SeoGlobalSettingResponse> getSettings() {
        return ApiResponse.success(service.getPublicGlobal());
    }

    @GetMapping("/routes")
    public ApiResponse<List<SeoRouteSettingResponse>> listRoutes() {
        return ApiResponse.success(service.listPublicRoutes());
    }

    @GetMapping("/urls")
    public ApiResponse<List<SeoUrlResponse>> listUrls() {
        return ApiResponse.success(urlInventoryService.listPublicUrls());
    }
}

package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.SeoGlobalSettingResponse;
import com.alfy.api.dto.SeoGlobalSettingUpsertRequest;
import com.alfy.api.dto.SeoRouteSettingResponse;
import com.alfy.api.dto.SeoRouteSettingUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.SeoSettingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/seo")
@RequiredArgsConstructor
@Validated
public class AdminSeoSettingController {
    private final SeoSettingService service;

    @GetMapping("/settings")
    public ApiResponse<SeoGlobalSettingResponse> getSettings() {
        return ApiResponse.success(service.getAdminGlobal());
    }

    @PutMapping("/settings")
    public ApiResponse<SeoGlobalSettingResponse> saveSettings(
            @Valid @RequestBody SeoGlobalSettingUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(service.saveGlobal(request, principal));
    }

    @GetMapping("/routes")
    public ApiResponse<List<SeoRouteSettingResponse>> listRoutes() {
        return ApiResponse.success(service.listAdminRoutes());
    }

    @PutMapping("/routes/{routeKey}")
    public ApiResponse<SeoRouteSettingResponse> saveRoute(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$", message = "固定页面标识格式不正确") String routeKey,
            @Valid @RequestBody SeoRouteSettingUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(service.saveRoute(routeKey, request, principal));
    }
}

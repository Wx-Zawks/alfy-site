package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.SiteSettingResponse;
import com.alfy.api.dto.SiteSettingUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/site-settings")
@RequiredArgsConstructor
public class AdminSiteSettingController {
    private final SiteSettingService service;
    @GetMapping public ApiResponse<SiteSettingResponse> get() { return ApiResponse.success(service.getAdmin()); }
    @PutMapping public ApiResponse<SiteSettingResponse> save(@Valid @RequestBody SiteSettingUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.save(request, principal)); }
}

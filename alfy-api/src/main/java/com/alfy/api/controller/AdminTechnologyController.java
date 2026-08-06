package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.TechnologyPageResponse;
import com.alfy.api.dto.TechnologyPageUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.TechnologyPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/technologies")
@RequiredArgsConstructor
public class AdminTechnologyController {
    private final TechnologyPageService service;
    @GetMapping public ApiResponse<TechnologyPageResponse> get() { return ApiResponse.success(service.getAdmin()); }
    @PutMapping public ApiResponse<TechnologyPageResponse> save(@Valid @RequestBody TechnologyPageUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.save(request, principal)); }
    @PostMapping("/publish") public ApiResponse<TechnologyPageResponse> publish(@AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(principal)); }
    @PostMapping("/offline") public ApiResponse<TechnologyPageResponse> offline(@AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(principal)); }

    @GetMapping("/pages")
    public ApiResponse<List<TechnologyPageResponse>> listPages() {
        return ApiResponse.success(service.listAdmin());
    }

    @GetMapping("/pages/{pageKey}")
    public ApiResponse<TechnologyPageResponse> getPage(@PathVariable String pageKey) {
        return ApiResponse.success(service.getAdmin(pageKey));
    }

    @PutMapping("/pages/{pageKey}")
    public ApiResponse<TechnologyPageResponse> savePage(
            @PathVariable String pageKey,
            @Valid @RequestBody TechnologyPageUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(service.save(pageKey, request, principal));
    }

    @PostMapping("/pages/{pageKey}/publish")
    public ApiResponse<TechnologyPageResponse> publishPage(
            @PathVariable String pageKey,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(service.publish(pageKey, principal));
    }

    @PostMapping("/pages/{pageKey}/offline")
    public ApiResponse<TechnologyPageResponse> offlinePage(
            @PathVariable String pageKey,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(service.offline(pageKey, principal));
    }
}

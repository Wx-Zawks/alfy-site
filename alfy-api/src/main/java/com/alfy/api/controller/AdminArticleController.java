package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminArticleListItemResponse;
import com.alfy.api.dto.AdminArticleResponse;
import com.alfy.api.dto.AdminArticleUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminArticleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final AdminArticleService adminArticleService;

    @GetMapping
    public ApiResponse<PageResponse<AdminArticleListItemResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size
    ) {
        Page<AdminArticleListItemResponse> result = adminArticleService.list(status, categoryId, keyword, page, size);
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminArticleResponse> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(adminArticleService.get(id));
    }

    @PostMapping
    public ApiResponse<AdminArticleResponse> create(
            @Valid @RequestBody AdminArticleUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(adminArticleService.create(request, principal));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminArticleResponse> update(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminArticleUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(adminArticleService.update(id, request, principal));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<AdminArticleResponse> publish(
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(adminArticleService.publish(id, principal));
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<AdminArticleResponse> offline(
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(adminArticleService.offline(id, principal));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        adminArticleService.delete(id, principal);
        return ApiResponse.success();
    }
}

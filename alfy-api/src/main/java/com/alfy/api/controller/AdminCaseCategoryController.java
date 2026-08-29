package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminCaseCategoryResponse;
import com.alfy.api.dto.AdminCaseCategoryUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminCaseCategoryService;
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
@RequestMapping("/api/v1/admin/case-categories")
@RequiredArgsConstructor
public class AdminCaseCategoryController {
    private final AdminCaseCategoryService service;

    @GetMapping
    public ApiResponse<PageResponse<AdminCaseCategoryResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) {
        Page<AdminCaseCategoryResponse> result = service.list(keyword, page, size);
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminCaseCategoryResponse> get(@PathVariable @Min(1) Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<AdminCaseCategoryResponse> create(
            @Valid @RequestBody AdminCaseCategoryUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(service.create(request, principal));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCaseCategoryResponse> update(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminCaseCategoryUpsertRequest request,
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @Min(1) Long id,
                                    @AuthenticationPrincipal AdminPrincipal principal) {
        service.delete(id, principal);
        return ApiResponse.success();
    }
}

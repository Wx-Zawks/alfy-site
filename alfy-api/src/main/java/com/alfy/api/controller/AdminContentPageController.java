package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.ContentPageResponse;
import com.alfy.api.dto.ContentPageUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.ContentPageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/pages") @RequiredArgsConstructor
public class AdminContentPageController {
    private final ContentPageService service;
    @GetMapping public ApiResponse<PageResponse<ContentPageResponse>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") @Min(1) long page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) { Page<ContentPageResponse> result = service.list(status, keyword, page, size); return ApiResponse.success(PageResponse.from(result)); }
    @GetMapping("/{id}") public ApiResponse<ContentPageResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<ContentPageResponse> create(@Valid @RequestBody ContentPageUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.create(request, principal)); }
    @PutMapping("/{id}") public ApiResponse<ContentPageResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody ContentPageUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
    @PostMapping("/{id}/publish") public ApiResponse<ContentPageResponse> publish(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(id, principal)); }
    @PostMapping("/{id}/offline") public ApiResponse<ContentPageResponse> offline(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(id, principal)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { service.delete(id, principal); return ApiResponse.success(); }
}

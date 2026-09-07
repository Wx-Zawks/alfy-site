package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminCaseResponse;
import com.alfy.api.dto.AdminCaseHomeDisplayRequest;
import com.alfy.api.dto.AdminCaseHomeOrderRequest;
import com.alfy.api.dto.AdminCaseHomePinnedRequest;
import com.alfy.api.dto.AdminCaseUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminCaseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cases")
@RequiredArgsConstructor
public class AdminCaseController {
    private final AdminCaseService service;
    @GetMapping public ApiResponse<PageResponse<AdminCaseResponse>> list(@RequestParam(required = false) String status, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long sceneId, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") @Min(1) long page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) { Page<AdminCaseResponse> result = service.list(status, categoryId, sceneId, keyword, page, size); return ApiResponse.success(PageResponse.from(result)); }
    @GetMapping("/{id}") public ApiResponse<AdminCaseResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<AdminCaseResponse> create(@Valid @RequestBody AdminCaseUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.create(request, principal)); }
    @PutMapping("/{id}") public ApiResponse<AdminCaseResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody AdminCaseUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
    @PutMapping("/{id}/home-display") public ApiResponse<AdminCaseResponse> updateHomeDisplay(@PathVariable @Min(1) Long id, @Valid @RequestBody AdminCaseHomeDisplayRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.updateHomeDisplay(id, request.visible(), principal)); }
    @PutMapping("/{id}/home-pinned") public ApiResponse<AdminCaseResponse> updateHomePinned(@PathVariable @Min(1) Long id, @Valid @RequestBody AdminCaseHomePinnedRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.updateHomePinned(id, request.pinned(), principal)); }
    @PutMapping("/home-order") public ApiResponse<Void> updateHomeOrder(@Valid @RequestBody AdminCaseHomeOrderRequest request, @AuthenticationPrincipal AdminPrincipal principal) { service.updateHomeOrder(request, principal); return ApiResponse.success(); }
    @PostMapping("/{id}/publish") public ApiResponse<AdminCaseResponse> publish(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(id, principal)); }
    @PostMapping("/{id}/offline") public ApiResponse<AdminCaseResponse> offline(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(id, principal)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { service.delete(id, principal); return ApiResponse.success(); }
}

package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminApplicationSceneResponse;
import com.alfy.api.dto.AdminApplicationSceneUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminApplicationSceneService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/application-scenes")
@RequiredArgsConstructor
public class AdminApplicationSceneController {
    private final AdminApplicationSceneService service;
    @GetMapping public ApiResponse<PageResponse<AdminApplicationSceneResponse>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") @Min(1) long page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size) { Page<AdminApplicationSceneResponse> result = service.list(status, keyword, page, size); return ApiResponse.success(PageResponse.from(result)); }
    @GetMapping("/{id}") public ApiResponse<AdminApplicationSceneResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<AdminApplicationSceneResponse> create(@Valid @RequestBody AdminApplicationSceneUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.create(request, principal)); }
    @PutMapping("/{id}") public ApiResponse<AdminApplicationSceneResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody AdminApplicationSceneUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
    @PostMapping("/{id}/publish") public ApiResponse<AdminApplicationSceneResponse> publish(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(id, principal)); }
    @PostMapping("/{id}/offline") public ApiResponse<AdminApplicationSceneResponse> offline(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(id, principal)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { service.delete(id, principal); return ApiResponse.success(); }
}

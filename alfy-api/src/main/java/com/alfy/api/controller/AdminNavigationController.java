package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.NavigationItemResponse;
import com.alfy.api.dto.NavigationItemUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.SiteNavigationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/navigations")
@RequiredArgsConstructor
public class AdminNavigationController {
    private final SiteNavigationService service;
    @GetMapping public ApiResponse<List<NavigationItemResponse>> list(@RequestParam String area) { return ApiResponse.success(service.listAdmin(area)); }
    @GetMapping("/{id}") public ApiResponse<NavigationItemResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<NavigationItemResponse> create(@Valid @RequestBody NavigationItemUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.create(request, principal)); }
    @PutMapping("/{id}") public ApiResponse<NavigationItemResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody NavigationItemUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { service.delete(id, principal); return ApiResponse.success(); }
}

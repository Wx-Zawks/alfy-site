package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.HeroSlideResponse;
import com.alfy.api.dto.HeroSlideUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.HeroSlideService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
public class AdminHeroSlideController {
    private final HeroSlideService service;
    @GetMapping public ApiResponse<List<HeroSlideResponse>> list(@RequestParam(required = false) String status) { return ApiResponse.success(service.listAdmin(status)); }
    @GetMapping("/{id}") public ApiResponse<HeroSlideResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<HeroSlideResponse> create(@Valid @RequestBody HeroSlideUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.create(request, principal)); }
    @PutMapping("/{id}") public ApiResponse<HeroSlideResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody HeroSlideUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
    @PostMapping("/{id}/publish") public ApiResponse<HeroSlideResponse> publish(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(id, principal)); }
    @PostMapping("/{id}/offline") public ApiResponse<HeroSlideResponse> offline(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(id, principal)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { service.delete(id, principal); return ApiResponse.success(); }
}

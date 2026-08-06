package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.PageHeroResponse;
import com.alfy.api.dto.PageHeroUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.PageHeroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/page-heroes")
@RequiredArgsConstructor
public class AdminPageHeroController {
    private final PageHeroService service;
    @GetMapping public ApiResponse<List<PageHeroResponse>> list() { return ApiResponse.success(service.listAdmin()); }
    @GetMapping("/{id}") public ApiResponse<PageHeroResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.getAdmin(id)); }
    @PostMapping public ApiResponse<PageHeroResponse> create(@Valid @RequestBody PageHeroUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.create(request, principal)); }
    @PutMapping("/{id}") public ApiResponse<PageHeroResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody PageHeroUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
    @PostMapping("/{id}/publish") public ApiResponse<PageHeroResponse> publish(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(id, principal)); }
    @PostMapping("/{id}/offline") public ApiResponse<PageHeroResponse> offline(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(id, principal)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal principal) { service.delete(id, principal); return ApiResponse.success(); }
}

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

@RestController
@RequestMapping("/api/v1/admin/technologies")
@RequiredArgsConstructor
public class AdminTechnologyController {
    private final TechnologyPageService service;
    @GetMapping public ApiResponse<TechnologyPageResponse> get() { return ApiResponse.success(service.getAdmin()); }
    @PutMapping public ApiResponse<TechnologyPageResponse> save(@Valid @RequestBody TechnologyPageUpsertRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.save(request, principal)); }
    @PostMapping("/publish") public ApiResponse<TechnologyPageResponse> publish(@AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.publish(principal)); }
    @PostMapping("/offline") public ApiResponse<TechnologyPageResponse> offline(@AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.offline(principal)); }
}

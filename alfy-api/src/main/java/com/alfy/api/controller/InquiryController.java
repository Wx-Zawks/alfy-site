package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminInquiryResponse;
import com.alfy.api.dto.InquiryCreateRequest;
import com.alfy.api.dto.InquirySubmitResponse;
import com.alfy.api.dto.InquiryUpdateRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.InquiryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryService service;
    @PostMapping("/api/v1/public/inquiries") public ApiResponse<InquirySubmitResponse> submit(@Valid @RequestBody InquiryCreateRequest request) { return ApiResponse.success(service.submit(request)); }
    @GetMapping("/api/v1/admin/inquiries") public ApiResponse<PageResponse<AdminInquiryResponse>> list(@RequestParam(required = false) String status, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") @Min(1) long page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) { Page<AdminInquiryResponse> result = service.list(status, keyword, page, size); return ApiResponse.success(PageResponse.from(result)); }
    @GetMapping("/api/v1/admin/inquiries/{id}") public ApiResponse<AdminInquiryResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PutMapping("/api/v1/admin/inquiries/{id}") public ApiResponse<AdminInquiryResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody InquiryUpdateRequest request, @AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(service.update(id, request, principal)); }
}

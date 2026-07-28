package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.AdminOperationLogResponse;
import com.alfy.api.service.AdminOperationLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @GetMapping
    public ApiResponse<PageResponse<AdminOperationLogResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) long size
    ) {
        Page<AdminOperationLogResponse> result = adminOperationLogService.list(keyword, page, size);
        return ApiResponse.success(PageResponse.from(result));
    }
}

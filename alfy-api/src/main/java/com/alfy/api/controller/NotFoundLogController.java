package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.NotFoundLogResponse;
import com.alfy.api.dto.NotFoundReportRequest;
import com.alfy.api.service.NotFoundLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotFoundLogController {

    private final NotFoundLogService notFoundLogService;

    @PostMapping("/api/v1/public/not-found")
    public ApiResponse<Void> record(@Valid @RequestBody NotFoundReportRequest request) {
        notFoundLogService.record(request);
        return ApiResponse.success();
    }

    @GetMapping("/api/v1/admin/not-found-logs")
    public ApiResponse<List<NotFoundLogResponse>> list() {
        return ApiResponse.success(notFoundLogService.list());
    }
}

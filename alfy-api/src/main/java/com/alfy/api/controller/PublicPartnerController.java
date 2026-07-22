package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.PartnerResponse;
import com.alfy.api.service.PartnerService;
import com.alfy.api.service.ContentPageService;
import com.alfy.api.dto.ContentPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/public") @RequiredArgsConstructor
public class PublicPartnerController {
    private final PartnerService partnerService; private final ContentPageService contentPageService;
    @GetMapping("/partners") public ApiResponse<List<PartnerResponse>> partners() { return ApiResponse.success(partnerService.listPublic()); }
    @GetMapping("/pages/{pageKey}") public ApiResponse<ContentPageResponse> page(@PathVariable String pageKey) { return ApiResponse.success(contentPageService.getPublic(pageKey)); }
}

package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.RedirectResolveResponse;
import com.alfy.api.service.RedirectRuleService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/redirects")
@RequiredArgsConstructor
@Validated
public class PublicRedirectController {
    private final RedirectRuleService service;

    @GetMapping("/resolve")
    public ApiResponse<RedirectResolveResponse> resolve(
            @RequestParam(defaultValue = "") @Size(max = 255) String host,
            @RequestParam @Size(max = 700) String path,
            @RequestParam(defaultValue = "") @Size(max = 2000) String query
    ) {
        return ApiResponse.success(service.resolve(host, path, query));
    }
}

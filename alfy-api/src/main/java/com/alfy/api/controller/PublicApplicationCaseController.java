package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.PublicApplicationSceneResponse;
import com.alfy.api.dto.PublicCaseCategoryResponse;
import com.alfy.api.dto.PublicCaseDetailResponse;
import com.alfy.api.dto.PublicCaseListItemResponse;
import com.alfy.api.service.PublicApplicationCaseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicApplicationCaseController {

    private final PublicApplicationCaseService publicApplicationCaseService;

    @GetMapping("/application-scenes")
    public ApiResponse<List<PublicApplicationSceneResponse>> listScenes() {
        return ApiResponse.success(publicApplicationCaseService.listScenes());
    }

    @GetMapping("/case-categories")
    public ApiResponse<List<PublicCaseCategoryResponse>> listCaseCategories() {
        return ApiResponse.success(publicApplicationCaseService.listCaseCategories());
    }

    @GetMapping("/cases")
    public ApiResponse<PageResponse<PublicCaseListItemResponse>> listCases(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) long size
    ) {
        Page<PublicCaseListItemResponse> result = publicApplicationCaseService.listCases(category, scene, product, featured, page, size);
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/cases/{slug}")
    public ApiResponse<PublicCaseDetailResponse> getCase(@PathVariable String slug) {
        return ApiResponse.success(publicApplicationCaseService.getCase(slug));
    }
}

package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.PublicProductCategoryResponse;
import com.alfy.api.dto.PublicProductDetailResponse;
import com.alfy.api.dto.PublicProductListItemResponse;
import com.alfy.api.service.PublicProductService;
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
public class PublicProductController {

    private final PublicProductService publicProductService;

    @GetMapping("/product-categories")
    public ApiResponse<List<PublicProductCategoryResponse>> listCategories() {
        return ApiResponse.success(publicProductService.listCategories());
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<PublicProductListItemResponse>> listProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) long size
    ) {
        Page<PublicProductListItemResponse> result = publicProductService.listProducts(category, featured, page, size);
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/products/{slug}")
    public ApiResponse<PublicProductDetailResponse> getProduct(@PathVariable String slug) {
        return ApiResponse.success(publicProductService.getProduct(slug));
    }
}

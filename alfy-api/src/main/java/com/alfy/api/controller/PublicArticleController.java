package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.common.PageResponse;
import com.alfy.api.dto.ArticleCategoryResponse;
import com.alfy.api.dto.ArticleDetailResponse;
import com.alfy.api.dto.ArticleListItemResponse;
import com.alfy.api.service.PublicArticleService;
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

/** 对官网开放的文章读取接口。 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicArticleController {

    private final PublicArticleService publicArticleService;

    @GetMapping("/article-categories")
    public ApiResponse<List<ArticleCategoryResponse>> listCategories() {
        return ApiResponse.success(publicArticleService.listCategories());
    }

    @GetMapping("/articles")
    public ApiResponse<PageResponse<ArticleListItemResponse>> listArticles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size
    ) {
        Page<ArticleListItemResponse> result = publicArticleService.listArticles(category, page, size);
        return ApiResponse.success(PageResponse.from(result));
    }

    @GetMapping("/articles/{articleId}")
    public ApiResponse<ArticleDetailResponse> getArticle(@PathVariable @Min(1) Long articleId) {
        return ApiResponse.success(publicArticleService.getArticle(articleId));
    }
}

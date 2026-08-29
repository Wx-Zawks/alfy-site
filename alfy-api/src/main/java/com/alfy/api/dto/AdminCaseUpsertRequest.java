package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminCaseUpsertRequest(
        Long categoryId,
        /** 旧版 Admin 兼容字段；新版使用 categoryId 和 sceneIds。 */
        Long sceneId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String slug,
        @Size(max = 255) String clientName,
        @Size(max = 255) String location,
        @Size(max = 1000) String summary,
        String background,
        String customerNeed,
        String solution,
        String implementation,
        String resultSummary,
        String contentHtml,
        Long coverMediaId,
        Boolean featured,
        Integer sortOrder,
        @Size(max = 255) String seoTitle,
        @Size(max = 500) String seoDescription,
        @Size(max = 500) String seoKeywords,
        List<Long> productIds,
        List<Long> sceneIds,
        Long version
) { }

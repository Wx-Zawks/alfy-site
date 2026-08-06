package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 后台创建或编辑文章的结构化字段；发布状态由独立动作接口控制。 */
public record AdminArticleUpsertRequest(
        @NotBlank(message = "文章标题不能为空") @Size(max = 255, message = "文章标题不能超过 255 个字符") String title,
        @NotBlank(message = "slug 不能为空") @Size(max = 255, message = "slug 不能超过 255 个字符") String slug,
        @Size(max = 1000, message = "摘要不能超过 1000 个字符") String summary,
        String contentHtml,
        String contentText,
        Long coverMediaId,
        @Size(max = 100, message = "作者展示名不能超过 100 个字符") String authorName,
        @Size(max = 1024, message = "来源链接不能超过 1024 个字符") String sourceUrl,
        LocalDateTime sourcePublishedAt,
        @NotEmpty(message = "至少选择一个文章分类") List<@NotNull(message = "文章分类不能为空") Long> categoryIds,
        Integer sortOrder,
        Boolean featured,
        @Size(max = 32, message = "首页展示位不能超过 32 个字符") String homeSlot,
        Integer homeSortOrder,
        @Size(max = 255, message = "SEO 标题不能超过 255 个字符") String seoTitle,
        @Size(max = 500, message = "SEO 描述不能超过 500 个字符") String seoDescription,
        @Size(max = 500, message = "SEO 关键词不能超过 500 个字符") String seoKeywords,
        Long version
) {
}

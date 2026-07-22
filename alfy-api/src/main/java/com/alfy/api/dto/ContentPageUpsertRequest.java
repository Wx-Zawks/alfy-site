package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ContentPageUpsertRequest(@NotBlank @Size(max = 100) String pageKey, @NotBlank @Size(max = 255) String title,
        @Size(max = 100) String category, @Size(max = 1000) String summary, String contentHtml, Long coverMediaId,
        Boolean featured, Integer sortOrder, @Size(max = 255) String seoTitle, @Size(max = 500) String seoDescription,
        @Size(max = 500) String seoKeywords, Long version) { }

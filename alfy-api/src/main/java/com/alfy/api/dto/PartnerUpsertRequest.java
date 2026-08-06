package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PartnerUpsertRequest(@NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 100) String slug,
        @Size(max = 100) String category, @Size(max = 1000) String summary, Long logoMediaId, @Size(max = 500) String websiteUrl,
        Boolean featured, Integer sortOrder, @Size(max = 255) String seoTitle, @Size(max = 500) String seoDescription,
        @Size(max = 500) String seoKeywords, Long version) { }

package com.alfy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminProductUpsertRequest(
        @NotNull Long categoryId, @NotBlank @Size(max = 255) String name, @NotBlank @Size(max = 255) String slug,
        @Size(max = 1000) String summary, String contentHtml, String contentText, Long coverMediaId,
        List<@Size(max = 100) String> features, JsonNode specifications, Boolean featured, Integer sortOrder,
        @Size(max = 255) String seoTitle, @Size(max = 500) String seoDescription, @Size(max = 500) String seoKeywords,
        List<Long> sceneIds, Long version
) { }

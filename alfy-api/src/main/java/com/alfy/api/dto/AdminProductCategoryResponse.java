package com.alfy.api.dto;

public record AdminProductCategoryResponse(Long id, String name, String slug, String summary, Long coverMediaId,
                                           Integer sortOrder, boolean enabled, Long version) { }

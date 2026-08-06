package com.alfy.api.dto;

import java.util.List;

public record NavigationItemResponse(
        Long id, String label, String target, Integer sortOrder, boolean highlighted,
        boolean enabled, Long version, List<NavigationItemResponse> children
) { }

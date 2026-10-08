package com.alfy.api.dto;
import java.time.LocalDateTime;
public record BaseFacilityResponse(Long id, String name, String address, Long imageMediaId, String imageUrl, Integer sortOrder, boolean enabled, Long version, LocalDateTime updatedAt) { }

package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record BaseFacilityUpsertRequest(@NotBlank @Size(max = 255) String name, @Size(max = 500) String address, Long imageMediaId, Integer sortOrder, Boolean enabled, Long version) { }

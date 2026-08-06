package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record HomeSectionUpsertRequest(@NotBlank @Size(max = 64) String sectionKey, @NotBlank @Size(max = 100) String label, @Size(max = 255) String eyebrow, @NotBlank @Size(max = 255) String title, @Size(max = 255) String highlightText, @Size(max = 1000) String description, Long imageMediaId, Long mobileMediaId, @Size(max = 100) String buttonLabel, @Size(max = 500) String buttonTarget, Boolean enabled, Integer sortOrder, Long version) { }

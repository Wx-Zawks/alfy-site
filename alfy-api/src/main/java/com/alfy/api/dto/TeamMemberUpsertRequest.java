package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record TeamMemberUpsertRequest(@NotBlank @Size(max = 100) String role, @NotBlank @Size(max = 100) String name, @Size(max = 1000) String bio, Long photoMediaId, Integer sortOrder, Boolean enabled, Long version) { }

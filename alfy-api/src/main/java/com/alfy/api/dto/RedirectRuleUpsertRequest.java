package com.alfy.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RedirectRuleUpsertRequest(@NotBlank @Size(max = 700) String sourcePath, @NotBlank @Size(max = 1024) String targetUrl, Boolean enabled, Long version) { }

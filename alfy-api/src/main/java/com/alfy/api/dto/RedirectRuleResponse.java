package com.alfy.api.dto;
import java.time.LocalDateTime;
public record RedirectRuleResponse(Long id, String sourcePath, String targetUrl, boolean enabled, Long version, LocalDateTime updatedAt) { }

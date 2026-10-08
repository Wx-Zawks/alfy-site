package com.alfy.api.dto;
import java.time.LocalDateTime;
public record TeamMemberResponse(Long id, String role, String name, String bio, Long photoMediaId, String photoUrl, Integer sortOrder, boolean enabled, Long version, LocalDateTime updatedAt) { }

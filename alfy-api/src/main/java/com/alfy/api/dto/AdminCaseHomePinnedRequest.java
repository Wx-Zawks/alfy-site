package com.alfy.api.dto;

import jakarta.validation.constraints.NotNull;

public record AdminCaseHomePinnedRequest(@NotNull Boolean pinned) { }

package com.alfy.api.dto;

import jakarta.validation.constraints.NotNull;

public record AdminCaseHomeDisplayRequest(@NotNull Boolean visible) { }

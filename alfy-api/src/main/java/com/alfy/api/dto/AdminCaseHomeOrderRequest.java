package com.alfy.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminCaseHomeOrderRequest(@NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull @Min(1) Long id, @NotNull @Min(0) Integer sortOrder) { }
}

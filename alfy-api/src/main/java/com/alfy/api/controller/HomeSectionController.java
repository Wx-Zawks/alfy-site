package com.alfy.api.controller;
import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.HomeSectionResponse;
import com.alfy.api.dto.HomeSectionUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.HomeSectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequiredArgsConstructor
public class HomeSectionController { private final HomeSectionService service;
    @GetMapping("/api/v1/admin/home-sections") public ApiResponse<List<HomeSectionResponse>> listAdmin() { return ApiResponse.success(service.list(false)); }
    @GetMapping("/api/v1/admin/home-sections/{id}") public ApiResponse<HomeSectionResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping("/api/v1/admin/home-sections") public ApiResponse<HomeSectionResponse> create(@Valid @RequestBody HomeSectionUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(service.create(r, p)); }
    @PutMapping("/api/v1/admin/home-sections/{id}") public ApiResponse<HomeSectionResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody HomeSectionUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(service.update(id, r, p)); }
    @DeleteMapping("/api/v1/admin/home-sections/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal p) { service.delete(id, p); return ApiResponse.success(); }
    @GetMapping("/api/v1/public/home-sections") public ApiResponse<List<HomeSectionResponse>> listPublic() { return ApiResponse.success(service.list(true)); }
}

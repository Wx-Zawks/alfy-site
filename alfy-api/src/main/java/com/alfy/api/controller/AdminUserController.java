package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.AdminUserCreateRequest;
import com.alfy.api.dto.AdminUserResponse;
import com.alfy.api.dto.AdminUserUpdateRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.success(adminUserService.list());
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(
            @Valid @RequestBody AdminUserCreateRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(adminUserService.create(request, principal));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUserResponse> update(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(adminUserService.update(id, request, principal));
    }
}

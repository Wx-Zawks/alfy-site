package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.AdminAuthResponse;
import com.alfy.api.dto.AdminLoginRequest;
import com.alfy.api.dto.AdminProfileResponse;
import com.alfy.api.dto.RefreshTokenRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResponse<AdminAuthResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AdminAuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(adminAuthService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        adminAuthService.logout(request.refreshToken(), principal);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> currentProfile(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.success(adminAuthService.currentProfile(principal));
    }
}

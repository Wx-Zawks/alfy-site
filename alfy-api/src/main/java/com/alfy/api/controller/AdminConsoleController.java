package com.alfy.api.controller;

import com.alfy.api.common.ApiResponse;
import com.alfy.api.security.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/** Vben 管理端所需的登录后基础资料接口；内容菜单仍由前端路由模块维护。 */
@RestController @RequestMapping("/api/v1/admin")
public class AdminConsoleController {
    @GetMapping("/user/info") public ApiResponse<Map<String, Object>> userInfo(@AuthenticationPrincipal AdminPrincipal principal) {
        String role = principal.role().toLowerCase();
        return ApiResponse.success(Map.of("id", principal.id(), "username", principal.username(), "realName", principal.username(), "roles", List.of(role), "desc", "奥飞新材内容管理后台", "homePath", "/home-content/sections", "token", ""));
    }
    @GetMapping("/auth/codes") public ApiResponse<List<String>> accessCodes(@AuthenticationPrincipal AdminPrincipal principal) { return ApiResponse.success(List.of("CMS_READ", "CMS_WRITE")); }
    @GetMapping("/menu/all") public ApiResponse<List<Object>> menus() { return ApiResponse.success(List.of()); }
}

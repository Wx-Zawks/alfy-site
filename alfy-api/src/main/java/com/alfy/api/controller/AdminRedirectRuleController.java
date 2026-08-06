package com.alfy.api.controller;
import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.RedirectRuleResponse;
import com.alfy.api.dto.RedirectRuleUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.RedirectRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/admin/redirects") @RequiredArgsConstructor
public class AdminRedirectRuleController { private final RedirectRuleService service;
    @GetMapping public ApiResponse<List<RedirectRuleResponse>> list() { return ApiResponse.success(service.list()); }
    @GetMapping("/{id}") public ApiResponse<RedirectRuleResponse> get(@PathVariable @Min(1) Long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping public ApiResponse<RedirectRuleResponse> create(@Valid @RequestBody RedirectRuleUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(service.create(r, p)); }
    @PutMapping("/{id}") public ApiResponse<RedirectRuleResponse> update(@PathVariable @Min(1) Long id, @Valid @RequestBody RedirectRuleUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(service.update(id, r, p)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal p) { service.delete(id, p); return ApiResponse.success(); }
}

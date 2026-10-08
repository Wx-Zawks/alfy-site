package com.alfy.api.controller;
import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.BaseFacilityResponse;
import com.alfy.api.dto.BaseFacilityUpsertRequest;
import com.alfy.api.dto.TeamMemberResponse;
import com.alfy.api.dto.TeamMemberUpsertRequest;
import com.alfy.api.security.AdminPrincipal;
import com.alfy.api.service.BaseFacilityService;
import com.alfy.api.service.TeamMemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequiredArgsConstructor
public class AdminAboutContentController {
    private final TeamMemberService teamMemberService; private final BaseFacilityService baseFacilityService;
    @GetMapping("/api/v1/admin/about/team-members") public ApiResponse<List<TeamMemberResponse>> listTeamMembers() { return ApiResponse.success(teamMemberService.list(false)); }
    @GetMapping("/api/v1/admin/about/team-members/{id}") public ApiResponse<TeamMemberResponse> getTeamMember(@PathVariable @Min(1) Long id) { return ApiResponse.success(teamMemberService.get(id)); }
    @PostMapping("/api/v1/admin/about/team-members") public ApiResponse<TeamMemberResponse> createTeamMember(@Valid @RequestBody TeamMemberUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(teamMemberService.create(r, p)); }
    @PutMapping("/api/v1/admin/about/team-members/{id}") public ApiResponse<TeamMemberResponse> updateTeamMember(@PathVariable @Min(1) Long id, @Valid @RequestBody TeamMemberUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(teamMemberService.update(id, r, p)); }
    @DeleteMapping("/api/v1/admin/about/team-members/{id}") public ApiResponse<Void> deleteTeamMember(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal p) { teamMemberService.delete(id, p); return ApiResponse.success(); }
    @GetMapping("/api/v1/admin/about/base-facilities") public ApiResponse<List<BaseFacilityResponse>> listBaseFacilities() { return ApiResponse.success(baseFacilityService.list(false)); }
    @GetMapping("/api/v1/admin/about/base-facilities/{id}") public ApiResponse<BaseFacilityResponse> getBaseFacility(@PathVariable @Min(1) Long id) { return ApiResponse.success(baseFacilityService.get(id)); }
    @PostMapping("/api/v1/admin/about/base-facilities") public ApiResponse<BaseFacilityResponse> createBaseFacility(@Valid @RequestBody BaseFacilityUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(baseFacilityService.create(r, p)); }
    @PutMapping("/api/v1/admin/about/base-facilities/{id}") public ApiResponse<BaseFacilityResponse> updateBaseFacility(@PathVariable @Min(1) Long id, @Valid @RequestBody BaseFacilityUpsertRequest r, @AuthenticationPrincipal AdminPrincipal p) { return ApiResponse.success(baseFacilityService.update(id, r, p)); }
    @DeleteMapping("/api/v1/admin/about/base-facilities/{id}") public ApiResponse<Void> deleteBaseFacility(@PathVariable @Min(1) Long id, @AuthenticationPrincipal AdminPrincipal p) { baseFacilityService.delete(id, p); return ApiResponse.success(); }
}

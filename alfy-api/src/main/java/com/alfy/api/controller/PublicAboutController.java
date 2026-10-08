package com.alfy.api.controller;
import com.alfy.api.common.ApiResponse;
import com.alfy.api.dto.BaseFacilityResponse;
import com.alfy.api.dto.TeamMemberResponse;
import com.alfy.api.service.BaseFacilityService;
import com.alfy.api.service.TeamMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/api/v1/public/about")
@RequiredArgsConstructor
public class PublicAboutController {
    private final TeamMemberService teamMemberService; private final BaseFacilityService baseFacilityService;
    @GetMapping("/team-members") public ApiResponse<List<TeamMemberResponse>> teamMembers() { return ApiResponse.success(teamMemberService.list(true)); }
    @GetMapping("/base-facilities") public ApiResponse<List<BaseFacilityResponse>> baseFacilities() { return ApiResponse.success(baseFacilityService.list(true)); }
}

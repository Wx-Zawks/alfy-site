package com.alfy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record SeoGlobalSettingUpsertRequest(
        @NotBlank @Size(max = 255) String canonicalOrigin,
        @NotBlank @Size(max = 100) String titleTemplate,
        @NotBlank @Size(max = 255) String defaultTitle,
        @NotBlank @Size(max = 500) String defaultDescription,
        Long defaultOgMediaId,
        @NotBlank @Size(max = 255) String organizationName,
        @Size(max = 100) String organizationAlternateName,
        @Size(max = 255) String organizationLegalName,
        @Size(max = 1000) String organizationDescription,
        LocalDate foundingDate,
        @Size(max = 20) List<@Size(max = 500) String> sameAs,
        @Size(max = 255) @Pattern(regexp = "^[^<>\\\"']*$", message = "百度验证值不能包含 HTML 标签") String baiduSiteVerification,
        @Size(max = 255) @Pattern(regexp = "^[^<>\\\"']*$", message = "Google 验证值不能包含 HTML 标签") String googleSiteVerification,
        @Size(max = 255) @Pattern(regexp = "^[^<>\\\"']*$", message = "Bing 验证值不能包含 HTML 标签") String bingSiteVerification,
        Boolean ga4Enabled,
        @Size(max = 50) @Pattern(regexp = "^$|^G-[A-Z0-9]+$", message = "GA4 Measurement ID 格式应为 G-XXXXXXXXXX") String ga4MeasurementId,
        Boolean baiduAnalyticsEnabled,
        @Size(max = 100) @Pattern(regexp = "^$|^[A-Za-z0-9_-]+$", message = "百度统计 ID 格式不正确") String baiduAnalyticsId,
        Boolean clarityEnabled,
        @Size(max = 100) @Pattern(regexp = "^$|^[A-Za-z0-9]+$", message = "Clarity Project ID 格式不正确") String clarityProjectId,
        Boolean publicIndexEnabled,
        @Pattern(regexp = "allow|disallow", message = "OAI-SearchBot 策略只能是 allow 或 disallow") String oaiSearchbotPolicy,
        @Pattern(regexp = "allow|disallow", message = "GPTBot 策略只能是 allow 或 disallow") String gptbotPolicy,
        @Pattern(regexp = "allow|disallow", message = "Google-Extended 策略只能是 allow 或 disallow") String googleExtendedPolicy,
        Long version
) { }

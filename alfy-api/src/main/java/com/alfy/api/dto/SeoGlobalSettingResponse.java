package com.alfy.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SeoGlobalSettingResponse(
        Long id,
        String canonicalOrigin,
        String titleTemplate,
        String defaultTitle,
        String defaultDescription,
        Long defaultOgMediaId,
        String defaultOgImageUrl,
        String organizationName,
        String organizationAlternateName,
        String organizationLegalName,
        String organizationDescription,
        LocalDate foundingDate,
        List<String> sameAs,
        String baiduSiteVerification,
        String googleSiteVerification,
        String bingSiteVerification,
        boolean ga4Enabled,
        String ga4MeasurementId,
        boolean baiduAnalyticsEnabled,
        String baiduAnalyticsId,
        boolean clarityEnabled,
        String clarityProjectId,
        boolean publicIndexEnabled,
        String oaiSearchbotPolicy,
        String gptbotPolicy,
        String googleExtendedPolicy,
        Long version,
        LocalDateTime updatedAt
) { }

package com.alfy.api.dto;

public record SiteSettingResponse(
        Long id, String companyName, String logoUrl, String address, String servicePhone,
        String serviceEmail, String wechatQrImageUrl, String icpNumber, String copyrightText,
        String privacyPolicyUrl, Long version
) { }

package com.alfy.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record SiteSettingUpsertRequest(
        @Size(max = 255) String companyName, Long logoMediaId, @Size(max = 500) String address,
        @Size(max = 64) String servicePhone, @Email @Size(max = 255) String serviceEmail,
        Long wechatQrMediaId, @Size(max = 255) String icpNumber, @Size(max = 500) String copyrightText,
        @Size(max = 500) String privacyPolicyUrl, Long version
) { }

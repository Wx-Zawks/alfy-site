package com.alfy.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("seo_global_setting")
public class SeoGlobalSetting {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String canonicalOrigin;
    private String titleTemplate;
    private String defaultTitle;
    private String defaultDescription;
    private Long defaultOgMediaId;
    private String organizationName;
    private String organizationAlternateName;
    private String organizationLegalName;
    private String organizationDescription;
    private LocalDate foundingDate;
    private String sameAsJson;
    private String baiduSiteVerification;
    private String googleSiteVerification;
    private String bingSiteVerification;
    private Integer ga4Enabled;
    private String ga4MeasurementId;
    private Integer baiduAnalyticsEnabled;
    private String baiduAnalyticsId;
    private Integer clarityEnabled;
    private String clarityProjectId;
    private Integer publicIndexEnabled;
    private String oaiSearchbotPolicy;
    private String gptbotPolicy;
    private String googleExtendedPolicy;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}

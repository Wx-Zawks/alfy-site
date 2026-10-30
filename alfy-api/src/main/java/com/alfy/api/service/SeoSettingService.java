package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.SeoGlobalSettingResponse;
import com.alfy.api.dto.SeoGlobalSettingUpsertRequest;
import com.alfy.api.dto.SeoRouteSettingResponse;
import com.alfy.api.dto.SeoRouteSettingUpsertRequest;
import com.alfy.api.entity.SeoGlobalSetting;
import com.alfy.api.entity.SeoRouteSetting;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.SeoGlobalSettingMapper;
import com.alfy.api.mapper.SeoRouteSettingMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeoSettingService {

    private static final String DEFAULT_ORIGIN = "https://alfy.com.cn";
    private static final String DEFAULT_TEMPLATE = "%s｜奥飞新材";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final SeoGlobalSettingMapper globalMapper;
    private final SeoRouteSettingMapper routeMapper;
    private final AdminOperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public SeoGlobalSettingResponse getAdminGlobal() {
        return toGlobalResponse(currentGlobal());
    }

    public SeoGlobalSettingResponse getPublicGlobal() {
        return toGlobalResponse(currentGlobal());
    }

    public List<SeoRouteSettingResponse> listAdminRoutes() {
        return listRoutes();
    }

    public List<SeoRouteSettingResponse> listPublicRoutes() {
        return listRoutes();
    }

    @Transactional
    public SeoGlobalSettingResponse saveGlobal(
            SeoGlobalSettingUpsertRequest request,
            AdminPrincipal principal
    ) {
        validateGlobal(request);
        SeoGlobalSetting setting = findGlobal();
        if (setting == null) {
            setting = new SeoGlobalSetting();
            applyGlobal(setting, request);
            globalMapper.insert(setting);
            operationLogService.record(principal.id(), "CREATE", "SEO_GLOBAL_SETTING",
                    setting.getId(), "创建全局 SEO 配置");
        } else {
            if (request.version() == null || !request.version().equals(setting.getVersion())) {
                throw new BusinessException(ErrorCode.CONFLICT, "全局 SEO 配置已被其他管理员修改，请刷新后重试");
            }
            boolean originChanged = !setting.getCanonicalOrigin().equals(request.canonicalOrigin().trim());
            boolean requestedIndexEnabled = !Boolean.FALSE.equals(request.publicIndexEnabled());
            boolean indexChanged = booleanValue(setting.getPublicIndexEnabled()) != requestedIndexEnabled;
            applyGlobal(setting, request);
            if (globalMapper.updateById(setting) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "全局 SEO 配置已被其他管理员修改");
            }
            String detail = "更新全局 SEO 配置";
            if (originChanged) detail += "；正式域名已变更";
            if (indexChanged) detail += "；全站索引开关已变更";
            operationLogService.record(principal.id(), "UPDATE", "SEO_GLOBAL_SETTING",
                    setting.getId(), detail);
        }
        return getAdminGlobal();
    }

    @Transactional
    public SeoRouteSettingResponse saveRoute(
            String routeKey,
            SeoRouteSettingUpsertRequest request,
            AdminPrincipal principal
    ) {
        SeoRouteSetting setting = routeMapper.selectOne(new LambdaQueryWrapper<SeoRouteSetting>()
                .eq(SeoRouteSetting::getRouteKey, routeKey));
        if (setting == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "固定页面 SEO 配置不存在");
        }
        if (request.version() == null || !request.version().equals(setting.getVersion())) {
            throw new BusinessException(ErrorCode.CONFLICT, "固定页面 SEO 配置已被其他管理员修改，请刷新后重试");
        }
        setting.setSeoTitle(trimToNull(request.seoTitle()));
        setting.setSeoDescription(trimToNull(request.seoDescription()));
        setting.setSeoKeywords(trimToNull(request.seoKeywords()));
        setting.setOgMediaId(request.ogMediaId());
        setting.setIndexEnabled(Boolean.FALSE.equals(request.indexEnabled()) ? 0 : 1);
        if (routeMapper.updateById(setting) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "固定页面 SEO 配置已被其他管理员修改");
        }
        operationLogService.record(principal.id(), "UPDATE", "SEO_ROUTE_SETTING",
                setting.getId(), "更新固定页面 SEO：" + setting.getDisplayName());
        return toRouteResponse(routeMapper.selectById(setting.getId()));
    }

    private List<SeoRouteSettingResponse> listRoutes() {
        return routeMapper.selectList(new LambdaQueryWrapper<SeoRouteSetting>()
                        .orderByAsc(SeoRouteSetting::getId))
                .stream()
                .map(this::toRouteResponse)
                .toList();
    }

    private SeoGlobalSetting currentGlobal() {
        SeoGlobalSetting setting = findGlobal();
        if (setting != null) return setting;
        SeoGlobalSetting fallback = new SeoGlobalSetting();
        fallback.setCanonicalOrigin(DEFAULT_ORIGIN);
        fallback.setTitleTemplate(DEFAULT_TEMPLATE);
        fallback.setDefaultTitle("奥飞新材");
        fallback.setDefaultDescription("湖南奥飞新材料有限公司官方网站。");
        fallback.setOrganizationName("湖南奥飞新材料有限公司");
        fallback.setOrganizationAlternateName("奥飞新材");
        fallback.setOrganizationLegalName("湖南奥飞新材料有限公司");
        fallback.setPublicIndexEnabled(1);
        fallback.setOaiSearchbotPolicy("allow");
        fallback.setGptbotPolicy("allow");
        fallback.setGoogleExtendedPolicy("allow");
        return fallback;
    }

    private SeoGlobalSetting findGlobal() {
        return globalMapper.selectOne(new LambdaQueryWrapper<SeoGlobalSetting>()
                .orderByAsc(SeoGlobalSetting::getId)
                .last("LIMIT 1"));
    }

    private void validateGlobal(SeoGlobalSettingUpsertRequest request) {
        String origin = request.canonicalOrigin().trim();
        try {
            URI uri = new URI(origin);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "正式域名必须是无路径、查询参数和片段的 HTTPS 地址，例如 https://alfy.com.cn");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "正式域名格式不正确");
        }
        String template = request.titleTemplate().trim();
        if (template.indexOf("%s") < 0 || template.indexOf("%s") != template.lastIndexOf("%s")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标题模板必须且只能包含一个 %s");
        }
        validateEnabledId(request.ga4Enabled(), request.ga4MeasurementId(), "启用 GA4 时必须填写 Measurement ID");
        validateEnabledId(request.baiduAnalyticsEnabled(), request.baiduAnalyticsId(), "启用百度统计时必须填写统计 ID");
        validateEnabledId(request.clarityEnabled(), request.clarityProjectId(), "启用 Clarity 时必须填写 Project ID");
        if (request.sameAs() != null) {
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String value : request.sameAs()) {
                String url = trimToNull(value);
                if (url == null) continue;
                try {
                    URI uri = new URI(url);
                    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                        throw new BusinessException(ErrorCode.BAD_REQUEST, "官方资料链接必须是完整 HTTPS URL");
                    }
                } catch (URISyntaxException exception) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "官方资料链接格式不正确：" + url);
                }
                unique.add(url);
            }
            if (unique.size() > 20) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "官方资料链接最多 20 条");
            }
        }
    }

    private void validateEnabledId(Boolean enabled, String value, String message) {
        if (Boolean.TRUE.equals(enabled) && trimToNull(value) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private void applyGlobal(SeoGlobalSetting setting, SeoGlobalSettingUpsertRequest request) {
        setting.setCanonicalOrigin(request.canonicalOrigin().trim());
        setting.setTitleTemplate(request.titleTemplate().trim());
        setting.setDefaultTitle(request.defaultTitle().trim());
        setting.setDefaultDescription(request.defaultDescription().trim());
        setting.setDefaultOgMediaId(request.defaultOgMediaId());
        setting.setOrganizationName(request.organizationName().trim());
        setting.setOrganizationAlternateName(trimToNull(request.organizationAlternateName()));
        setting.setOrganizationLegalName(trimToNull(request.organizationLegalName()));
        setting.setOrganizationDescription(trimToNull(request.organizationDescription()));
        setting.setFoundingDate(request.foundingDate());
        setting.setSameAsJson(writeSameAs(request.sameAs()));
        setting.setBaiduSiteVerification(trimToNull(request.baiduSiteVerification()));
        setting.setGoogleSiteVerification(trimToNull(request.googleSiteVerification()));
        setting.setBingSiteVerification(trimToNull(request.bingSiteVerification()));
        setting.setGa4Enabled(Boolean.TRUE.equals(request.ga4Enabled()) ? 1 : 0);
        setting.setGa4MeasurementId(trimToNull(request.ga4MeasurementId()));
        setting.setBaiduAnalyticsEnabled(Boolean.TRUE.equals(request.baiduAnalyticsEnabled()) ? 1 : 0);
        setting.setBaiduAnalyticsId(trimToNull(request.baiduAnalyticsId()));
        setting.setClarityEnabled(Boolean.TRUE.equals(request.clarityEnabled()) ? 1 : 0);
        setting.setClarityProjectId(trimToNull(request.clarityProjectId()));
        setting.setPublicIndexEnabled(Boolean.FALSE.equals(request.publicIndexEnabled()) ? 0 : 1);
        setting.setOaiSearchbotPolicy(policy(request.oaiSearchbotPolicy()));
        setting.setGptbotPolicy(policy(request.gptbotPolicy()));
        setting.setGoogleExtendedPolicy(policy(request.googleExtendedPolicy()));
    }

    private SeoGlobalSettingResponse toGlobalResponse(SeoGlobalSetting setting) {
        return new SeoGlobalSettingResponse(
                setting.getId(),
                setting.getCanonicalOrigin(),
                setting.getTitleTemplate(),
                setting.getDefaultTitle(),
                setting.getDefaultDescription(),
                setting.getDefaultOgMediaId(),
                mediaUrl(setting.getDefaultOgMediaId()),
                setting.getOrganizationName(),
                setting.getOrganizationAlternateName(),
                setting.getOrganizationLegalName(),
                setting.getOrganizationDescription(),
                setting.getFoundingDate(),
                readSameAs(setting.getSameAsJson()),
                setting.getBaiduSiteVerification(),
                setting.getGoogleSiteVerification(),
                setting.getBingSiteVerification(),
                booleanValue(setting.getGa4Enabled()),
                setting.getGa4MeasurementId(),
                booleanValue(setting.getBaiduAnalyticsEnabled()),
                setting.getBaiduAnalyticsId(),
                booleanValue(setting.getClarityEnabled()),
                setting.getClarityProjectId(),
                booleanValue(setting.getPublicIndexEnabled()),
                policy(setting.getOaiSearchbotPolicy()),
                policy(setting.getGptbotPolicy()),
                policy(setting.getGoogleExtendedPolicy()),
                setting.getVersion(),
                setting.getUpdatedAt()
        );
    }

    private SeoRouteSettingResponse toRouteResponse(SeoRouteSetting setting) {
        return new SeoRouteSettingResponse(
                setting.getId(), setting.getRouteKey(), setting.getRoutePath(), setting.getDisplayName(),
                setting.getSeoTitle(), setting.getSeoDescription(), setting.getSeoKeywords(),
                setting.getOgMediaId(), mediaUrl(setting.getOgMediaId()),
                booleanValue(setting.getIndexEnabled()), setting.getVersion(), setting.getUpdatedAt()
        );
    }

    private String writeSameAs(List<String> values) {
        List<String> normalized = values == null ? List.of() : values.stream()
                .map(SeoSettingService::trimToNull)
                .filter(value -> value != null)
                .distinct()
                .toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "官方资料链接无法保存");
        }
    }

    private List<String> readSameAs(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            return new ArrayList<>();
        }
    }

    private static boolean booleanValue(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    private static String policy(String value) {
        return "disallow".equalsIgnoreCase(value) ? "disallow" : "allow";
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String mediaUrl(Long mediaId) {
        return mediaId == null ? null : "/api/v1/public/media/" + mediaId;
    }
}

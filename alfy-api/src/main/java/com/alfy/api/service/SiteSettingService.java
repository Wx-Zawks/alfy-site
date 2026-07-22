package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.SiteSettingResponse;
import com.alfy.api.dto.SiteSettingUpsertRequest;
import com.alfy.api.entity.SiteSetting;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.SiteSettingMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteSettingService {
    private final SiteSettingMapper siteSettingMapper;
    private final AdminOperationLogService operationLogService;

    public SiteSettingResponse getPublic() { return toResponse(current()); }
    public SiteSettingResponse getAdmin() { return toResponse(current()); }

    @Transactional
    public SiteSettingResponse save(SiteSettingUpsertRequest request, AdminPrincipal principal) {
        SiteSetting setting = find();
        if (setting == null) { setting = new SiteSetting(); apply(setting, request); siteSettingMapper.insert(setting); operationLogService.record(principal.id(), "CREATE", "SITE_SETTING", setting.getId(), "创建站点配置"); }
        else { if (request.version() == null || !request.version().equals(setting.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "站点配置已被其他管理员修改，请刷新后重试"); apply(setting, request); if (siteSettingMapper.updateById(setting) != 1) throw new BusinessException(ErrorCode.CONFLICT, "站点配置已被其他管理员修改"); operationLogService.record(principal.id(), "UPDATE", "SITE_SETTING", setting.getId(), "更新站点配置"); }
        return getAdmin();
    }

    private SiteSetting current() { SiteSetting setting = find(); return setting == null ? new SiteSetting() : setting; }
    private SiteSetting find() { return siteSettingMapper.selectOne(new LambdaQueryWrapper<SiteSetting>().orderByAsc(SiteSetting::getId).last("LIMIT 1")); }
    private void apply(SiteSetting s, SiteSettingUpsertRequest r) { s.setCompanyName(trim(r.companyName())); s.setLogoMediaId(r.logoMediaId()); s.setAddress(trim(r.address())); s.setServicePhone(trim(r.servicePhone())); s.setServiceEmail(trim(r.serviceEmail())); s.setWechatQrMediaId(r.wechatQrMediaId()); s.setIcpNumber(trim(r.icpNumber())); s.setCopyrightText(trim(r.copyrightText())); s.setPrivacyPolicyUrl(trim(r.privacyPolicyUrl())); }
    private SiteSettingResponse toResponse(SiteSetting s) { return new SiteSettingResponse(s.getId(), s.getCompanyName(), url(s.getLogoMediaId()), s.getAddress(), s.getServicePhone(), s.getServiceEmail(), url(s.getWechatQrMediaId()), s.getIcpNumber(), s.getCopyrightText(), s.getPrivacyPolicyUrl(), s.getVersion()); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String url(Long mediaId) { return mediaId == null ? null : "/api/v1/public/media/" + mediaId; }
}

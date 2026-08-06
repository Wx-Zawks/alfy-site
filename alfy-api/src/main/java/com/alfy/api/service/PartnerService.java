package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.PartnerResponse;
import com.alfy.api.dto.PartnerUpsertRequest;
import com.alfy.api.entity.Partner;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.PartnerMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor
public class PartnerService {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Set<String> STATUS = Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    private final PartnerMapper mapper;
    private final AdminOperationLogService logs;

    public Page<PartnerResponse> list(String status, String keyword, long page, long size) {
        if (status != null && !status.isBlank() && !STATUS.contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的合作伙伴状态");
        Page<Partner> result = mapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Partner>()
                .eq(status != null && !status.isBlank(), Partner::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(Partner::getName, keyword).or().like(Partner::getSlug, keyword))
                .orderByDesc(Partner::getIsFeatured).orderByAsc(Partner::getSortOrder).orderByAsc(Partner::getId));
        Page<PartnerResponse> out = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        out.setRecords(result.getRecords().stream().map(this::toResponse).toList()); return out;
    }
    public List<PartnerResponse> listPublic() { return mapper.selectList(new LambdaQueryWrapper<Partner>().eq(Partner::getStatus, "PUBLISHED").orderByAsc(Partner::getSortOrder).orderByAsc(Partner::getId)).stream().map(this::toResponse).toList(); }
    public PartnerResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public PartnerResponse create(PartnerUpsertRequest request, AdminPrincipal principal) { validate(request, null); Partner partner = new Partner(); apply(partner, request); partner.setStatus("DRAFT"); mapper.insert(partner); logs.record(principal.id(), "CREATE", "PARTNER", partner.getId(), "创建合作伙伴草稿"); return get(partner.getId()); }
    @Transactional public PartnerResponse update(Long id, PartnerUpsertRequest request, AdminPrincipal principal) { Partner partner = require(id); version(request.version(), partner.getVersion()); validate(request, id); apply(partner, request); if (mapper.updateById(partner) != 1) throw new BusinessException(ErrorCode.CONFLICT, "合作伙伴已被其他管理员修改"); logs.record(principal.id(), "UPDATE", "PARTNER", id, "更新合作伙伴"); return get(id); }
    @Transactional public PartnerResponse publish(Long id, AdminPrincipal principal) { Partner partner = require(id); if (partner.getName().isBlank() || partner.getSlug().isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写名称和 slug"); partner.setStatus("PUBLISHED"); if (partner.getPublishedAt() == null) partner.setPublishedAt(LocalDateTime.now()); mapper.updateById(partner); logs.record(principal.id(), "PUBLISH", "PARTNER", id, "发布合作伙伴"); return get(id); }
    @Transactional public PartnerResponse offline(Long id, AdminPrincipal principal) { Partner partner = require(id); partner.setStatus("OFFLINE"); mapper.updateById(partner); logs.record(principal.id(), "OFFLINE", "PARTNER", id, "下线合作伙伴"); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal principal) { require(id); mapper.deleteById(id); logs.record(principal.id(), "DELETE", "PARTNER", id, "删除合作伙伴"); }
    private void validate(PartnerUpsertRequest r, Long currentId) { String slug = r.slug().trim(); if (!SLUG.matcher(slug).matches()) throw new BusinessException(ErrorCode.BAD_REQUEST, "slug 仅支持小写字母、数字和连字符"); Partner duplicate = mapper.selectOne(new LambdaQueryWrapper<Partner>().eq(Partner::getSlug, slug)); if (duplicate != null && !duplicate.getId().equals(currentId)) throw new BusinessException(ErrorCode.CONFLICT, "slug 已被其他合作伙伴使用"); }
    private void apply(Partner p, PartnerUpsertRequest r) { p.setName(r.name().trim()); p.setSlug(r.slug().trim()); p.setCategory(trim(r.category())); p.setSummary(trim(r.summary())); p.setLogoMediaId(r.logoMediaId()); p.setWebsiteUrl(trim(r.websiteUrl())); p.setIsFeatured(Boolean.TRUE.equals(r.featured()) ? 1 : 0); p.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); p.setSeoTitle(trim(r.seoTitle())); p.setSeoDescription(trim(r.seoDescription())); p.setSeoKeywords(trim(r.seoKeywords())); }
    private Partner require(Long id) { Partner p = mapper.selectById(id); if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND, "合作伙伴不存在"); return p; }
    private void version(Long wanted, Long actual) { if (wanted == null || !wanted.equals(actual)) throw new BusinessException(ErrorCode.CONFLICT, "合作伙伴已被其他管理员修改，请刷新后重试"); }
    private PartnerResponse toResponse(Partner p) { return new PartnerResponse(p.getId(), p.getName(), p.getSlug(), p.getCategory(), p.getSummary(), p.getLogoMediaId(), url(p.getLogoMediaId()), p.getWebsiteUrl(), Integer.valueOf(1).equals(p.getIsFeatured()), p.getSortOrder(), p.getStatus(), p.getSeoTitle(), p.getSeoDescription(), p.getSeoKeywords(), p.getPublishedAt(), p.getVersion()); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String url(Long mediaId) { return mediaId == null ? null : "/api/v1/public/media/" + mediaId; }
}

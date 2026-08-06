package com.alfy.api.service;
import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.HomeSectionResponse;
import com.alfy.api.dto.HomeSectionUpsertRequest;
import com.alfy.api.entity.HomeSection;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.HomeSectionMapper;
import com.alfy.api.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.regex.Pattern;
@Service @RequiredArgsConstructor
public class HomeSectionService {
    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private final HomeSectionMapper mapper; private final AdminOperationLogService logs;
    public List<HomeSectionResponse> list(boolean publicOnly) { return mapper.selectList(new LambdaQueryWrapper<HomeSection>().eq(publicOnly, HomeSection::getEnabled, 1).orderByAsc(HomeSection::getSortOrder).orderByAsc(HomeSection::getId)).stream().map(this::toResponse).toList(); }
    public HomeSectionResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public HomeSectionResponse create(HomeSectionUpsertRequest r, AdminPrincipal p) { validate(r, null); HomeSection x = new HomeSection(); apply(x, r); mapper.insert(x); logs.record(p.id(), "CREATE", "HOME_SECTION", x.getId(), "创建首页区块 " + x.getSectionKey()); return get(x.getId()); }
    @Transactional public HomeSectionResponse update(Long id, HomeSectionUpsertRequest r, AdminPrincipal p) { HomeSection x = require(id); if (r.version() == null || !r.version().equals(x.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "首页区块已被其他管理员修改，请刷新后重试"); validate(r, id); apply(x, r); if (mapper.updateById(x) != 1) throw new BusinessException(ErrorCode.CONFLICT, "首页区块已被其他管理员修改"); logs.record(p.id(), "UPDATE", "HOME_SECTION", id, "更新首页区块 " + x.getSectionKey()); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { HomeSection x = require(id); mapper.deleteById(id); logs.record(p.id(), "DELETE", "HOME_SECTION", id, "删除首页区块 " + x.getSectionKey()); }
    private void validate(HomeSectionUpsertRequest r, Long current) { String key = r.sectionKey().trim(); if (!KEY.matcher(key).matches()) throw new BusinessException(ErrorCode.BAD_REQUEST, "sectionKey 仅支持小写字母、数字和连字符"); HomeSection other = mapper.selectOne(new LambdaQueryWrapper<HomeSection>().eq(HomeSection::getSectionKey, key)); if (other != null && !other.getId().equals(current)) throw new BusinessException(ErrorCode.CONFLICT, "首页区块标识已存在"); }
    private void apply(HomeSection x, HomeSectionUpsertRequest r) { x.setSectionKey(r.sectionKey().trim()); x.setLabel(r.label().trim()); x.setEyebrow(trim(r.eyebrow())); x.setTitle(r.title().trim()); x.setHighlightText(trim(r.highlightText())); x.setDescriptionText(trim(r.description())); x.setImageMediaId(r.imageMediaId()); x.setMobileMediaId(r.mobileMediaId()); x.setButtonLabel(trim(r.buttonLabel())); x.setButtonTarget(trim(r.buttonTarget())); x.setEnabled(r.enabled() == null || Boolean.TRUE.equals(r.enabled()) ? 1 : 0); x.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); }
    private HomeSection require(Long id) { HomeSection x = mapper.selectById(id); if (x == null) throw new BusinessException(ErrorCode.NOT_FOUND, "首页区块不存在"); return x; }
    private HomeSectionResponse toResponse(HomeSection x) { return new HomeSectionResponse(x.getId(), x.getSectionKey(), x.getLabel(), x.getEyebrow(), x.getTitle(), x.getHighlightText(), x.getDescriptionText(), x.getImageMediaId(), url(x.getImageMediaId()), x.getMobileMediaId(), url(x.getMobileMediaId()), x.getButtonLabel(), x.getButtonTarget(), Integer.valueOf(1).equals(x.getEnabled()), x.getSortOrder(), x.getVersion(), x.getUpdatedAt()); }
    private static String trim(String value) { return value == null ? null : value.trim(); } private static String url(Long id) { return id == null ? null : "/api/v1/public/media/" + id; }
}

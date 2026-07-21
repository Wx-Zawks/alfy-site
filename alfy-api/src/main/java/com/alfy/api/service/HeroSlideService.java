package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.ActionResponse;
import com.alfy.api.dto.HeroSlideResponse;
import com.alfy.api.dto.HeroSlideUpsertRequest;
import com.alfy.api.entity.HeroSlide;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.HeroSlideMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HeroSlideService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    private final HeroSlideMapper heroSlideMapper;
    private final AdminOperationLogService operationLogService;

    public List<HeroSlideResponse> listAdmin(String status) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的轮播状态");
        return heroSlideMapper.selectList(new LambdaQueryWrapper<HeroSlide>()
                        .eq(status != null && !status.isBlank(), HeroSlide::getStatus, status)
                        .orderByAsc(HeroSlide::getSortOrder).orderByDesc(HeroSlide::getUpdatedAt))
                .stream().map(this::toResponse).toList();
    }

    public List<HeroSlideResponse> listPublic() {
        LocalDateTime now = LocalDateTime.now();
        return heroSlideMapper.selectList(new LambdaQueryWrapper<HeroSlide>()
                        .eq(HeroSlide::getStatus, "PUBLISHED")
                        .and(q -> q.isNull(HeroSlide::getStartsAt).or().le(HeroSlide::getStartsAt, now))
                        .and(q -> q.isNull(HeroSlide::getEndsAt).or().gt(HeroSlide::getEndsAt, now))
                        .orderByAsc(HeroSlide::getSortOrder).orderByAsc(HeroSlide::getId))
                .stream().map(this::toResponse).toList();
    }

    public HeroSlideResponse get(Long id) { return toResponse(require(id)); }

    @Transactional
    public HeroSlideResponse create(HeroSlideUpsertRequest request, AdminPrincipal principal) {
        validate(request, null);
        HeroSlide slide = new HeroSlide(); apply(slide, request); slide.setStatus("DRAFT");
        heroSlideMapper.insert(slide);
        operationLogService.record(principal.id(), "CREATE", "HERO_SLIDE", slide.getId(), "创建首页轮播草稿");
        return get(slide.getId());
    }

    @Transactional
    public HeroSlideResponse update(Long id, HeroSlideUpsertRequest request, AdminPrincipal principal) {
        HeroSlide slide = require(id); assertVersion(request.version(), slide.getVersion()); validate(request, id); apply(slide, request);
        if (heroSlideMapper.updateById(slide) != 1) throw new BusinessException(ErrorCode.CONFLICT, "轮播已被其他管理员修改");
        operationLogService.record(principal.id(), "UPDATE", "HERO_SLIDE", id, "更新首页轮播"); return get(id);
    }

    @Transactional
    public HeroSlideResponse publish(Long id, AdminPrincipal principal) {
        HeroSlide slide = require(id);
        if (blank(slide.getTitle()) || slide.getDesktopMediaId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题和桌面端图片");
        slide.setStatus("PUBLISHED"); if (slide.getPublishedAt() == null) slide.setPublishedAt(LocalDateTime.now()); heroSlideMapper.updateById(slide);
        operationLogService.record(principal.id(), "PUBLISH", "HERO_SLIDE", id, "发布首页轮播"); return get(id);
    }

    @Transactional
    public HeroSlideResponse offline(Long id, AdminPrincipal principal) {
        HeroSlide slide = require(id); slide.setStatus("OFFLINE"); heroSlideMapper.updateById(slide);
        operationLogService.record(principal.id(), "OFFLINE", "HERO_SLIDE", id, "下线首页轮播"); return get(id);
    }

    @Transactional
    public void delete(Long id, AdminPrincipal principal) {
        require(id); heroSlideMapper.deleteById(id); operationLogService.record(principal.id(), "DELETE", "HERO_SLIDE", id, "删除首页轮播");
    }

    private void validate(HeroSlideUpsertRequest r, Long ignored) {
        if (r.startsAt() != null && r.endsAt() != null && !r.endsAt().isAfter(r.startsAt())) throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
    }
    private void apply(HeroSlide s, HeroSlideUpsertRequest r) {
        s.setEyebrow(trim(r.eyebrow())); s.setTitle(r.title().trim()); s.setHighlightText(trim(r.highlightText())); s.setSummary(trim(r.summary()));
        s.setDesktopMediaId(r.desktopMediaId()); s.setMobileMediaId(r.mobileMediaId()); s.setPrimaryActionLabel(trim(r.primaryActionLabel())); s.setPrimaryActionTarget(trim(r.primaryActionTarget()));
        s.setSecondaryActionLabel(trim(r.secondaryActionLabel())); s.setSecondaryActionTarget(trim(r.secondaryActionTarget())); s.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); s.setStartsAt(r.startsAt()); s.setEndsAt(r.endsAt());
    }
    private HeroSlide require(Long id) { HeroSlide slide = heroSlideMapper.selectById(id); if (slide == null) throw new BusinessException(ErrorCode.NOT_FOUND, "轮播不存在"); return slide; }
    private HeroSlideResponse toResponse(HeroSlide s) { return new HeroSlideResponse(s.getId(), s.getEyebrow(), s.getTitle(), s.getHighlightText(), s.getSummary(), url(s.getDesktopMediaId()), url(s.getMobileMediaId()), action(s.getPrimaryActionLabel(), s.getPrimaryActionTarget()), action(s.getSecondaryActionLabel(), s.getSecondaryActionTarget()), s.getSortOrder(), s.getStatus(), s.getStartsAt(), s.getEndsAt(), s.getPublishedAt(), s.getVersion()); }
    private ActionResponse action(String label, String target) { return blank(label) && blank(target) ? null : new ActionResponse(label, target); }
    private String url(Long id) { return id == null ? null : "/api/v1/public/media/" + id; }
    private void assertVersion(Long expected, Long actual) { if (expected == null || !expected.equals(actual)) throw new BusinessException(ErrorCode.CONFLICT, "轮播已被其他管理员修改，请刷新后重试"); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String trim(String value) { return value == null ? null : value.trim(); }
}

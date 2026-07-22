package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.ActionResponse;
import com.alfy.api.dto.PageHeroResponse;
import com.alfy.api.dto.PageHeroUpsertRequest;
import com.alfy.api.entity.PageHero;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.PageHeroMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PageHeroService {
    private final PageHeroMapper pageHeroMapper;
    private final AdminOperationLogService operationLogService;

    public List<PageHeroResponse> listAdmin() { return pageHeroMapper.selectList(new LambdaQueryWrapper<PageHero>().orderByAsc(PageHero::getPageKey)).stream().map(this::toResponse).toList(); }
    public PageHeroResponse getAdmin(Long id) { return toResponse(require(id)); }
    public PageHeroResponse getPublic(String pageKey) { PageHero hero = pageHeroMapper.selectOne(new LambdaQueryWrapper<PageHero>().eq(PageHero::getPageKey, pageKey).eq(PageHero::getStatus, "PUBLISHED")); if (hero == null) throw new BusinessException(ErrorCode.NOT_FOUND, "页面首屏内容不存在或尚未发布"); return toResponse(hero); }

    @Transactional
    public PageHeroResponse create(PageHeroUpsertRequest request, AdminPrincipal principal) {
        assertKeyAvailable(request.pageKey(), null); PageHero hero = new PageHero(); apply(hero, request); hero.setStatus("DRAFT"); pageHeroMapper.insert(hero); operationLogService.record(principal.id(), "CREATE", "PAGE_HERO", hero.getId(), "创建单页首屏草稿 " + hero.getPageKey()); return getAdmin(hero.getId());
    }
    @Transactional
    public PageHeroResponse update(Long id, PageHeroUpsertRequest request, AdminPrincipal principal) {
        PageHero hero = require(id); if (request.version() == null || !request.version().equals(hero.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "页面首屏内容已被其他管理员修改，请刷新后重试"); assertKeyAvailable(request.pageKey(), id); apply(hero, request); if (pageHeroMapper.updateById(hero) != 1) throw new BusinessException(ErrorCode.CONFLICT, "页面首屏内容已被其他管理员修改"); operationLogService.record(principal.id(), "UPDATE", "PAGE_HERO", id, "更新单页首屏 " + hero.getPageKey()); return getAdmin(id);
    }
    @Transactional
    public PageHeroResponse publish(Long id, AdminPrincipal principal) { PageHero hero = require(id); if (hero.getTitle() == null || hero.getTitle().isBlank() || hero.getBackgroundMediaId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题和桌面端背景图"); hero.setStatus("PUBLISHED"); if (hero.getPublishedAt() == null) hero.setPublishedAt(LocalDateTime.now()); pageHeroMapper.updateById(hero); operationLogService.record(principal.id(), "PUBLISH", "PAGE_HERO", id, "发布单页首屏 " + hero.getPageKey()); return getAdmin(id); }
    @Transactional public PageHeroResponse offline(Long id, AdminPrincipal principal) { PageHero hero = require(id); hero.setStatus("OFFLINE"); pageHeroMapper.updateById(hero); operationLogService.record(principal.id(), "OFFLINE", "PAGE_HERO", id, "下线单页首屏 " + hero.getPageKey()); return getAdmin(id); }
    @Transactional public void delete(Long id, AdminPrincipal principal) { PageHero hero = require(id); pageHeroMapper.deleteById(id); operationLogService.record(principal.id(), "DELETE", "PAGE_HERO", id, "删除单页首屏 " + hero.getPageKey()); }

    private void assertKeyAvailable(String key, Long currentId) { PageHero duplicate = pageHeroMapper.selectOne(new LambdaQueryWrapper<PageHero>().eq(PageHero::getPageKey, key)); if (duplicate != null && !duplicate.getId().equals(currentId)) throw new BusinessException(ErrorCode.CONFLICT, "该页面标识已存在"); }
    private void apply(PageHero hero, PageHeroUpsertRequest r) { hero.setPageKey(r.pageKey().trim()); hero.setEyebrow(trim(r.eyebrow())); hero.setTitle(r.title().trim()); hero.setHighlightText(trim(r.highlightText())); hero.setSummary(trim(r.summary())); hero.setBackgroundMediaId(r.backgroundMediaId()); hero.setMobileBackgroundMediaId(r.mobileBackgroundMediaId()); hero.setPrimaryActionLabel(trim(r.primaryActionLabel())); hero.setPrimaryActionTarget(trim(r.primaryActionTarget())); hero.setSecondaryActionLabel(trim(r.secondaryActionLabel())); hero.setSecondaryActionTarget(trim(r.secondaryActionTarget())); }
    private PageHero require(Long id) { PageHero hero = pageHeroMapper.selectById(id); if (hero == null) throw new BusinessException(ErrorCode.NOT_FOUND, "页面首屏内容不存在"); return hero; }
    private PageHeroResponse toResponse(PageHero hero) { return new PageHeroResponse(hero.getId(), hero.getPageKey(), hero.getEyebrow(), hero.getTitle(), hero.getHighlightText(), hero.getSummary(), url(hero.getBackgroundMediaId()), url(hero.getMobileBackgroundMediaId()), action(hero.getPrimaryActionLabel(), hero.getPrimaryActionTarget()), action(hero.getSecondaryActionLabel(), hero.getSecondaryActionTarget()), hero.getStatus(), hero.getPublishedAt(), hero.getVersion()); }
    private static ActionResponse action(String label, String target) { return label == null && target == null ? null : new ActionResponse(label, target); }
    private static String url(Long id) { return id == null ? null : "/api/v1/public/media/" + id; }
    private static String trim(String value) { return value == null ? null : value.trim(); }
}

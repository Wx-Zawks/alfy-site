package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.NavigationItemResponse;
import com.alfy.api.dto.NavigationItemUpsertRequest;
import com.alfy.api.entity.SiteNavigationItem;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.SiteNavigationItemMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteNavigationService {
    private final SiteNavigationItemMapper navigationItemMapper;
    private final AdminOperationLogService operationLogService;

    public List<NavigationItemResponse> listPublic(String area) { return tree(area, true); }
    public List<NavigationItemResponse> listAdmin(String area) { return tree(area, false); }
    public NavigationItemResponse get(Long id) { return toResponse(require(id), Map.of()); }

    @Transactional
    public NavigationItemResponse create(NavigationItemUpsertRequest request, AdminPrincipal principal) {
        validateParent(request.parentId(), request.navigationArea(), null); SiteNavigationItem item = new SiteNavigationItem(); apply(item, request); navigationItemMapper.insert(item); operationLogService.record(principal.id(), "CREATE", "SITE_NAVIGATION", item.getId(), "创建站点导航项"); return get(item.getId());
    }
    @Transactional
    public NavigationItemResponse update(Long id, NavigationItemUpsertRequest request, AdminPrincipal principal) {
        SiteNavigationItem item = require(id); if (request.version() == null || !request.version().equals(item.getVersion())) throw new BusinessException(ErrorCode.CONFLICT, "导航项已被其他管理员修改，请刷新后重试"); validateParent(request.parentId(), request.navigationArea(), id); apply(item, request); if (navigationItemMapper.updateById(item) != 1) throw new BusinessException(ErrorCode.CONFLICT, "导航项已被其他管理员修改"); operationLogService.record(principal.id(), "UPDATE", "SITE_NAVIGATION", id, "更新站点导航项"); return get(id);
    }
    @Transactional public void delete(Long id, AdminPrincipal principal) { SiteNavigationItem item = require(id); if (navigationItemMapper.selectCount(new LambdaQueryWrapper<SiteNavigationItem>().eq(SiteNavigationItem::getParentId, id)) > 0) throw new BusinessException(ErrorCode.CONFLICT, "请先删除子导航项"); navigationItemMapper.deleteById(id); operationLogService.record(principal.id(), "DELETE", "SITE_NAVIGATION", id, "删除站点导航项 " + item.getLabel()); }

    private List<NavigationItemResponse> tree(String area, boolean enabledOnly) {
        if (!"HEADER".equals(area) && !"FOOTER".equals(area)) throw new BusinessException(ErrorCode.BAD_REQUEST, "导航区域仅支持 HEADER 或 FOOTER");
        List<SiteNavigationItem> items = navigationItemMapper.selectList(new LambdaQueryWrapper<SiteNavigationItem>().eq(SiteNavigationItem::getNavigationArea, area).eq(enabledOnly, SiteNavigationItem::getEnabled, 1).orderByAsc(SiteNavigationItem::getSortOrder).orderByAsc(SiteNavigationItem::getId));
        Map<Long, List<SiteNavigationItem>> children = items.stream().filter(item -> item.getParentId() != null).collect(Collectors.groupingBy(SiteNavigationItem::getParentId));
        return items.stream().filter(item -> item.getParentId() == null).map(item -> toResponse(item, children)).toList();
    }
    private NavigationItemResponse toResponse(SiteNavigationItem item, Map<Long, List<SiteNavigationItem>> children) { List<NavigationItemResponse> childResponses = children.getOrDefault(item.getId(), List.of()).stream().sorted(Comparator.comparing(SiteNavigationItem::getSortOrder).thenComparing(SiteNavigationItem::getId)).map(child -> toResponse(child, children)).toList(); return new NavigationItemResponse(item.getId(), item.getLabel(), item.getTarget(), item.getSortOrder(), Integer.valueOf(1).equals(item.getHighlighted()), Integer.valueOf(1).equals(item.getEnabled()), item.getVersion(), childResponses); }
    private void validateParent(Long parentId, String area, Long currentId) { if (parentId == null) return; if (parentId.equals(currentId)) throw new BusinessException(ErrorCode.BAD_REQUEST, "导航项不能以自身为父项"); SiteNavigationItem parent = require(parentId); if (!area.equals(parent.getNavigationArea())) throw new BusinessException(ErrorCode.BAD_REQUEST, "父导航项必须属于相同导航区域"); }
    private void apply(SiteNavigationItem item, NavigationItemUpsertRequest r) { item.setNavigationArea(r.navigationArea()); item.setParentId(r.parentId()); item.setLabel(r.label().trim()); item.setTarget(r.target().trim()); item.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); item.setHighlighted(Boolean.TRUE.equals(r.highlighted()) ? 1 : 0); item.setEnabled(r.enabled() == null || Boolean.TRUE.equals(r.enabled()) ? 1 : 0); }
    private SiteNavigationItem require(Long id) { SiteNavigationItem item = navigationItemMapper.selectById(id); if (item == null) throw new BusinessException(ErrorCode.NOT_FOUND, "导航项不存在"); return item; }
}

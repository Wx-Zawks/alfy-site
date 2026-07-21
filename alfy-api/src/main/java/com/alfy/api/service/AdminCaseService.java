package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.AdminCaseResponse;
import com.alfy.api.dto.AdminCaseUpsertRequest;
import com.alfy.api.entity.ApplicationScene;
import com.alfy.api.entity.CaseProject;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.ProductCaseRel;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ApplicationSceneMapper;
import com.alfy.api.mapper.CaseProjectMapper;
import com.alfy.api.mapper.ProductCaseRelMapper;
import com.alfy.api.mapper.ProductMapper;
import com.alfy.api.security.AdminPrincipal;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCaseService {
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    private final CaseProjectMapper caseProjectMapper;
    private final ApplicationSceneMapper applicationSceneMapper;
    private final ProductMapper productMapper;
    private final ProductCaseRelMapper productCaseRelMapper;
    private final AdminOperationLogService operationLogService;
    private final HtmlSanitizer htmlSanitizer;

    public Page<AdminCaseResponse> list(String status, Long sceneId, String keyword, long page, long size) {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的案例状态");
        Page<CaseProject> result = caseProjectMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<CaseProject>()
                .eq(status != null && !status.isBlank(), CaseProject::getStatus, status).eq(sceneId != null, CaseProject::getSceneId, sceneId)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(CaseProject::getTitle, keyword).or().like(CaseProject::getSlug, keyword))
                .orderByDesc(CaseProject::getIsFeatured).orderByAsc(CaseProject::getSortOrder).orderByDesc(CaseProject::getUpdatedAt));
        Page<AdminCaseResponse> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).toList()); return response;
    }
    public AdminCaseResponse get(Long id) { return toResponse(require(id)); }
    @Transactional public AdminCaseResponse create(AdminCaseUpsertRequest r, AdminPrincipal p) {
        validate(r, null); CaseProject item = new CaseProject(); apply(item, r); item.setStatus("DRAFT"); caseProjectMapper.insert(item); replaceProducts(item.getId(), r.productIds());
        operationLogService.record(p.id(), "CREATE", "CASE_PROJECT", item.getId(), "创建案例草稿"); return get(item.getId());
    }
    @Transactional public AdminCaseResponse update(Long id, AdminCaseUpsertRequest r, AdminPrincipal p) {
        CaseProject item = require(id); assertVersion(r.version(), item.getVersion()); validate(r, id); apply(item, r);
        if (caseProjectMapper.updateById(item) != 1) throw new BusinessException(ErrorCode.CONFLICT, "案例已被其他管理员修改");
        replaceProducts(id, r.productIds()); operationLogService.record(p.id(), "UPDATE", "CASE_PROJECT", id, "更新案例"); return get(id);
    }
    @Transactional public AdminCaseResponse publish(Long id, AdminPrincipal p) {
        CaseProject item = require(id);
        if (item.getTitle() == null || item.getTitle().isBlank() || item.getSlug() == null || item.getSlug().isBlank() || item.getSummary() == null || item.getSummary().isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布前必须填写标题、slug 和摘要");
        ApplicationScene scene = applicationSceneMapper.selectById(item.getSceneId());
        if (scene == null || !"PUBLISHED".equals(scene.getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "发布案例前，所属应用场景必须已发布");
        item.setStatus("PUBLISHED"); if (item.getPublishedAt() == null) item.setPublishedAt(LocalDateTime.now()); caseProjectMapper.updateById(item);
        operationLogService.record(p.id(), "PUBLISH", "CASE_PROJECT", id, "发布案例"); return get(id);
    }
    @Transactional public AdminCaseResponse offline(Long id, AdminPrincipal p) { CaseProject item = require(id); item.setStatus("OFFLINE"); caseProjectMapper.updateById(item); operationLogService.record(p.id(), "OFFLINE", "CASE_PROJECT", id, "下线案例"); return get(id); }
    @Transactional public void delete(Long id, AdminPrincipal p) { require(id); caseProjectMapper.deleteById(id); productCaseRelMapper.delete(new LambdaQueryWrapper<ProductCaseRel>().eq(ProductCaseRel::getCaseId, id)); operationLogService.record(p.id(), "DELETE", "CASE_PROJECT", id, "软删除案例"); }

    private void validate(AdminCaseUpsertRequest r, Long currentId) {
        if (!SLUG_PATTERN.matcher(r.slug().trim()).matches()) throw new BusinessException(ErrorCode.BAD_REQUEST, "slug 仅支持小写字母、数字和连字符");
        if (applicationSceneMapper.selectById(r.sceneId()) == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "所属应用场景不存在");
        CaseProject duplicate = caseProjectMapper.selectOne(new LambdaQueryWrapper<CaseProject>().eq(CaseProject::getSlug, r.slug().trim()));
        if (duplicate != null && !duplicate.getId().equals(currentId)) throw new BusinessException(ErrorCode.CONFLICT, "slug 已被其他案例使用");
        Set<Long> ids = r.productIds() == null ? Set.of() : new LinkedHashSet<>(r.productIds());
        if (r.productIds() != null && (ids.size() != r.productIds().size() || productMapper.selectCount(new LambdaQueryWrapper<Product>().in(Product::getId, ids)) != ids.size())) throw new BusinessException(ErrorCode.BAD_REQUEST, "关联产品不存在或重复");
    }
    private void apply(CaseProject x, AdminCaseUpsertRequest r) { x.setSceneId(r.sceneId()); x.setTitle(r.title().trim()); x.setSlug(r.slug().trim()); x.setClientName(r.clientName()); x.setLocation(r.location()); x.setSummary(r.summary()); x.setBackground(r.background()); x.setCustomerNeed(r.customerNeed()); x.setSolution(r.solution()); x.setImplementation(r.implementation()); x.setResultSummary(r.resultSummary()); x.setContentHtml(htmlSanitizer.clean(r.contentHtml())); x.setCoverMediaId(r.coverMediaId()); x.setIsFeatured(Boolean.TRUE.equals(r.featured()) ? 1 : 0); x.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder()); x.setSeoTitle(r.seoTitle()); x.setSeoDescription(r.seoDescription()); x.setSeoKeywords(r.seoKeywords()); }
    private void replaceProducts(Long caseId, List<Long> ids) { productCaseRelMapper.delete(new LambdaQueryWrapper<ProductCaseRel>().eq(ProductCaseRel::getCaseId, caseId)); if (ids == null) return; int order = 0; for (Long id : new LinkedHashSet<>(ids)) { ProductCaseRel rel = new ProductCaseRel(); rel.setCaseId(caseId); rel.setProductId(id); rel.setSortOrder(order++); productCaseRelMapper.insert(rel); } }
    private List<Long> productIds(Long caseId) { return productCaseRelMapper.selectList(new LambdaQueryWrapper<ProductCaseRel>().eq(ProductCaseRel::getCaseId, caseId).orderByAsc(ProductCaseRel::getSortOrder)).stream().map(ProductCaseRel::getProductId).collect(Collectors.toList()); }
    private void assertVersion(Long requested, Long actual) { if (requested == null || !requested.equals(actual)) throw new BusinessException(ErrorCode.CONFLICT, "案例已被其他管理员修改，请刷新后重试"); }
    private CaseProject require(Long id) { CaseProject item = caseProjectMapper.selectById(id); if (item == null) throw new BusinessException(ErrorCode.NOT_FOUND, "案例不存在"); return item; }
    private AdminCaseResponse toResponse(CaseProject x) { return new AdminCaseResponse(x.getId(), x.getSceneId(), x.getTitle(), x.getSlug(), x.getClientName(), x.getLocation(), x.getSummary(), x.getBackground(), x.getCustomerNeed(), x.getSolution(), x.getImplementation(), x.getResultSummary(), x.getContentHtml(), x.getCoverMediaId(), Integer.valueOf(1).equals(x.getIsFeatured()), x.getSortOrder(), x.getStatus(), x.getSeoTitle(), x.getSeoDescription(), x.getSeoKeywords(), x.getPublishedAt(), x.getVersion(), productIds(x.getId())); }
}

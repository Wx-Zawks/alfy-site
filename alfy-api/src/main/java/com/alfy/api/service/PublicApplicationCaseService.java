package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.PublicApplicationSceneResponse;
import com.alfy.api.dto.PublicCaseCategoryResponse;
import com.alfy.api.dto.PublicCaseDetailResponse;
import com.alfy.api.dto.PublicCaseListItemResponse;
import com.alfy.api.dto.PublicRelatedProductResponse;
import com.alfy.api.dto.SeoMetaResponse;
import com.alfy.api.entity.ApplicationScene;
import com.alfy.api.entity.CaseCategory;
import com.alfy.api.entity.CaseProject;
import com.alfy.api.entity.CaseSceneRel;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.ProductCaseRel;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ApplicationSceneMapper;
import com.alfy.api.mapper.CaseCategoryMapper;
import com.alfy.api.mapper.CaseProjectMapper;
import com.alfy.api.mapper.CaseSceneRelMapper;
import com.alfy.api.mapper.ProductCaseRelMapper;
import com.alfy.api.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicApplicationCaseService {

    private static final String PUBLISHED = "PUBLISHED";

    private final ApplicationSceneMapper applicationSceneMapper;
    private final CaseCategoryMapper caseCategoryMapper;
    private final CaseProjectMapper caseProjectMapper;
    private final CaseSceneRelMapper caseSceneRelMapper;
    private final ProductCaseRelMapper productCaseRelMapper;
    private final ProductMapper productMapper;

    public List<PublicApplicationSceneResponse> listScenes() {
        return applicationSceneMapper.selectList(new LambdaQueryWrapper<ApplicationScene>()
                        .eq(ApplicationScene::getStatus, PUBLISHED)
                        .orderByAsc(ApplicationScene::getSortOrder)
                        .orderByAsc(ApplicationScene::getId))
                .stream().map(this::toSceneResponse).toList();
    }

    public List<PublicCaseCategoryResponse> listCaseCategories() {
        return caseCategoryMapper.selectList(new LambdaQueryWrapper<CaseCategory>()
                        .eq(CaseCategory::getStatus, 1)
                        .orderByAsc(CaseCategory::getSortOrder)
                        .orderByAsc(CaseCategory::getId))
                .stream().map(this::toCategoryResponse).toList();
    }

    /** 兼容旧调用：第一项仍是场景筛选。 */
    public Page<PublicCaseListItemResponse> listCases(
            String sceneSlug, String productSlug, Boolean featured, long pageNumber, long pageSize) {
        return listCases(null, sceneSlug, productSlug, featured, pageNumber, pageSize);
    }

    public Page<PublicCaseListItemResponse> listCases(
            String categorySlug, String sceneSlug, String productSlug, Boolean featured,
            long pageNumber, long pageSize) {
        Set<Long> enabledCategoryIds = caseCategoryMapper.selectList(new LambdaQueryWrapper<CaseCategory>()
                        .select(CaseCategory::getId)
                        .eq(CaseCategory::getStatus, 1))
                .stream().map(CaseCategory::getId).collect(Collectors.toSet());
        if (enabledCategoryIds.isEmpty()) {
            return new Page<PublicCaseListItemResponse>(pageNumber, pageSize, 0);
        }

        Long categoryId = resolveCategoryId(categorySlug);
        Long sceneId = resolveSceneId(sceneSlug);
        Set<Long> sceneCaseIds = resolveCaseIdsByScene(sceneId);
        Set<Long> productCaseIds = resolveCaseIdsByProduct(productSlug);
        if ((categoryId != null && !enabledCategoryIds.contains(categoryId))
                || (sceneCaseIds != null && sceneCaseIds.isEmpty())
                || (productCaseIds != null && productCaseIds.isEmpty())) {
            return new Page<PublicCaseListItemResponse>(pageNumber, pageSize, 0);
        }

        Page<CaseProject> page = caseProjectMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<CaseProject>()
                        .eq(CaseProject::getStatus, PUBLISHED)
                        .in(CaseProject::getCategoryId, enabledCategoryIds)
                        .eq(categoryId != null, CaseProject::getCategoryId, categoryId)
                        .in(sceneCaseIds != null, CaseProject::getId, sceneCaseIds)
                        .in(productCaseIds != null, CaseProject::getId, productCaseIds)
                        .orderByDesc(CaseProject::getIsFeatured)
                        .orderByAsc(CaseProject::getSortOrder)
                        .orderByDesc(CaseProject::getPublishedAt)
                        .orderByDesc(CaseProject::getId));
        Map<Long, CaseCategory> categories = categoriesById(page.getRecords());
        Map<Long, ApplicationScene> primaryScenes = primaryScenesByCase(page.getRecords());
        Page<PublicCaseListItemResponse> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(item -> toCaseListItem(item, categories.get(item.getCategoryId()), primaryScenes.get(item.getId())))
                .toList());
        return result;
    }

    public PublicCaseDetailResponse getCase(String slug) {
        CaseProject item = caseProjectMapper.selectOne(new LambdaQueryWrapper<CaseProject>()
                .eq(CaseProject::getSlug, slug)
                .eq(CaseProject::getStatus, PUBLISHED));
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "案例不存在或尚未发布");
        }
        CaseCategory category = caseCategoryMapper.selectById(item.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "案例所属分类不存在或尚未启用");
        }
        ApplicationScene scene = primaryScenesByCase(List.of(item)).get(item.getId());
        return new PublicCaseDetailResponse(
                item.getId(), item.getSlug(), item.getTitle(), category.getName(), category.getSlug(),
                scene == null ? null : scene.getName(), scene == null ? null : scene.getSlug(), item.getLocation(),
                item.getSummary(), item.getBackground(), item.getCustomerNeed(), item.getSolution(), item.getImplementation(),
                item.getResultSummary(), item.getContentHtml(), mediaUrl(item.getCoverMediaId()), relatedProducts(item.getId()),
                new SeoMetaResponse(item.getSeoTitle(), item.getSeoDescription(), item.getSeoKeywords()));
    }

    private Long resolveCategoryId(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) return null;
        CaseCategory category = caseCategoryMapper.selectOne(new LambdaQueryWrapper<CaseCategory>()
                .eq(CaseCategory::getSlug, categorySlug).eq(CaseCategory::getStatus, 1));
        return category == null ? -1L : category.getId();
    }

    private Long resolveSceneId(String sceneSlug) {
        if (sceneSlug == null || sceneSlug.isBlank()) return null;
        ApplicationScene scene = applicationSceneMapper.selectOne(new LambdaQueryWrapper<ApplicationScene>()
                .eq(ApplicationScene::getSlug, sceneSlug).eq(ApplicationScene::getStatus, PUBLISHED));
        return scene == null ? -1L : scene.getId();
    }

    private Set<Long> resolveCaseIdsByScene(Long sceneId) {
        if (sceneId == null) return null;
        if (sceneId < 0) return Collections.emptySet();
        return caseSceneRelMapper.selectList(new LambdaQueryWrapper<CaseSceneRel>()
                        .eq(CaseSceneRel::getSceneId, sceneId))
                .stream().map(CaseSceneRel::getCaseId).collect(Collectors.toSet());
    }

    private Set<Long> resolveCaseIdsByProduct(String productSlug) {
        if (productSlug == null || productSlug.isBlank()) return null;
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getSlug, productSlug).eq(Product::getStatus, PUBLISHED));
        if (product == null) return Collections.emptySet();
        return productCaseRelMapper.selectList(new LambdaQueryWrapper<ProductCaseRel>()
                        .eq(ProductCaseRel::getProductId, product.getId()))
                .stream().map(ProductCaseRel::getCaseId).collect(Collectors.toSet());
    }

    private Map<Long, CaseCategory> categoriesById(Collection<CaseProject> cases) {
        Set<Long> ids = cases.stream().map(CaseProject::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return caseCategoryMapper.selectBatchIds(ids).stream()
                .filter(category -> Integer.valueOf(1).equals(category.getStatus()))
                .collect(Collectors.toMap(CaseCategory::getId, Function.identity()));
    }

    private Map<Long, ApplicationScene> primaryScenesByCase(Collection<CaseProject> cases) {
        Set<Long> caseIds = cases.stream().map(CaseProject::getId).collect(Collectors.toSet());
        if (caseIds.isEmpty()) return Map.of();
        List<CaseSceneRel> relations = caseSceneRelMapper.selectList(new LambdaQueryWrapper<CaseSceneRel>()
                .in(CaseSceneRel::getCaseId, caseIds)
                .orderByAsc(CaseSceneRel::getSortOrder)
                .orderByAsc(CaseSceneRel::getId));
        Map<Long, List<Long>> sceneIdsByCase = new LinkedHashMap<>();
        for (CaseSceneRel relation : relations) {
            sceneIdsByCase.computeIfAbsent(relation.getCaseId(), ignored -> new ArrayList<>()).add(relation.getSceneId());
        }
        for (CaseProject item : cases) {
            if (!sceneIdsByCase.containsKey(item.getId()) && item.getSceneId() != null) {
                sceneIdsByCase.put(item.getId(), List.of(item.getSceneId()));
            }
        }
        Set<Long> sceneIds = sceneIdsByCase.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        if (sceneIds.isEmpty()) return Map.of();
        Map<Long, ApplicationScene> scenes = applicationSceneMapper.selectBatchIds(sceneIds).stream()
                .filter(scene -> PUBLISHED.equals(scene.getStatus()))
                .collect(Collectors.toMap(ApplicationScene::getId, Function.identity()));
        Map<Long, ApplicationScene> result = new LinkedHashMap<>();
        sceneIdsByCase.forEach((caseId, ids) -> ids.stream().map(scenes::get).filter(Objects::nonNull).findFirst()
                .ifPresent(scene -> result.put(caseId, scene)));
        return result;
    }

    private List<PublicRelatedProductResponse> relatedProducts(Long caseId) {
        List<ProductCaseRel> relations = productCaseRelMapper.selectList(new LambdaQueryWrapper<ProductCaseRel>()
                .eq(ProductCaseRel::getCaseId, caseId).orderByAsc(ProductCaseRel::getSortOrder));
        Set<Long> productIds = relations.stream().map(ProductCaseRel::getProductId).collect(Collectors.toSet());
        if (productIds.isEmpty()) return List.of();
        Map<Long, Product> products = productMapper.selectBatchIds(productIds).stream()
                .filter(product -> PUBLISHED.equals(product.getStatus()))
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return relations.stream().map(ProductCaseRel::getProductId).map(products::get).filter(Objects::nonNull)
                .map(product -> new PublicRelatedProductResponse(product.getId(), product.getName(), product.getSlug())).toList();
    }

    private PublicApplicationSceneResponse toSceneResponse(ApplicationScene item) {
        return new PublicApplicationSceneResponse(item.getId(), item.getSlug(), item.getName(), item.getSlogan(),
                item.getSummary(), mediaUrl(item.getCoverMediaId()));
    }

    private PublicCaseCategoryResponse toCategoryResponse(CaseCategory item) {
        return new PublicCaseCategoryResponse(item.getId(), item.getSlug(), item.getName(), item.getSummary(), item.getSortOrder());
    }

    private PublicCaseListItemResponse toCaseListItem(CaseProject item, CaseCategory category, ApplicationScene scene) {
        return new PublicCaseListItemResponse(item.getId(), item.getSlug(), item.getTitle(),
                category == null ? null : category.getName(), category == null ? null : category.getSlug(),
                scene == null ? null : scene.getName(), scene == null ? null : scene.getSlug(), item.getLocation(),
                item.getSummary(), mediaUrl(item.getCoverMediaId()), Integer.valueOf(1).equals(item.getIsFeatured()), item.getSortOrder());
    }

    private String mediaUrl(Long mediaId) {
        return mediaId == null ? null : "/api/v1/public/media/" + mediaId;
    }
}

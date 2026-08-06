package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.PublicApplicationSceneResponse;
import com.alfy.api.dto.PublicCaseDetailResponse;
import com.alfy.api.dto.PublicCaseListItemResponse;
import com.alfy.api.dto.PublicRelatedProductResponse;
import com.alfy.api.dto.SeoMetaResponse;
import com.alfy.api.entity.ApplicationScene;
import com.alfy.api.entity.CaseProject;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.ProductCaseRel;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ApplicationSceneMapper;
import com.alfy.api.mapper.CaseProjectMapper;
import com.alfy.api.mapper.ProductCaseRelMapper;
import com.alfy.api.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
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
    private final CaseProjectMapper caseProjectMapper;
    private final ProductCaseRelMapper productCaseRelMapper;
    private final ProductMapper productMapper;

    public List<PublicApplicationSceneResponse> listScenes() {
        return applicationSceneMapper.selectList(new LambdaQueryWrapper<ApplicationScene>()
                        .eq(ApplicationScene::getStatus, PUBLISHED)
                        .orderByAsc(ApplicationScene::getSortOrder)
                        .orderByAsc(ApplicationScene::getId))
                .stream().map(this::toSceneResponse).toList();
    }

    public Page<PublicCaseListItemResponse> listCases(
            String sceneSlug, String productSlug, Boolean featured, long pageNumber, long pageSize) {
        Set<Long> publishedSceneIds = applicationSceneMapper.selectList(new LambdaQueryWrapper<ApplicationScene>()
                        .select(ApplicationScene::getId)
                        .eq(ApplicationScene::getStatus, PUBLISHED))
                .stream().map(ApplicationScene::getId).collect(Collectors.toSet());
        if (publishedSceneIds.isEmpty()) {
            return new Page<PublicCaseListItemResponse>(pageNumber, pageSize, 0);
        }
        Long sceneId = resolveSceneId(sceneSlug);
        Set<Long> caseIds = resolveCaseIdsByProduct(productSlug);
        if ((sceneId != null && !publishedSceneIds.contains(sceneId)) || (caseIds != null && caseIds.isEmpty())) {
            return new Page<PublicCaseListItemResponse>(pageNumber, pageSize, 0);
        }

        Page<CaseProject> page = caseProjectMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<CaseProject>()
                        .eq(CaseProject::getStatus, PUBLISHED)
                        .in(CaseProject::getSceneId, publishedSceneIds)
                        .eq(sceneId != null, CaseProject::getSceneId, sceneId)
                        .eq(featured != null, CaseProject::getIsFeatured, Boolean.TRUE.equals(featured) ? 1 : 0)
                        .in(caseIds != null, CaseProject::getId, caseIds)
                        .orderByDesc(CaseProject::getIsFeatured)
                        .orderByAsc(CaseProject::getSortOrder)
                        .orderByDesc(CaseProject::getPublishedAt)
                        .orderByDesc(CaseProject::getId));
        Map<Long, ApplicationScene> scenes = scenesById(page.getRecords());
        Page<PublicCaseListItemResponse> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(item -> toCaseListItem(item, scenes.get(item.getSceneId())))
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
        ApplicationScene scene = applicationSceneMapper.selectById(item.getSceneId());
        if (scene == null || !PUBLISHED.equals(scene.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "案例所属场景不存在或尚未发布");
        }
        return new PublicCaseDetailResponse(
                item.getId(), item.getSlug(), item.getTitle(), scene.getName(), scene.getSlug(), item.getLocation(),
                item.getSummary(), item.getBackground(), item.getCustomerNeed(), item.getSolution(), item.getImplementation(),
                item.getResultSummary(), item.getContentHtml(), mediaUrl(item.getCoverMediaId()), relatedProducts(item.getId()),
                new SeoMetaResponse(item.getSeoTitle(), item.getSeoDescription(), item.getSeoKeywords()));
    }

    private Long resolveSceneId(String sceneSlug) {
        if (sceneSlug == null || sceneSlug.isBlank()) {
            return null;
        }
        ApplicationScene scene = applicationSceneMapper.selectOne(new LambdaQueryWrapper<ApplicationScene>()
                .eq(ApplicationScene::getSlug, sceneSlug).eq(ApplicationScene::getStatus, PUBLISHED));
        return scene == null ? -1L : scene.getId();
    }

    private Set<Long> resolveCaseIdsByProduct(String productSlug) {
        if (productSlug == null || productSlug.isBlank()) {
            return null;
        }
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getSlug, productSlug).eq(Product::getStatus, PUBLISHED));
        if (product == null) {
            return Collections.emptySet();
        }
        return productCaseRelMapper.selectList(new LambdaQueryWrapper<ProductCaseRel>()
                        .eq(ProductCaseRel::getProductId, product.getId()))
                .stream().map(ProductCaseRel::getCaseId).collect(Collectors.toSet());
    }

    private Map<Long, ApplicationScene> scenesById(Collection<CaseProject> cases) {
        Set<Long> ids = cases.stream().map(CaseProject::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return applicationSceneMapper.selectBatchIds(ids).stream()
                .filter(scene -> PUBLISHED.equals(scene.getStatus()))
                .collect(Collectors.toMap(ApplicationScene::getId, Function.identity()));
    }

    private List<PublicRelatedProductResponse> relatedProducts(Long caseId) {
        List<ProductCaseRel> relations = productCaseRelMapper.selectList(new LambdaQueryWrapper<ProductCaseRel>()
                .eq(ProductCaseRel::getCaseId, caseId).orderByAsc(ProductCaseRel::getSortOrder));
        Set<Long> productIds = relations.stream().map(ProductCaseRel::getProductId).collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return List.of();
        }
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

    private PublicCaseListItemResponse toCaseListItem(CaseProject item, ApplicationScene scene) {
        return new PublicCaseListItemResponse(item.getId(), item.getSlug(), item.getTitle(),
                scene == null ? null : scene.getName(), scene == null ? null : scene.getSlug(), item.getLocation(),
                item.getSummary(), mediaUrl(item.getCoverMediaId()), Integer.valueOf(1).equals(item.getIsFeatured()), item.getSortOrder());
    }

    private String mediaUrl(Long mediaId) {
        return mediaId == null ? null : "/api/v1/public/media/" + mediaId;
    }
}

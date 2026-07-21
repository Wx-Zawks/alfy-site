package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.dto.PublicProductCategoryResponse;
import com.alfy.api.dto.PublicProductDetailResponse;
import com.alfy.api.dto.PublicProductListItemResponse;
import com.alfy.api.dto.SeoMetaResponse;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.ProductCategory;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ProductCategoryMapper;
import com.alfy.api.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 官网公开产品读取。严格过滤 PUBLISHED，草稿和下线产品不会被前端读取。 */
@Service
@RequiredArgsConstructor
public class PublicProductService {

    private static final String PUBLISHED = "PUBLISHED";

    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ObjectMapper objectMapper;

    public List<PublicProductCategoryResponse> listCategories() {
        return productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getStatus, 1)
                        .orderByAsc(ProductCategory::getSortOrder)
                        .orderByAsc(ProductCategory::getId))
                .stream().map(this::toCategoryResponse).toList();
    }

    public Page<PublicProductListItemResponse> listProducts(
            String categorySlug, Boolean featured, long pageNumber, long pageSize) {
        ProductCategory category = categorySlug == null || categorySlug.isBlank() ? null : requireActiveCategory(categorySlug);
        Page<Product> productPage = productMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, PUBLISHED)
                        .eq(category != null, Product::getCategoryId, category == null ? null : category.getId())
                        .eq(featured != null, Product::getIsFeatured, Boolean.TRUE.equals(featured) ? 1 : 0)
                        .orderByAsc(Product::getSortOrder)
                        .orderByDesc(Product::getPublishedAt)
                        .orderByDesc(Product::getId));
        Map<Long, ProductCategory> categories = categoriesById(productPage.getRecords());
        List<PublicProductListItemResponse> records = productPage.getRecords().stream()
                .map(product -> toListResponse(product, categories.get(product.getCategoryId())))
                .toList();
        Page<PublicProductListItemResponse> response = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        response.setRecords(records);
        return response;
    }

    public PublicProductDetailResponse getProduct(String slug) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getSlug, slug)
                .eq(Product::getStatus, PUBLISHED));
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在或尚未发布");
        }
        ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品分类不存在或已停用");
        }
        return new PublicProductDetailResponse(product.getId(), product.getName(), product.getSlug(), category.getSlug(),
                category.getName(), product.getSummary(), product.getContentHtml(), mediaUrl(product.getCoverMediaId()),
                parseFeatures(product.getFeaturesJson()), parseJson(product.getSpecificationsJson()),
                new SeoMetaResponse(product.getSeoTitle(), product.getSeoDescription(), product.getSeoKeywords()));
    }

    private ProductCategory requireActiveCategory(String slug) {
        ProductCategory category = productCategoryMapper.selectOne(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getSlug, slug)
                .eq(ProductCategory::getStatus, 1));
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品分类不存在或已停用");
        }
        return category;
    }

    private Map<Long, ProductCategory> categoriesById(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return productCategoryMapper.selectBatchIds(categoryIds)
                .stream().collect(Collectors.toMap(ProductCategory::getId, item -> item));
    }

    private PublicProductCategoryResponse toCategoryResponse(ProductCategory category) {
        return new PublicProductCategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getSummary(),
                mediaUrl(category.getCoverMediaId()), category.getSortOrder());
    }

    private PublicProductListItemResponse toListResponse(Product product, ProductCategory category) {
        return new PublicProductListItemResponse(product.getId(), product.getName(), product.getSlug(),
                category == null ? null : category.getSlug(), category == null ? null : category.getName(), product.getSummary(),
                mediaUrl(product.getCoverMediaId()), parseFeatures(product.getFeaturesJson()));
    }

    private List<String> parseFeatures(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String mediaUrl(Long mediaId) {
        return mediaId == null ? null : "/api/v1/public/media/" + mediaId;
    }
}

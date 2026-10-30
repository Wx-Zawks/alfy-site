package com.alfy.api.service;

import com.alfy.api.dto.SeoRouteSettingResponse;
import com.alfy.api.dto.SeoUrlResponse;
import com.alfy.api.entity.Article;
import com.alfy.api.entity.CaseProject;
import com.alfy.api.entity.ContentPage;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.TechnologyPage;
import com.alfy.api.mapper.ArticleMapper;
import com.alfy.api.mapper.CaseProjectMapper;
import com.alfy.api.mapper.ContentPageMapper;
import com.alfy.api.mapper.ProductMapper;
import com.alfy.api.mapper.TechnologyPageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeoUrlInventoryService {

    private static final String PUBLISHED = "PUBLISHED";
    private static final Set<String> TECHNOLOGY_DETAIL_KEYS = Set.of(
            "aerogel-material", "aerogel-composite", "other"
    );
    private static final Map<String, String> CONTENT_PAGE_PATHS = Map.of(
            "cooperation-dealer", "/cooperation/dealer",
            "cooperation-product-development", "/cooperation/product-development",
            "cooperation-industry-chain", "/cooperation/industry-chain"
    );

    private final SeoSettingService seoSettingService;
    private final ProductMapper productMapper;
    private final ArticleMapper articleMapper;
    private final CaseProjectMapper caseProjectMapper;
    private final TechnologyPageMapper technologyPageMapper;
    private final ContentPageMapper contentPageMapper;

    public List<SeoUrlResponse> listPublicUrls() {
        if (!seoSettingService.getPublicGlobal().publicIndexEnabled()) return List.of();

        Map<String, SeoUrlResponse> urls = new LinkedHashMap<>();
        for (SeoRouteSettingResponse route : seoSettingService.listPublicRoutes()) {
            if (route.indexEnabled()) {
                add(urls, new SeoUrlResponse(route.routePath(), route.updatedAt(), "route"));
            }
        }

        productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, PUBLISHED)
                        .isNotNull(Product::getSlug))
                .forEach(item -> add(urls, new SeoUrlResponse(
                        "/products/" + item.getSlug(), item.getUpdatedAt(), "product")));

        articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, PUBLISHED)
                        .isNotNull(Article::getSlug))
                .forEach(item -> add(urls, new SeoUrlResponse(
                        "/news/" + item.getSlug(), item.getUpdatedAt(), "article")));

        caseProjectMapper.selectList(new LambdaQueryWrapper<CaseProject>()
                        .eq(CaseProject::getStatus, PUBLISHED)
                        .isNotNull(CaseProject::getSlug))
                .forEach(item -> add(urls, new SeoUrlResponse(
                        "/cases/" + item.getSlug(), item.getUpdatedAt(), "case")));

        technologyPageMapper.selectList(new LambdaQueryWrapper<TechnologyPage>()
                        .eq(TechnologyPage::getStatus, PUBLISHED)
                        .in(TechnologyPage::getPageKey, TECHNOLOGY_DETAIL_KEYS))
                .forEach(item -> add(urls, new SeoUrlResponse(
                        "/technology/" + item.getPageKey(), item.getUpdatedAt(), "technology")));

        contentPageMapper.selectList(new LambdaQueryWrapper<ContentPage>()
                        .eq(ContentPage::getStatus, PUBLISHED)
                        .in(ContentPage::getPageKey, CONTENT_PAGE_PATHS.keySet()))
                .forEach(item -> add(urls, new SeoUrlResponse(
                        CONTENT_PAGE_PATHS.get(item.getPageKey()), item.getUpdatedAt(), "content-page")));

        List<SeoUrlResponse> result = new ArrayList<>(urls.values());
        result.sort(Comparator.comparing(SeoUrlResponse::loc));
        return result;
    }

    private void add(Map<String, SeoUrlResponse> urls, SeoUrlResponse value) {
        if (value.loc() == null || !value.loc().startsWith("/") || value.loc().contains("?")) return;
        urls.put(value.loc(), value);
    }
}

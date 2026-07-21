package com.alfy.api.service;

import com.alfy.api.common.ErrorCode;
import com.alfy.api.entity.Article;
import com.alfy.api.entity.ArticleMedia;
import com.alfy.api.entity.ApplicationScene;
import com.alfy.api.entity.CaseProject;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.entity.Product;
import com.alfy.api.entity.HeroSlide;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.ArticleMapper;
import com.alfy.api.mapper.ArticleMediaMapper;
import com.alfy.api.mapper.ApplicationSceneMapper;
import com.alfy.api.mapper.CaseProjectMapper;
import com.alfy.api.mapper.MediaAssetMapper;
import com.alfy.api.mapper.ProductMapper;
import com.alfy.api.mapper.HeroSlideMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** 统一控制公开媒体：只有被已发布内容引用的文件才可读取。 */
@Service
@RequiredArgsConstructor
public class PublicMediaService {

    private static final String PUBLISHED = "PUBLISHED";

    private final MediaAssetMapper mediaAssetMapper;
    private final ArticleMediaMapper articleMediaMapper;
    private final ArticleMapper articleMapper;
    private final ProductMapper productMapper;
    private final ApplicationSceneMapper applicationSceneMapper;
    private final CaseProjectMapper caseProjectMapper;
    private final HeroSlideMapper heroSlideMapper;

    public MediaAsset getPublicMedia(Long mediaId) {
        MediaAsset media = mediaAssetMapper.selectById(mediaId);
        if (media == null || (!belongsToPublishedArticle(mediaId) && !belongsToPublishedProduct(mediaId)
                && !belongsToPublishedScene(mediaId) && !belongsToPublishedCase(mediaId) && !belongsToPublishedHeroSlide(mediaId))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "媒体资源不存在或尚未公开");
        }
        return media;
    }

    private boolean belongsToPublishedArticle(Long mediaId) {
        Set<Long> articleIds = articleMediaMapper.selectList(new LambdaQueryWrapper<ArticleMedia>()
                        .eq(ArticleMedia::getMediaId, mediaId))
                .stream().map(ArticleMedia::getArticleId).collect(Collectors.toSet());
        return !articleIds.isEmpty() && articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .in(Article::getId, articleIds).eq(Article::getStatus, PUBLISHED)) > 0;
    }

    private boolean belongsToPublishedProduct(Long mediaId) {
        return productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCoverMediaId, mediaId)
                .eq(Product::getStatus, PUBLISHED)) > 0;
    }

    private boolean belongsToPublishedScene(Long mediaId) {
        return applicationSceneMapper.selectCount(new LambdaQueryWrapper<ApplicationScene>()
                .eq(ApplicationScene::getCoverMediaId, mediaId)
                .eq(ApplicationScene::getStatus, PUBLISHED)) > 0;
    }

    private boolean belongsToPublishedCase(Long mediaId) {
        return caseProjectMapper.selectCount(new LambdaQueryWrapper<CaseProject>()
                .eq(CaseProject::getCoverMediaId, mediaId)
                .eq(CaseProject::getStatus, PUBLISHED)) > 0;
    }

    private boolean belongsToPublishedHeroSlide(Long mediaId) {
        LocalDateTime now = LocalDateTime.now();
        return heroSlideMapper.selectCount(new LambdaQueryWrapper<HeroSlide>()
                .and(q -> q.eq(HeroSlide::getDesktopMediaId, mediaId).or().eq(HeroSlide::getMobileMediaId, mediaId))
                .eq(HeroSlide::getStatus, PUBLISHED)
                .and(q -> q.isNull(HeroSlide::getStartsAt).or().le(HeroSlide::getStartsAt, now))
                .and(q -> q.isNull(HeroSlide::getEndsAt).or().gt(HeroSlide::getEndsAt, now))) > 0;
    }
}

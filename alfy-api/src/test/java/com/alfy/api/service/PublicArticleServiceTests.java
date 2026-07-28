package com.alfy.api.service;

import com.alfy.api.dto.ArticleListItemResponse;
import com.alfy.api.entity.Article;
import com.alfy.api.mapper.ArticleCategoryMapper;
import com.alfy.api.mapper.ArticleCategoryRelationMapper;
import com.alfy.api.mapper.ArticleMapper;
import com.alfy.api.mapper.ArticleMediaMapper;
import com.alfy.api.mapper.MediaAssetMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicArticleServiceTests {

    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleCategoryMapper articleCategoryMapper = mock(ArticleCategoryMapper.class);
    private final ArticleCategoryRelationMapper articleCategoryRelationMapper = mock(ArticleCategoryRelationMapper.class);
    private final ArticleMediaMapper articleMediaMapper = mock(ArticleMediaMapper.class);
    private final MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
    private final PublicArticleService service = new PublicArticleService(
            articleMapper, articleCategoryMapper, articleCategoryRelationMapper, articleMediaMapper, mediaAssetMapper);

    @Test
    void placesPrimaryHomeArticleBeforeNormallySortedArticles() {
        Article firstSorted = article(1L, "NEWS_LIST", 1);
        Article primary = article(2L, "NEWS_PRIMARY", 20);
        Article secondSorted = article(3L, "NEWS_LIST", 30);
        when(articleMapper.selectList(any())).thenReturn(List.of(firstSorted, primary, secondSorted));
        when(articleCategoryRelationMapper.selectList(any())).thenReturn(List.of());

        List<ArticleListItemResponse> result = service.listHomeArticles(4);

        assertThat(result).extracting(ArticleListItemResponse::id).containsExactly(2L, 1L, 3L);
        assertThat(result.get(0).homeSlot()).isEqualTo("NEWS_PRIMARY");
    }

    private Article article(Long id, String homeSlot, int homeSortOrder) {
        Article article = new Article();
        article.setId(id);
        article.setSlug("article-" + id);
        article.setTitle("Article " + id);
        article.setHomeSlot(homeSlot);
        article.setHomeSortOrder(homeSortOrder);
        article.setPublishedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        return article;
    }
}

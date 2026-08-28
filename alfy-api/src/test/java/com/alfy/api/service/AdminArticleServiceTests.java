package com.alfy.api.service;

import com.alfy.api.dto.AdminArticleResponse;
import com.alfy.api.dto.AdminArticleUpsertRequest;
import com.alfy.api.entity.Article;
import com.alfy.api.entity.ArticleMedia;
import com.alfy.api.entity.MediaAsset;
import com.alfy.api.mapper.ArticleCategoryMapper;
import com.alfy.api.mapper.ArticleCategoryRelationMapper;
import com.alfy.api.mapper.ArticleMapper;
import com.alfy.api.mapper.ArticleMediaMapper;
import com.alfy.api.mapper.MediaAssetMapper;
import com.alfy.api.security.AdminPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminArticleServiceTests {

    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final ArticleCategoryMapper articleCategoryMapper = mock(ArticleCategoryMapper.class);
    private final ArticleCategoryRelationMapper articleCategoryRelationMapper =
            mock(ArticleCategoryRelationMapper.class);
    private final ArticleMediaMapper articleMediaMapper = mock(ArticleMediaMapper.class);
    private final MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
    private final AdminOperationLogService operationLogService = mock(AdminOperationLogService.class);
    private final AdminArticleService service = new AdminArticleService(
            articleMapper,
            articleCategoryMapper,
            articleCategoryRelationMapper,
            articleMediaMapper,
            mediaAssetMapper,
            operationLogService,
            new HtmlSanitizer()
    );

    @Test
    void createsInlineMediaRelationFromManagedPlaceholder() {
        MediaAsset image = image(42L, "2026-07-29/news.jpg");
        AtomicReference<Article> savedArticle = new AtomicReference<>();
        when(articleMapper.selectOne(any())).thenReturn(null);
        when(articleCategoryMapper.selectCount(any())).thenReturn(1L);
        when(mediaAssetMapper.selectBatchIds(anyCollection())).thenReturn(List.of(image));
        when(articleMapper.insert(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(7L);
            article.setVersion(0L);
            savedArticle.set(article);
            return 1;
        });
        when(articleMapper.selectById(7L)).thenAnswer(ignored -> savedArticle.get());
        when(articleCategoryRelationMapper.selectList(any())).thenReturn(List.of());
        when(articleMediaMapper.selectList(any())).thenReturn(List.of());

        AdminArticleUpsertRequest request = new AdminArticleUpsertRequest(
                "新闻标题",
                "news-title",
                "新闻摘要",
                "<p>正文</p><figure><img src=\"alfy-media:42\" alt=\"现场\"></figure>",
                null,
                null,
                "编辑",
                null,
                null,
                List.of(3L),
                0,
                false,
                null,
                0,
                null,
                null,
                null,
                null
        );

        service.create(request, new AdminPrincipal(1L, "admin", "content_admin"));

        ArgumentCaptor<ArticleMedia> relationCaptor = ArgumentCaptor.forClass(ArticleMedia.class);
        verify(articleMediaMapper).deleteInlineByArticleId(7L);
        verify(articleMediaMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getArticleId()).isEqualTo(7L);
        assertThat(relationCaptor.getValue().getMediaId()).isEqualTo(42L);
        assertThat(relationCaptor.getValue().getUsageType()).isEqualTo("INLINE");
        assertThat(savedArticle.get().getContentHtml()).contains("src=\"alfy-media:42\"");
    }

    @Test
    void createsInlineVideoRelationFromManagedPlaceholder() {
        MediaAsset video = new MediaAsset();
        video.setId(3L);
        video.setMediaType("VIDEO");
        video.setStorageKey("2026-07-29/event.mp4");
        video.setOriginalFilename("event.mp4");
        AtomicReference<Article> savedArticle = new AtomicReference<>();
        when(articleMapper.selectOne(any())).thenReturn(null);
        when(articleCategoryMapper.selectCount(any())).thenReturn(1L);
        when(mediaAssetMapper.selectBatchIds(anyCollection())).thenReturn(List.of(video));
        when(articleMapper.insert(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(9L);
            article.setVersion(0L);
            savedArticle.set(article);
            return 1;
        });
        when(articleMapper.selectById(9L)).thenAnswer(ignored -> savedArticle.get());
        when(articleCategoryRelationMapper.selectList(any())).thenReturn(List.of());
        when(articleMediaMapper.selectList(any())).thenReturn(List.of());

        AdminArticleUpsertRequest request = new AdminArticleUpsertRequest(
                "视频新闻", "video-news", "摘要",
                "<figure><video controls preload=\"metadata\"><source src=\"alfy-media:3\" type=\"video/mp4\"></video></figure>",
                null, null, "编辑", null, null, List.of(3L), 0, false,
                null, 0, null, null, null, null
        );

        service.create(request, new AdminPrincipal(1L, "admin", "content_admin"));

        ArgumentCaptor<ArticleMedia> relationCaptor = ArgumentCaptor.forClass(ArticleMedia.class);
        verify(articleMediaMapper).insert(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getMediaId()).isEqualTo(3L);
        assertThat(savedArticle.get().getContentHtml()).contains("src=\"alfy-media:3\"");
    }

    @Test
    void convertsImportedStorageKeyToEditablePlaceholder() {
        Article article = new Article();
        article.setId(8L);
        article.setTitle("已导入新闻");
        article.setSlug("imported-news");
        article.setContentHtml("<figure><img src=\"2026-07-20/imported.jpg\" alt=\"现场\"></figure>");

        ArticleMedia relation = new ArticleMedia();
        relation.setArticleId(8L);
        relation.setMediaId(55L);
        relation.setUsageType("INLINE");
        relation.setSortOrder(0);
        MediaAsset image = image(55L, "2026-07-20/imported.jpg");

        when(articleMapper.selectById(8L)).thenReturn(article);
        when(articleMediaMapper.selectList(any())).thenReturn(List.of(relation));
        when(mediaAssetMapper.selectBatchIds(anyCollection())).thenReturn(List.of(image));
        when(articleCategoryRelationMapper.selectList(any())).thenReturn(List.of());

        AdminArticleResponse response = service.get(8L);

        assertThat(response.contentHtml())
                .contains("src=\"alfy-media:55\"")
                .doesNotContain("src=\"2026-07-20/imported.jpg\"");
    }

    private MediaAsset image(Long id, String storageKey) {
        MediaAsset image = new MediaAsset();
        image.setId(id);
        image.setMediaType("IMAGE");
        image.setStorageKey(storageKey);
        image.setOriginalFilename("news.jpg");
        return image;
    }
}

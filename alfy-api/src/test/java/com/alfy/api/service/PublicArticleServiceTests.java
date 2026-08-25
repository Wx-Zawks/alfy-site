package com.alfy.api.service;

import com.alfy.api.entity.Article;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicArticleServiceTests {

    @Test
    void listSummaryFallsBackToContentTextWithoutRequestingArticleDetail() {
        Article article = new Article();
        article.setTitle("测试新闻");
        article.setContentText("测试新闻   这是正文第一段，应该直接作为列表摘要。\n第二段");

        assertThat(PublicArticleService.summaryOf(article))
                .isEqualTo("这是正文第一段，应该直接作为列表摘要。 第二段");

        article.setSummary("  人工摘要  ");
        assertThat(PublicArticleService.summaryOf(article)).isEqualTo("人工摘要");
    }
}

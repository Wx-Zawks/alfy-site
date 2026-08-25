package com.alfy.api.controller;

import com.alfy.api.service.HeroSlideService;
import com.alfy.api.service.PublicApplicationCaseService;
import com.alfy.api.service.PublicArticleService;
import com.alfy.api.service.TechnologyPageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicHomeControllerTests {

    @Test
    void requestsFiveArticlesForTheHomepage() {
        HeroSlideService heroSlideService = mock(HeroSlideService.class);
        TechnologyPageService technologyPageService = mock(TechnologyPageService.class);
        PublicApplicationCaseService applicationCaseService = mock(PublicApplicationCaseService.class);
        PublicArticleService articleService = mock(PublicArticleService.class);
        when(applicationCaseService.listCases(null, null, true, 1, 4)).thenReturn(new Page<>());

        PublicHomeController controller = new PublicHomeController(
                heroSlideService, technologyPageService, applicationCaseService, articleService);

        controller.home();

        verify(articleService).listHomeArticles(5);
    }
}

package com.alfy.api.service;

import com.alfy.api.dto.TechnologyPageResponse;
import com.alfy.api.entity.TechnologyPage;
import com.alfy.api.mapper.TechnologyPageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TechnologyPageServiceTests {

    @Test
    void resolvesManagedInlineImagesToPublicMediaUrls() {
        TechnologyPage page = new TechnologyPage();
        page.setId(2L);
        page.setPageKey("aerogel-material");
        page.setTitle("气凝胶材料技术");
        page.setSummary("技术简介");
        page.setStatus("PUBLISHED");
        page.setContentHtml("<figure><img src=\"alfy-media:628\" alt=\"技术图\"></figure>");

        TechnologyPageMapper mapper = mock(TechnologyPageMapper.class);
        when(mapper.selectOne(any())).thenReturn(page);
        TechnologyPageService service = new TechnologyPageService(
                mapper, mock(AdminOperationLogService.class), new HtmlSanitizer(), new ObjectMapper());

        TechnologyPageResponse response = service.getPublic("aerogel-material");

        assertThat(response.contentHtml())
                .contains("src=\"/api/v1/public/media/628\"")
                .doesNotContain("alfy-media:");
    }
}

package com.alfy.api.service;

import com.alfy.api.dto.SeoGlobalSettingUpsertRequest;
import com.alfy.api.dto.SeoRouteSettingUpsertRequest;
import com.alfy.api.entity.SeoGlobalSetting;
import com.alfy.api.entity.SeoRouteSetting;
import com.alfy.api.exception.BusinessException;
import com.alfy.api.mapper.SeoGlobalSettingMapper;
import com.alfy.api.mapper.SeoRouteSettingMapper;
import com.alfy.api.security.AdminPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoSettingServiceTests {

    @Mock
    private SeoGlobalSettingMapper globalMapper;
    @Mock
    private SeoRouteSettingMapper routeMapper;
    @Mock
    private AdminOperationLogService operationLogService;

    private SeoSettingService service;
    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new SeoSettingService(globalMapper, routeMapper, operationLogService, new ObjectMapper());
        principal = new AdminPrincipal(1L, "admin", "SUPER_ADMIN");
    }

    @Test
    void rejectsCanonicalOriginWithPath() {
        SeoGlobalSettingUpsertRequest request = validGlobalRequest("https://alfy.com.cn/path");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.saveGlobal(request, principal));

        assertEquals("正式域名必须是无路径、查询参数和片段的 HTTPS 地址，例如 https://alfy.com.cn",
                exception.getMessage());
    }

    @Test
    void savesGlobalSettingWithOptimisticVersion() {
        SeoGlobalSetting entity = new SeoGlobalSetting();
        entity.setId(1L);
        entity.setCanonicalOrigin("https://alfy.com.cn");
        entity.setPublicIndexEnabled(1);
        entity.setVersion(2L);
        when(globalMapper.selectOne(any())).thenReturn(entity);
        when(globalMapper.updateById(entity)).thenReturn(1);

        var response = service.saveGlobal(validGlobalRequest("https://alfy.com.cn"), principal);

        assertEquals("%s｜奥飞新材", response.titleTemplate());
        assertEquals(List.of("https://example.com/alfy"), response.sameAs());
        verify(globalMapper).updateById(entity);
        verify(operationLogService).record(1L, "UPDATE", "SEO_GLOBAL_SETTING", 1L,
                "更新全局 SEO 配置");
    }

    @Test
    void savesOnlyEditableRouteFields() {
        SeoRouteSetting entity = new SeoRouteSetting();
        entity.setId(7L);
        entity.setRouteKey("about-index");
        entity.setRoutePath("/about");
        entity.setDisplayName("关于我们");
        entity.setVersion(3L);
        when(routeMapper.selectOne(any())).thenReturn(entity);
        when(routeMapper.updateById(entity)).thenReturn(1);
        when(routeMapper.selectById(7L)).thenReturn(entity);

        var response = service.saveRoute("about-index",
                new SeoRouteSettingUpsertRequest("关于奥飞", "公司介绍", "奥飞新材", 9L, true, 3L),
                principal);

        assertEquals("/about", response.routePath());
        assertEquals("关于奥飞", response.seoTitle());
        assertEquals(9L, response.ogMediaId());
        verify(operationLogService).record(1L, "UPDATE", "SEO_ROUTE_SETTING", 7L,
                "更新固定页面 SEO：关于我们");
    }

    private SeoGlobalSettingUpsertRequest validGlobalRequest(String canonicalOrigin) {
        return new SeoGlobalSettingUpsertRequest(
                canonicalOrigin,
                "%s｜奥飞新材",
                "奥飞新材",
                "湖南奥飞新材料有限公司官方网站。",
                null,
                "湖南奥飞新材料有限公司",
                "奥飞新材",
                "湖南奥飞新材料有限公司",
                "公司简介",
                null,
                List.of("https://example.com/alfy"),
                "",
                "",
                "",
                false,
                "",
                false,
                "",
                false,
                "",
                true,
                "allow",
                "allow",
                "allow",
                2L
        );
    }
}

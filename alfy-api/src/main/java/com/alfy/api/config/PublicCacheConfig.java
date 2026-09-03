package com.alfy.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 公开查询接口（/api/v1/public/**）统一附加 30 秒 HTTP 短缓存。
 * 官网前端自身还有 30 秒客户端缓存与 120 秒自动刷新，CMS 发布后最长约 1 分钟生效，
 * 换来的是热点页面不再每次访问都实时查库。媒体文件接口自带 30 天缓存，不受影响。
 */
@Component
public class PublicCacheConfig implements WebMvcConfigurer {

    private static final String PUBLIC_CACHE_VALUE = "public, max-age=30";

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    @NonNull HttpServletRequest request,
                    @NonNull HttpServletResponse response,
                    @NonNull Object handler
            ) {
                if (HttpMethod.GET.matches(request.getMethod())) {
                    response.setHeader(HttpHeaders.CACHE_CONTROL, PUBLIC_CACHE_VALUE);
                }
                return true;
            }
        }).addPathPatterns("/api/v1/public/**")
          .excludePathPatterns("/api/v1/public/media/**");
    }
}

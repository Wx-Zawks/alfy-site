package com.alfy.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 让路径/查询参数在绑定到 Long / Integer 时容忍：
 * 1. 首尾空白（如 "3 "、" 3"）
 * 2. 全角数字与全角负号（如 ３、－1）
 * 真实非法输入（如 "abc"）仍会抛异常并由 GlobalExceptionHandler 转成 400。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Long.class, new StringToLongConverter());
        registry.addConverter(String.class, Integer.class, new StringToIntegerConverter());
    }

    static class StringToLongConverter implements Converter<String, Long> {
        @Override
        public Long convert(String source) {
            if (source == null) {
                return null;
            }
            return Long.valueOf(normalize(source));
        }
    }

    static class StringToIntegerConverter implements Converter<String, Integer> {
        @Override
        public Integer convert(String source) {
            if (source == null) {
                return null;
            }
            return Integer.valueOf(normalize(source));
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            } else if (c == '－') {
                sb.append('-');
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}

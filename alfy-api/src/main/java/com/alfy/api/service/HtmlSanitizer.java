package com.alfy.api.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/** 对后台富文本做服务端白名单清洗，前端仍应只以安全样式渲染。 */
@Component
public class HtmlSanitizer {

    private static final Safelist ARTICLE_SAFE_LIST = Safelist.relaxed()
            .addTags("figure", "figcaption", "video", "source")
            .addAttributes("video", "controls", "poster", "preload")
            .addAttributes("source", "src", "type")
            .addAttributes("img", "src", "alt", "title")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String clean(String html) {
        return html == null ? null : Jsoup.clean(html, ARTICLE_SAFE_LIST);
    }

    public String toPlainText(String html) {
        return html == null ? null : Jsoup.parse(clean(html)).text();
    }
}

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
            .addAttributes("img", "src", "alt", "title", "loading")
            .addAttributes("span", "data-font", "data-size", "data-color", "data-highlight")
            .addAttributes("p", "data-align")
            .addAttributes("div", "data-align")
            .addAttributes("blockquote", "data-align")
            .addAttributes("li", "data-align")
            .addAttributes("h1", "data-align")
            .addAttributes("h2", "data-align")
            .addAttributes("h3", "data-align")
            .addAttributes("h4", "data-align")
            .addAttributes("h5", "data-align")
            .addAttributes("h6", "data-align")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "alfy-media");

    public String clean(String html) {
        return html == null ? null : Jsoup.clean(html, ARTICLE_SAFE_LIST);
    }

    public String toPlainText(String html) {
        return html == null ? null : Jsoup.parse(clean(html)).text();
    }
}

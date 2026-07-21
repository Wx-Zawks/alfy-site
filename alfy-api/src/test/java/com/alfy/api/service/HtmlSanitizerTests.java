package com.alfy.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTests {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void removesScriptsAndEventHandlersButKeepsArticleStructure() {
        String clean = sanitizer.clean("<p onclick=\"alert(1)\">正文</p><script>alert(2)</script><figure><img src=\"/media/1\" alt=\"图\"></figure>");

        assertThat(clean).contains("<p>正文</p>").contains("<figure>").contains("alt=\"图\"")
                .doesNotContain("script").doesNotContain("onclick");
    }
}

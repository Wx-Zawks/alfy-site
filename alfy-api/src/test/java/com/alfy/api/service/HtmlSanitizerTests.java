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

    @Test
    void keepsManagedInlineImagePlaceholder() {
        String clean = sanitizer.clean("""
                <figure>
                  <img src="alfy-media:42" alt="签约现场" loading="lazy">
                  <figcaption>签约仪式现场</figcaption>
                </figure>
                """);

        assertThat(clean)
                .contains("src=\"alfy-media:42\"")
                .contains("alt=\"签约现场\"")
                .contains("loading=\"lazy\"")
                .contains("<figcaption>")
                .contains("签约仪式现场");
    }

    @Test
    void keepsManagedInlineVideoPlaceholder() {
        String clean = sanitizer.clean("""
                <figure>
                  <video controls preload="metadata">
                    <source src="alfy-media:3" type="video/mp4">
                  </video>
                  <figcaption>活动现场</figcaption>
                </figure>
                """);

        assertThat(clean)
                .contains("<video controls preload=\"metadata\">")
                .contains("src=\"alfy-media:3\"")
                .contains("type=\"video/mp4\"")
                .contains("活动现场");
    }

    @Test
    void keepsControlledTypographyAttributesButRemovesUnsafeStyles() {
        String clean = sanitizer.clean("""
                <p data-align="center" onclick="alert(1)">
                  <span data-font="serif" data-size="large" data-color="red" data-highlight="yellow" style="position:fixed">重点内容</span>
                </p>
                """);

        assertThat(clean)
                .contains("data-align=\"center\"")
                .contains("data-font=\"serif\"")
                .contains("data-size=\"large\"")
                .contains("data-color=\"red\"")
                .contains("data-highlight=\"yellow\"")
                .doesNotContain("onclick")
                .doesNotContain("style=");
    }
}

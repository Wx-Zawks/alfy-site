CREATE TABLE seo_global_setting
(
    id                         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    canonical_origin           VARCHAR(255) NOT NULL,
    title_template             VARCHAR(100) NOT NULL DEFAULT '%s｜奥飞新材',
    default_title              VARCHAR(255) NOT NULL,
    default_description        VARCHAR(500) NOT NULL,
    default_og_media_id        BIGINT UNSIGNED NULL,
    organization_name          VARCHAR(255) NOT NULL,
    organization_alternate_name VARCHAR(100) NULL,
    organization_legal_name    VARCHAR(255) NULL,
    organization_description   VARCHAR(1000) NULL,
    founding_date              DATE NULL,
    same_as_json               JSON NULL,
    baidu_site_verification    VARCHAR(255) NULL,
    google_site_verification   VARCHAR(255) NULL,
    bing_site_verification     VARCHAR(255) NULL,
    ga4_enabled                TINYINT NOT NULL DEFAULT 0,
    ga4_measurement_id         VARCHAR(50) NULL,
    baidu_analytics_enabled    TINYINT NOT NULL DEFAULT 0,
    baidu_analytics_id         VARCHAR(100) NULL,
    clarity_enabled            TINYINT NOT NULL DEFAULT 0,
    clarity_project_id         VARCHAR(100) NULL,
    public_index_enabled       TINYINT NOT NULL DEFAULT 1,
    oai_searchbot_policy       VARCHAR(20) NOT NULL DEFAULT 'allow',
    gptbot_policy              VARCHAR(20) NOT NULL DEFAULT 'allow',
    google_extended_policy     VARCHAR(20) NOT NULL DEFAULT 'allow',
    version                    BIGINT NOT NULL DEFAULT 0,
    created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网全局 SEO、站长验证、统计和爬虫策略';

CREATE TABLE seo_route_setting
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    route_key       VARCHAR(100) NOT NULL,
    route_path      VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    seo_title       VARCHAR(255) NULL,
    seo_description VARCHAR(500) NULL,
    seo_keywords    VARCHAR(500) NULL,
    og_media_id     BIGINT UNSIGNED NULL,
    index_enabled   TINYINT NOT NULL DEFAULT 1,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seo_route_key (route_key),
    UNIQUE KEY uk_seo_route_path (route_path)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '固定栏目页面 SEO 配置';

INSERT INTO seo_global_setting
    (canonical_origin, title_template, default_title, default_description,
     organization_name, organization_alternate_name, organization_legal_name)
VALUES
    ('https://alfy.com.cn', '%s｜奥飞新材', '奥飞新材',
     '湖南奥飞新材料有限公司官方网站。', '湖南奥飞新材料有限公司',
     '奥飞新材', '湖南奥飞新材料有限公司');

INSERT INTO seo_route_setting
    (route_key, route_path, display_name, index_enabled)
VALUES
    ('home', '/', '首页', 1),
    ('products-index', '/products', '产品中心', 1),
    ('applications-index', '/applications', '应用领域', 1),
    ('technology-index', '/technology', '技术创新', 1),
    ('news-index', '/news', '新闻中心', 1),
    ('cooperation-index', '/cooperation', '合作伙伴', 1),
    ('about-index', '/about', '关于我们', 1);

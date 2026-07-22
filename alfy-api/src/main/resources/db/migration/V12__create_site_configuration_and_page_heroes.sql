CREATE TABLE site_setting
(
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_name         VARCHAR(255) NULL,
    logo_media_id        BIGINT UNSIGNED NULL,
    address              VARCHAR(500) NULL,
    service_phone        VARCHAR(64) NULL,
    service_email        VARCHAR(255) NULL,
    wechat_qr_media_id   BIGINT UNSIGNED NULL,
    icp_number           VARCHAR(255) NULL,
    copyright_text       VARCHAR(500) NULL,
    privacy_policy_url   VARCHAR(500) NULL,
    version              BIGINT NOT NULL DEFAULT 0,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted              TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点基础信息与页脚联系方式';

CREATE TABLE site_navigation_item
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    navigation_area VARCHAR(16) NOT NULL COMMENT 'HEADER 或 FOOTER',
    parent_id      BIGINT UNSIGNED NULL COMMENT '父导航项，NULL 表示一级项',
    label          VARCHAR(100) NOT NULL,
    target         VARCHAR(500) NOT NULL,
    sort_order     INT NOT NULL DEFAULT 0,
    highlighted    TINYINT NOT NULL DEFAULT 0,
    enabled        TINYINT NOT NULL DEFAULT 1,
    version        BIGINT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_navigation_display (navigation_area, parent_id, enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点顶部与页脚导航';

CREATE TABLE page_hero
(
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    page_key               VARCHAR(64) NOT NULL COMMENT 'about、technology、cooperation 等页面标识',
    eyebrow                VARCHAR(255) NULL,
    title                  VARCHAR(255) NOT NULL,
    highlight_text         VARCHAR(255) NULL,
    summary                VARCHAR(1000) NULL,
    background_media_id    BIGINT UNSIGNED NULL,
    mobile_background_media_id BIGINT UNSIGNED NULL,
    primary_action_label   VARCHAR(100) NULL,
    primary_action_target  VARCHAR(500) NULL,
    secondary_action_label VARCHAR(100) NULL,
    secondary_action_target VARCHAR(500) NULL,
    status                 VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at           DATETIME NULL,
    version                BIGINT NOT NULL DEFAULT 0,
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_hero_key (page_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '单页顶部首屏栏目';

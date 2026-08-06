CREATE TABLE hero_slide
(
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    eyebrow                 VARCHAR(255) NULL,
    title                   VARCHAR(255) NOT NULL,
    highlight_text          VARCHAR(255) NULL,
    summary                 VARCHAR(1000) NULL,
    desktop_media_id        BIGINT UNSIGNED NULL,
    mobile_media_id         BIGINT UNSIGNED NULL,
    primary_action_label    VARCHAR(100) NULL,
    primary_action_target   VARCHAR(500) NULL,
    secondary_action_label  VARCHAR(100) NULL,
    secondary_action_target VARCHAR(500) NULL,
    sort_order              INT NOT NULL DEFAULT 0,
    status                  VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    starts_at               DATETIME NULL,
    ends_at                 DATETIME NULL,
    published_at            DATETIME NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT NOT NULL DEFAULT 0,
    version                 BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hero_slide_display (status, starts_at, ends_at, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网首页首屏轮播';

CREATE TABLE technology_page
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    page_key           VARCHAR(64) NOT NULL,
    eyebrow            VARCHAR(255) NULL,
    title              VARCHAR(255) NOT NULL,
    highlight_text     VARCHAR(255) NULL,
    summary            VARCHAR(1000) NULL,
    cta_label          VARCHAR(100) NULL,
    cta_target         VARCHAR(500) NULL,
    capability_rows_json JSON NULL,
    pillars_json       JSON NULL,
    content_html       LONGTEXT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at       DATETIME NULL,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT NOT NULL DEFAULT 0,
    version            BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_technology_page_key (page_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技术研发页面及首页概览';

CREATE TABLE inquiry
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    inquiry_no       VARCHAR(32) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    company          VARCHAR(255) NULL,
    phone            VARCHAR(32) NULL,
    email            VARCHAR(255) NULL,
    inquiry_type     VARCHAR(32) NOT NULL DEFAULT 'GENERAL',
    message          TEXT NULL,
    product_id       BIGINT UNSIGNED NULL,
    source_url       VARCHAR(1024) NULL,
    utm_json         JSON NULL,
    privacy_accepted TINYINT NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'NEW',
    admin_note       TEXT NULL,
    handled_at       DATETIME NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inquiry_no (inquiry_no),
    KEY idx_inquiry_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网询盘线索';

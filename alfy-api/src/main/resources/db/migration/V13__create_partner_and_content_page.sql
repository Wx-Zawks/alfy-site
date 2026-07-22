CREATE TABLE partner
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                VARCHAR(255) NOT NULL,
    slug                VARCHAR(255) NOT NULL,
    category            VARCHAR(100) NULL,
    summary             VARCHAR(1000) NULL,
    logo_media_id       BIGINT UNSIGNED NULL,
    website_url         VARCHAR(500) NULL,
    is_featured         TINYINT NOT NULL DEFAULT 0,
    sort_order          INT NOT NULL DEFAULT 0,
    status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    seo_title           VARCHAR(255) NULL,
    seo_description     VARCHAR(500) NULL,
    seo_keywords        VARCHAR(500) NULL,
    published_at        DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT NOT NULL DEFAULT 0,
    version             BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_partner_slug (slug),
    KEY idx_partner_public (status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网合作伙伴';

CREATE TABLE content_page
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    page_key            VARCHAR(100) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    category            VARCHAR(100) NULL,
    summary             VARCHAR(1000) NULL,
    content_html        LONGTEXT NULL,
    cover_media_id      BIGINT UNSIGNED NULL,
    is_featured         TINYINT NOT NULL DEFAULT 0,
    sort_order          INT NOT NULL DEFAULT 0,
    status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    seo_title           VARCHAR(255) NULL,
    seo_description     VARCHAR(500) NULL,
    seo_keywords        VARCHAR(500) NULL,
    published_at        DATETIME NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT NOT NULL DEFAULT 0,
    version             BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_page_key (page_key),
    KEY idx_content_page_public (status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网可编辑单页';

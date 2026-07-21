CREATE TABLE application_scene
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100)    NOT NULL,
    slug           VARCHAR(100)    NOT NULL,
    slogan         VARCHAR(255)    NULL,
    summary        VARCHAR(1000)   NULL,
    pain_point     TEXT            NULL,
    solution       TEXT            NULL,
    content_html   LONGTEXT        NULL,
    cover_media_id BIGINT UNSIGNED NULL,
    is_featured    TINYINT         NOT NULL DEFAULT 0,
    sort_order     INT             NOT NULL DEFAULT 0,
    status         VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    seo_title      VARCHAR(255)    NULL,
    seo_description VARCHAR(500)   NULL,
    seo_keywords   VARCHAR(500)    NULL,
    published_at   DATETIME        NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_application_scene_slug (slug),
    KEY idx_application_scene_display (status, sort_order, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE case_project
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scene_id        BIGINT UNSIGNED NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    slug            VARCHAR(255)    NOT NULL,
    client_name     VARCHAR(255)    NULL,
    location        VARCHAR(255)    NULL,
    summary         VARCHAR(1000)   NULL,
    background      TEXT            NULL,
    customer_need   TEXT            NULL,
    solution        TEXT            NULL,
    implementation  TEXT            NULL,
    result_summary  TEXT            NULL,
    content_html    LONGTEXT        NULL,
    cover_media_id  BIGINT UNSIGNED NULL,
    is_featured     TINYINT         NOT NULL DEFAULT 0,
    sort_order      INT             NOT NULL DEFAULT 0,
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    seo_title       VARCHAR(255)    NULL,
    seo_description VARCHAR(500)    NULL,
    seo_keywords    VARCHAR(500)    NULL,
    published_at    DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_case_project_slug (slug),
    KEY idx_case_project_scene (scene_id, status, sort_order, published_at),
    KEY idx_case_project_featured (status, is_featured, sort_order, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product_scene_rel
(
    product_id BIGINT UNSIGNED NOT NULL,
    scene_id   BIGINT UNSIGNED NOT NULL,
    sort_order INT             NOT NULL DEFAULT 0,

    PRIMARY KEY (product_id, scene_id),
    KEY idx_product_scene_scene (scene_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE product_case_rel
(
    product_id BIGINT UNSIGNED NOT NULL,
    case_id    BIGINT UNSIGNED NOT NULL,
    sort_order INT             NOT NULL DEFAULT 0,

    PRIMARY KEY (product_id, case_id),
    KEY idx_product_case_case (case_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

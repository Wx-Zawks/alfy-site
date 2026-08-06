CREATE TABLE product_category
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(100)    NOT NULL COMMENT '分类名称',
    slug           VARCHAR(100)    NOT NULL COMMENT '公开 URL 分类标识',
    summary        VARCHAR(1000)   NULL COMMENT '分类简介',
    cover_media_id BIGINT UNSIGNED NULL COMMENT '分类封面媒体',
    sort_order     INT             NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    status         TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_product_category_slug (slug),
    KEY idx_product_category_display (status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品分类';

CREATE TABLE product
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id         BIGINT UNSIGNED NOT NULL COMMENT '产品分类 ID',
    name                VARCHAR(255)    NOT NULL COMMENT '产品名称',
    slug                VARCHAR(255)    NOT NULL COMMENT '公开 URL 标识',
    summary             VARCHAR(1000)   NULL COMMENT '列表短描述',
    content_html        LONGTEXT        NULL COMMENT '经过清洗的详情正文',
    content_text        LONGTEXT        NULL COMMENT '纯文本正文',
    cover_media_id      BIGINT UNSIGNED NULL COMMENT '封面媒体 ID',
    features_json       JSON            NULL COMMENT '产品特点字符串数组',
    specifications_json JSON            NULL COMMENT '经授权公开的参数结构',
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT、PENDING_REVIEW、PUBLISHED、OFFLINE',
    is_featured         TINYINT         NOT NULL DEFAULT 0 COMMENT '是否首页推荐',
    sort_order          INT             NOT NULL DEFAULT 0 COMMENT '列表排序',
    seo_title           VARCHAR(255)    NULL COMMENT 'SEO 标题',
    seo_description     VARCHAR(500)    NULL COMMENT 'SEO 描述',
    seo_keywords        VARCHAR(500)    NULL COMMENT 'SEO 关键词',
    version             BIGINT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    published_at        DATETIME        NULL COMMENT '本站发布时间',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_product_slug (slug),
    KEY idx_product_category_status (category_id, status, sort_order),
    KEY idx_product_featured (status, is_featured, sort_order, published_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品';

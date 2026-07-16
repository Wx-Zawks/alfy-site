CREATE TABLE article_category
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id  BIGINT UNSIGNED NULL COMMENT '父分类 ID，新闻动态为公司/行业新闻的父分类',
    code       VARCHAR(64)     NOT NULL COMMENT '稳定分类编码',
    name       VARCHAR(64)     NOT NULL COMMENT '分类名称',
    sort_order INT             NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_article_category_code (code),
    KEY idx_article_category_parent_id (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '博客文章分类';

INSERT INTO article_category (id, parent_id, code, name, sort_order)
VALUES (1, NULL, 'news', '新闻动态', 10),
       (2, 1, 'company_news', '公司新闻', 11),
       (3, 1, 'industry_news', '行业新闻', 12),
       (4, NULL, 'case', '相关案例', 20),
       (5, NULL, 'research', '研发成果', 30),
       (6, NULL, 'application_technology', '应用技术', 40);

CREATE TABLE media_asset
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    media_type        VARCHAR(16)     NOT NULL COMMENT '媒体类型：IMAGE、VIDEO、DOCUMENT',
    storage_key       VARCHAR(512)    NOT NULL COMMENT '相对存储路径或对象存储 Key',
    original_filename VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    mime_type         VARCHAR(128)    NULL COMMENT 'MIME 类型',
    file_size         BIGINT UNSIGNED NULL COMMENT '文件大小，字节',
    sha256            CHAR(64)        NULL COMMENT '文件 SHA-256',
    width             INT UNSIGNED    NULL COMMENT '图片/视频宽度',
    height            INT UNSIGNED    NULL COMMENT '图片/视频高度',
    duration_seconds  INT UNSIGNED    NULL COMMENT '视频时长，秒',
    alt_text          VARCHAR(255)    NULL COMMENT '替代文本',
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    KEY idx_media_asset_type (media_type),
    KEY idx_media_asset_sha256 (sha256)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '媒体资源';

CREATE TABLE article
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_id         BIGINT UNSIGNED NOT NULL COMMENT '文章分类 ID',
    title               VARCHAR(255)    NOT NULL COMMENT '标题',
    slug                VARCHAR(255)    NULL COMMENT 'URL 标识，发布前生成并校验',
    summary             VARCHAR(1000)   NULL COMMENT '摘要',
    content_html        LONGTEXT        NULL COMMENT '经过清洗的 HTML 正文',
    content_text        LONGTEXT        NULL COMMENT '纯文本正文，用于搜索和审计',
    cover_media_id      BIGINT UNSIGNED NULL COMMENT '封面媒体 ID',
    author_name         VARCHAR(100)    NULL COMMENT '作者展示名',
    source_url          VARCHAR(1024)   NULL COMMENT '旧站原文链接或转载来源',
    source_file         VARCHAR(1024)   NULL COMMENT '导入源文件的相对路径',
    content_hash        CHAR(64)        NULL COMMENT '规范化正文 SHA-256，用于辅助排重',
    source_published_at DATETIME        NULL COMMENT '源内容标注的发布时间，需审核确认',
    published_at        DATETIME        NULL COMMENT '本站实际发布时间',
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT、PENDING_REVIEW、PUBLISHED、OFFLINE',
    sort_order          INT             NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    seo_title           VARCHAR(255)    NULL COMMENT 'SEO 标题',
    seo_description     VARCHAR(500)    NULL COMMENT 'SEO 描述',
    seo_keywords        VARCHAR(500)    NULL COMMENT 'SEO 关键词，逗号分隔',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_article_slug (slug),
    KEY idx_article_category_status_published (category_id, status, published_at),
    KEY idx_article_content_hash (content_hash),
    KEY idx_article_source_url (source_url(255))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '博客文章';

CREATE TABLE article_media
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章 ID',
    media_id   BIGINT UNSIGNED NOT NULL COMMENT '媒体 ID',
    usage_type VARCHAR(16)     NOT NULL DEFAULT 'INLINE' COMMENT 'COVER、INLINE、VIDEO、ATTACHMENT',
    sort_order INT             NOT NULL DEFAULT 0 COMMENT '展示排序',
    caption    VARCHAR(500)    NULL COMMENT '媒体说明',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_article_media_usage (article_id, media_id, usage_type),
    KEY idx_article_media_article_sort (article_id, sort_order),
    KEY idx_article_media_media_id (media_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '文章与媒体资源关联';

CREATE TABLE content_import_record
(
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    article_id      BIGINT UNSIGNED NULL COMMENT '成功入库后的文章 ID',
    category_code   VARCHAR(64)     NOT NULL COMMENT '预检识别的分类编码',
    source_file     VARCHAR(1024)   NOT NULL COMMENT '源 DOCX 相对路径',
    source_url      VARCHAR(1024)   NULL COMMENT '预检识别的旧站链接',
    source_hash     CHAR(64)        NOT NULL COMMENT '源 DOCX SHA-256',
    content_hash    CHAR(64)        NULL COMMENT '提取后正文 SHA-256',
    import_status   VARCHAR(24)     NOT NULL COMMENT 'PREFLIGHT、IMPORTED、SKIPPED、FAILED',
    warning_message TEXT            NULL COMMENT '预检或导入警告',
    imported_at     DATETIME        NULL COMMENT '实际写入文章表的时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_content_import_source_hash (source_hash),
    KEY idx_content_import_article_id (article_id),
    KEY idx_content_import_category_status (category_code, import_status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '内容导入审计记录';

ALTER TABLE article
    MODIFY COLUMN category_id BIGINT UNSIGNED NOT NULL COMMENT '主分类 ID，用于默认排序和编辑入口';

CREATE TABLE article_category_relation
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    article_id  BIGINT UNSIGNED NOT NULL COMMENT '文章 ID',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类 ID',
    sort_order  INT             NOT NULL DEFAULT 0 COMMENT '该分类下的展示排序',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_article_category_relation (article_id, category_id),
    KEY idx_article_category_relation_category (category_id, sort_order),
    KEY idx_article_category_relation_article (article_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '文章多分类关联';

-- 为已有文章补上主分类关联；新导入文章也必须同时写入该表。
INSERT INTO article_category_relation (article_id, category_id, sort_order)
SELECT id, category_id, sort_order
FROM article
WHERE deleted = 0;

ALTER TABLE article
    ADD COLUMN is_featured TINYINT NOT NULL DEFAULT 0 COMMENT '是否首页推荐：1是，0否' AFTER sort_order,
    ADD COLUMN home_slot VARCHAR(32) NULL COMMENT '首页展示位：HERO、NEWS_PRIMARY、NEWS_SECONDARY、NEWS_LIST' AFTER is_featured,
    ADD COLUMN home_sort_order INT NOT NULL DEFAULT 0 COMMENT '首页展示排序，越小越靠前' AFTER home_slot,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER seo_keywords;

-- 历史草稿此前允许 slug 为空。先生成稳定占位 slug，内容管理员发布前可改为正式可读 slug。
UPDATE article
SET slug = CONCAT('article-', id)
WHERE slug IS NULL OR slug = '';

ALTER TABLE article
    MODIFY COLUMN slug VARCHAR(255) NOT NULL COMMENT '公开 URL 标识，唯一且发布前可编辑',
    ADD KEY idx_article_home_display (status, home_slot, home_sort_order, published_at),
    ADD KEY idx_article_featured (status, is_featured, published_at);

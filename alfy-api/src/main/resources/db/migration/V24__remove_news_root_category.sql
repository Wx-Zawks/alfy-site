-- “新闻动态”不再作为分类入口；公司新闻、行业新闻改为一级分类。
UPDATE article_category child
JOIN article_category parent ON child.parent_id = parent.id
SET child.parent_id = NULL,
    child.updated_at = NOW()
WHERE parent.code = 'news'
  AND parent.deleted = 0;

-- 使用逻辑删除，保留历史记录与既有外键/关联兼容性。
UPDATE article_category
SET deleted = 1,
    status = 0,
    updated_at = NOW()
WHERE code = 'news'
  AND deleted = 0;

-- 旧更新逻辑在隐藏新闻时已把 is_featured 改为 0，却因 NULL 更新策略保留了 home_slot。
-- 清理这类不一致数据，确保首页配置页和公共首页都不再读取已经隐藏的新闻。
UPDATE article
SET home_slot = NULL
WHERE deleted = 0
  AND is_featured = 0
  AND home_slot IS NOT NULL;

-- 频道首页统一由 page_hero 管理。背景图允许为空，前台会继续使用各页面自带的兜底图。
INSERT INTO page_hero (
    page_key, eyebrow, title, highlight_text, summary, status, published_at
)
SELECT
    'about', '关于我们', '中南大学气凝胶成果转化核心平台', NULL, NULL, 'PUBLISHED', CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM page_hero WHERE page_key = 'about');

INSERT INTO page_hero (
    page_key, eyebrow, title, highlight_text, summary, status, published_at
)
SELECT
    'applications', 'Applications & Cases', '从行业问题出发', '让材料创造价值',
    '围绕能效、空间、施工与安全等问题，匹配材料、产品和项目应用路径。',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM page_hero WHERE page_key = 'applications');

INSERT INTO page_hero (
    page_key, eyebrow, title, highlight_text, summary, status, published_at
)
SELECT
    'news', 'Newsroom', '关注气凝胶产业', '最新进展',
    '记录科研、产品、项目与产业合作的重要节点。',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM page_hero WHERE page_key = 'news');

INSERT INTO page_hero (
    page_key, eyebrow, title, highlight_text, summary, status, published_at
)
SELECT
    'products', '核心产品', '中南大学气凝胶成果转化核心平台', NULL, NULL,
    'PUBLISHED', CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM page_hero WHERE page_key = 'products');

INSERT INTO page_hero (
    page_key, eyebrow, title, highlight_text, summary, status, published_at
)
SELECT
    'technology', '核心技术', '中南大学气凝胶成果转化核心平台', NULL, NULL,
    'PUBLISHED', CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM page_hero WHERE page_key = 'technology');

INSERT INTO page_hero (
    page_key, eyebrow, title, highlight_text, summary, status, published_at
)
SELECT
    'cooperation', 'OPEN COLLABORATION', '开放共赢，共建', '气凝胶产业生态', NULL,
    'PUBLISHED', CURRENT_TIMESTAMP
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM page_hero WHERE page_key = 'cooperation');

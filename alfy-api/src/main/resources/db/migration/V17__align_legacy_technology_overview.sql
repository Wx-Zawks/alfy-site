UPDATE technology_page
SET eyebrow = '核心技术',
    title = '中南大学气凝胶成果转化核心平台',
    highlight_text = NULL,
    summary = '围绕气凝胶双纳米结构、常压干燥与复合材料制备，推动科研成果进入规模化产业应用。',
    cta_label = '与技术团队交流',
    cta_target = '/cooperation',
    seo_title = '核心技术｜奥飞新材',
    seo_description = '奥飞新材气凝胶材料、复合产品与产业化技术平台。',
    seo_keywords = '奥飞新材,气凝胶,核心技术',
    version = version + 1
WHERE page_key = 'technology'
  AND deleted = 0
  AND title = '新一代常压'
  AND highlight_text = '干燥技术';

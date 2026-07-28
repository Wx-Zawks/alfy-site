ALTER TABLE content_page
    ADD COLUMN content_json LONGTEXT NULL COMMENT '页面结构化内容 JSON' AFTER content_html;

UPDATE content_page
SET title = CASE
        WHEN title = '开放共赢，共建气凝胶产业生态' THEN '开放共赢，共建'
        ELSE title
    END,
    content_json = JSON_OBJECT(
        'eyebrow', 'OPEN COLLABORATION',
        'highlightText', '气凝胶产业生态',
        'actionLabel', '发起合作咨询',
        'sectionEyebrow', '合作模式',
        'sectionTitle', '选择适合双方资源的合作方式',
        'processEyebrow', '合作流程',
        'processTitle', '让合作预期清晰可见',
        'process', JSON_ARRAY(
            JSON_OBJECT('title', '需求沟通', 'description', '明确市场、场景与合作目标。'),
            JSON_OBJECT('title', '能力评估', 'description', '确认双方资源和合作边界。'),
            JSON_OBJECT('title', '方案验证', 'description', '通过样品、小试或项目验证。'),
            JSON_OBJECT('title', '合作落地', 'description', '明确交付、支持和复盘机制。')
        ),
        'faqEyebrow', '常见问题',
        'faqTitle', '合作前，您可能关心',
        'faqs', JSON_ARRAY(
            JSON_OBJECT('title', '如何申请成为区域经销合作伙伴？', 'description', '提交目标区域、现有渠道、服务团队和重点客户类型，商务团队评估后安排沟通。'),
            JSON_OBJECT('title', '联合开发需要提供哪些信息？', 'description', '建议提供目标产品体系、关键性能、基材、工艺条件和计划周期。'),
            JSON_OBJECT('title', '是否可以先做样品或小试？', 'description', '可根据项目情况讨论样品、小试或联合测试方式。')
        )
    )
WHERE page_key = 'cooperation'
  AND deleted = 0
  AND (content_json IS NULL OR content_json = '');

INSERT INTO content_page
(
    page_key, title, category, summary, content_html, content_json, cover_media_id,
    is_featured, sort_order, status, seo_title, seo_description, seo_keywords, published_at
)
SELECT
    'cooperation-dealer',
    '经销商合作',
    '合作模式',
    '面向具备区域客户、项目服务或建材渠道能力的伙伴，共同拓展建筑节能和工业节能市场。',
    '<h2>携手开拓区域市场</h2><p>奥飞新材为经销合作伙伴提供产品资料、技术培训、项目支持与市场协同，共同服务区域客户。</p><h2>我们期待的伙伴</h2><ul><li>具备清晰的目标区域和客户资源</li><li>拥有项目服务或建材渠道能力</li><li>认同长期合作与规范经营</li></ul><h2>合作支持</h2><p>根据区域特点与业务计划，双方共同制定产品导入、客户拓展和项目跟进方案。</p>',
    JSON_OBJECT('eyebrow', 'DEALER COOPERATION', 'highlightText', '共拓区域市场', 'actionLabel', '咨询经销合作'),
    NULL, 0, 21, 'PUBLISHED',
    '经销商合作｜奥飞新材',
    '了解奥飞新材区域经销合作条件、合作支持与沟通流程。',
    '奥飞新材,经销商合作,气凝胶渠道',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM content_page WHERE page_key = 'cooperation-dealer' AND deleted = 0
);

INSERT INTO content_page
(
    page_key, title, category, summary, content_html, content_json, cover_media_id,
    is_featured, sort_order, status, seo_title, seo_description, seo_keywords, published_at
)
SELECT
    'cooperation-product-development',
    '复合产品开发模式',
    '合作模式',
    '基于气凝胶粉体、分散体和界面改性能力，与合作伙伴共同定义产品目标并完成验证。',
    '<h2>从产品目标到联合验证</h2><p>围绕涂料、板材、纤维、树脂及其他复合体系，双方共同明确性能目标、基材条件与应用场景。</p><h2>合作内容</h2><ul><li>材料选型与配方匹配</li><li>样品、小试与性能验证</li><li>工艺适配与工程化放大</li></ul><h2>交付方式</h2><p>可根据项目阶段提供气凝胶功能材料、配方建议、联合测试与技术协同。</p>',
    JSON_OBJECT('eyebrow', 'PRODUCT DEVELOPMENT', 'highlightText', '联合定义产品', 'actionLabel', '咨询联合开发'),
    NULL, 0, 22, 'PUBLISHED',
    '复合产品开发模式｜奥飞新材',
    '了解奥飞新材气凝胶复合产品联合开发、样品验证与工程化服务。',
    '气凝胶复合产品,联合开发,材料验证',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM content_page WHERE page_key = 'cooperation-product-development' AND deleted = 0
);

INSERT INTO content_page
(
    page_key, title, category, summary, content_html, content_json, cover_media_id,
    is_featured, sort_order, status, seo_title, seo_description, seo_keywords, published_at
)
SELECT
    'cooperation-industry-chain',
    '产业链合作模式',
    '合作模式',
    '连接科研、材料、制造、设计与工程资源，围绕气凝胶产业化建立长期协同关系。',
    '<h2>共建气凝胶产业合作生态</h2><p>面向产业链上下游伙伴，奥飞新材开放材料、技术与应用验证能力，共同推进创新成果落地。</p><h2>合作方向</h2><ul><li>科研成果转化与联合研发</li><li>材料供应、制造与工程协同</li><li>标准、示范项目与市场共建</li></ul><h2>合作原则</h2><p>以能力互补、边界清晰、成果共享和长期价值为基础，建立可持续的产业合作机制。</p>',
    JSON_OBJECT('eyebrow', 'INDUSTRY ECOSYSTEM', 'highlightText', '共建产业生态', 'actionLabel', '咨询产业链合作'),
    NULL, 0, 23, 'PUBLISHED',
    '产业链合作模式｜奥飞新材',
    '了解奥飞新材与科研、材料、制造、设计和工程伙伴的产业链合作模式。',
    '气凝胶产业链,产业合作,联合研发',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM content_page WHERE page_key = 'cooperation-industry-chain' AND deleted = 0
);

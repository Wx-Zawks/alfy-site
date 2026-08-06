ALTER TABLE technology_page
    ADD COLUMN hero_media_id BIGINT UNSIGNED NULL AFTER summary,
    ADD COLUMN seo_title VARCHAR(255) NULL AFTER content_html,
    ADD COLUMN seo_description VARCHAR(500) NULL AFTER seo_title,
    ADD COLUMN seo_keywords VARCHAR(500) NULL AFTER seo_description,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER seo_keywords,
    ADD KEY idx_technology_page_display (status, sort_order, id);

INSERT INTO technology_page
    (page_key, eyebrow, title, summary, cta_label, cta_target,
     capability_rows_json, pillars_json, status, published_at, sort_order,
     seo_title, seo_description, seo_keywords)
SELECT
    'technology',
    '核心技术',
    '中南大学气凝胶成果转化核心平台',
    '围绕气凝胶双纳米结构、常压干燥与复合材料制备，推动科研成果进入规模化产业应用。',
    '与技术团队交流',
    '/cooperation',
    JSON_ARRAY(),
    JSON_ARRAY(),
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    0,
    '核心技术｜奥飞新材',
    '奥飞新材气凝胶材料、复合产品与产业化技术平台。',
    '奥飞新材,气凝胶,核心技术'
WHERE NOT EXISTS (
    SELECT 1 FROM technology_page WHERE page_key = 'technology' AND deleted = 0
);

INSERT INTO technology_page
    (page_key, eyebrow, title, summary, cta_label, cta_target,
     capability_rows_json, pillars_json, content_html, status, published_at,
     sort_order, seo_title, seo_description, seo_keywords)
SELECT
    'aerogel-material',
    '气凝胶材料技术',
    '新一代气凝胶常压干燥技术',
    '通过溶胶-凝胶结构均匀调控与环境友好型表面疏水改性，使常压干燥产品在导热系数、孔隙率和比表面积等关键指标上达到超临界干燥水平。',
    '获取技术资料',
    '/cooperation',
    JSON_ARRAY(
        JSON_OBJECT('title', '双纳米结构调控', 'description', '稳定保留纳米骨架和纳米孔洞结构，兼顾低密度、高孔隙率与超低导热性能。'),
        JSON_OBJECT('title', '环境友好型改性', 'description', '以环境友好型工艺完成凝胶表面疏水改性，提高干燥效率与产品稳定性。'),
        JSON_OBJECT('title', '规模化常压干燥', 'description', '降低传统超临界工艺的设备投入和综合能耗，适配连续化生产。')
    ),
    JSON_ARRAY(
        JSON_OBJECT('title', '低导热', 'description', '导热系数可低至 0.012W/(m·K)@25℃。'),
        JSON_OBJECT('title', '高孔隙率', 'description', '多孔网络结构完整，孔隙率最高可达 99.8%。'),
        JSON_OBJECT('title', '轻质', 'description', '低密度特性为隔热、吸附与功能复合提供材料基础。')
    ),
    '<p>奥飞新材依托中南大学科研成果，建立了从结构设计、凝胶制备、表面改性到常压干燥的完整技术路线。</p>',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    1,
    '气凝胶材料技术｜奥飞新材',
    '了解奥飞新材新一代气凝胶常压干燥、结构调控与规模化制备技术。',
    '气凝胶材料,常压干燥,双纳米结构'
WHERE NOT EXISTS (
    SELECT 1 FROM technology_page WHERE page_key = 'aerogel-material' AND deleted = 0
);

INSERT INTO technology_page
    (page_key, eyebrow, title, summary, cta_label, cta_target,
     capability_rows_json, pillars_json, content_html, status, published_at,
     sort_order, seo_title, seo_description, seo_keywords)
SELECT
    'aerogel-composite',
    '气凝胶复合产品技术',
    '气凝胶复合材料制备技术',
    '通过界面与工艺控制，在粉碎、分散和复合过程中保留气凝胶双纳米结构，使复合材料兼具应用强度、超低导热、轻质和防火等性能。',
    '获取联合开发方案',
    '/cooperation',
    JSON_ARRAY(
        JSON_OBJECT('title', '第一步：粉碎', 'description', '气凝胶块体经破碎制成微米级粉体，双纳米结构完整保留。'),
        JSON_OBJECT('title', '第二步：分散', 'description', '粉体均匀分散于体系中，纳米多孔结构不坍塌并保持隔热活性。'),
        JSON_OBJECT('title', '第三步：结构保护', 'description', '通过界面调控保护纳米骨架，使复合体系兼顾加工性与功能表现。')
    ),
    JSON_ARRAY(
        JSON_OBJECT('title', '气凝胶涂料', 'description', '面向建筑内外墙、工业管道和设备隔热防结露。'),
        JSON_OBJECT('title', '气凝胶毡板', 'description', '面向工业保温、高温设备与新能源热管理。'),
        JSON_OBJECT('title', '定制复合体系', 'description', '围绕树脂、纤维和无机基体开展联合配方与工艺开发。')
    ),
    '<p>围绕不同基体、施工方式与使用环境，技术团队可开展材料选型、配方匹配、样品验证和工程化放大。</p>',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    2,
    '气凝胶复合产品技术｜奥飞新材',
    '了解奥飞新材气凝胶粉碎、分散、界面保护与复合产品开发技术。',
    '气凝胶复合材料,气凝胶涂料,复合产品'
WHERE NOT EXISTS (
    SELECT 1 FROM technology_page WHERE page_key = 'aerogel-composite' AND deleted = 0
);

INSERT INTO technology_page
    (page_key, eyebrow, title, summary, cta_label, cta_target,
     capability_rows_json, pillars_json, content_html, status, published_at,
     sort_order, seo_title, seo_description, seo_keywords)
SELECT
    'other',
    '其他技术',
    '面向产业应用的延伸技术能力',
    '围绕固废资源化、功能材料与产业化装备，持续推进可验证、可放大、可交付的技术成果。',
    '咨询技术合作',
    '/cooperation',
    JSON_ARRAY(
        JSON_OBJECT('title', '固废资源化', 'description', '围绕工业固废高值利用开展材料设计、工艺验证与应用评价。'),
        JSON_OBJECT('title', '功能材料开发', 'description', '结合多孔材料、界面改性和复合工艺开发面向特定场景的功能材料。'),
        JSON_OBJECT('title', '产业化装备', 'description', '围绕连续化、低能耗和稳定制造开展工艺包与装备协同开发。')
    ),
    JSON_ARRAY(
        JSON_OBJECT('title', '联合研发', 'description', '从需求定义到实验验证，与合作伙伴共同形成技术路线。'),
        JSON_OBJECT('title', '中试放大', 'description', '围绕关键参数和质量控制完成工程化验证。'),
        JSON_OBJECT('title', '成果转化', 'description', '连接科研、制造与市场资源，推动技术成果进入实际应用。')
    ),
    '<p>其他技术页面用于持续发布经公司确认可公开的延伸技术方向、阶段性成果与合作需求。</p>',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    3,
    '其他技术｜奥飞新材',
    '了解奥飞新材固废资源化、功能材料与产业化装备等延伸技术能力。',
    '固废资源化,功能材料,产业化装备'
WHERE NOT EXISTS (
    SELECT 1 FROM technology_page WHERE page_key = 'other' AND deleted = 0
);

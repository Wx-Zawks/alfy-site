-- 奥飞新材官网展示内容（来源：E:\alfy-projects\2 整体思路）
-- 仅补充不存在的数据，不更新或覆盖已有记录。

SET NAMES utf8mb4;
START TRANSACTION;

-- 站点基础信息
INSERT INTO site_setting
    (company_name, address, copyright_text, version, created_at, updated_at, deleted)
SELECT
    '湖南奥飞新材料有限公司',
    '湖南省浏阳市永安镇星辰·尚东产业小镇',
    '© 2026 湖南奥飞新材料有限公司',
    0, NOW(), NOW(), 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM site_setting WHERE deleted = 0);

-- 首页区块文案
INSERT INTO home_section
    (section_key, label, eyebrow, title, highlight_text, description_text, button_label, button_target, enabled, sort_order, version, created_at, updated_at, deleted)
SELECT 'proof', '实力背书', '技术与产业化能力', '中南大学气凝胶成果转化核心平台', NULL,
       '源自中南大学粉末冶金全国重点实验室，十余年技术积淀，推动新一代气凝胶常压干燥技术产业化。',
       NULL, NULL, 1, 10, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'proof')
UNION ALL
SELECT 'about', '关于我们', '关于我们', '中南大学气凝胶', '成果转化核心平台',
       '依托中南大学粉末冶金全国重点实验室科研力量，聚焦气凝胶技术开发、复合产品及规模化应用。',
       '了解奥飞新材', '/about', 1, 20, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'about')
UNION ALL
SELECT 'applications', '应用场景', '找到您的应用场景', '无论您在哪个行业，总有一种方案适合您', NULL,
       '围绕建筑节能、工业炉窑、新能源汽车、石油化工、粮仓仓储和通信机房提供隔热解决方案。',
       '查看应用场景', '/applications', 1, 30, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'applications')
UNION ALL
SELECT 'cases', '典型案例', '标杆项目', '用数据说话的标杆项目', NULL,
       '以真实项目呈现从材料选型、方案验证到工程落地的全过程。',
       '查看项目案例', '/applications', 1, 40, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'cases')
UNION ALL
SELECT 'partners', '合作伙伴', '与奥飞同行', '与行业伙伴共建气凝胶应用生态', NULL,
       '与建筑、工业、新能源等领域伙伴开展材料、产品与项目协同。',
       '洽谈合作', '/cooperation', 1, 50, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'partners')
UNION ALL
SELECT 'technology', '技术研发', '核心技术', '新一代常压', '干燥技术',
       '突破传统超临界工艺高投入、高能耗瓶颈，推动气凝胶绿色、低成本和规模化生产。',
       '查看核心技术', '/technology', 1, 60, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'technology')
UNION ALL
SELECT 'news', '新闻资讯', '新闻动态', '持续创新，持续发声', NULL,
       '关注奥飞新材技术研发、应用实践与气凝胶产业进展。',
       '进入新闻中心', '/news', 1, 70, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM home_section WHERE section_key = 'news');

-- 产品分类
INSERT INTO product_category
    (name, slug, summary, sort_order, status, version, created_at, updated_at, deleted)
SELECT '气凝胶涂料', 'aerogel-coatings', '面向建筑及工业隔热的气凝胶功能涂层产品。', 10, 1, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE slug = 'aerogel-coatings')
UNION ALL
SELECT '气凝胶毡/板', 'aerogel-felts-boards', '兼顾隔热性能与工程施工适配性的柔性毡材和板材。', 20, 1, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE slug = 'aerogel-felts-boards')
UNION ALL
SELECT '气凝胶配套', 'aerogel-additives', '服务复合产品开发的改性浆料、功能母粒等配套材料。', 30, 1, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE slug = 'aerogel-additives')
UNION ALL
SELECT '气凝胶粉体', 'aerogel-powders', '用于隔热、功能填充和复合改性的气凝胶粉体及分散体。', 40, 1, 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE slug = 'aerogel-powders');

SET @cat_coatings = (SELECT id FROM product_category WHERE slug = 'aerogel-coatings' LIMIT 1);
SET @cat_felts = (SELECT id FROM product_category WHERE slug = 'aerogel-felts-boards' LIMIT 1);
SET @cat_additives = (SELECT id FROM product_category WHERE slug = 'aerogel-additives' LIMIT 1);
SET @cat_powders = (SELECT id FROM product_category WHERE slug = 'aerogel-powders' LIMIT 1);

-- 产品内容
INSERT INTO product
    (category_id, name, slug, summary, content_html, content_text, features_json, specifications_json, status, is_featured, sort_order,
     seo_title, seo_description, seo_keywords, version, published_at, created_at, updated_at, deleted)
SELECT @cat_coatings, '气凝胶隔热涂料', 'aerogel-thermal-insulation-coating',
       '以气凝胶为核心功能材料的薄层隔热涂层，可面向建筑与工业设备表面应用。',
       '<h2>产品概述</h2><p>通过气凝胶纳米多孔结构降低热传导，为不同基材提供轻量化、薄层化隔热方案。</p><h2>应用方向</h2><p>建筑围护结构、工业设备及管道等需要隔热降耗的表面。</p>',
       '以气凝胶为核心功能材料的薄层隔热涂层，适用于建筑围护结构、工业设备及管道等表面。',
       JSON_ARRAY('薄层隔热', '施工适配性强', '支持场景化配方'),
       JSON_OBJECT('产品形态', '功能涂料', '主要方向', '建筑与工业隔热', '说明', '具体参数以项目技术资料为准'),
       'PUBLISHED', 1, 10, '气凝胶隔热涂料', '奥飞新材气凝胶隔热涂料产品与应用方向。', '气凝胶涂料,隔热涂料,建筑节能', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_coatings IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'aerogel-thermal-insulation-coating')
UNION ALL
SELECT @cat_coatings, '外墙耐候隔热涂料', 'exterior-weather-resistant-insulation-coating',
       '面向建筑外墙节能改造与新建项目的耐候型气凝胶隔热涂料。',
       '<h2>产品概述</h2><p>兼顾外墙耐候需求与气凝胶隔热能力，可结合基层条件形成配套施工方案。</p><h2>应用方向</h2><p>公共建筑、产业园区及既有建筑节能改造。</p>',
       '面向建筑外墙节能改造与新建项目的耐候型气凝胶隔热涂料。',
       JSON_ARRAY('外墙耐候', '节能改造', '配套施工方案'),
       JSON_OBJECT('产品形态', '外墙涂料', '主要方向', '建筑外墙隔热', '说明', '具体参数以项目技术资料为准'),
       'PUBLISHED', 1, 20, '外墙耐候隔热涂料', '适用于建筑外墙节能场景的气凝胶功能涂层。', '外墙涂料,气凝胶,建筑保温', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_coatings IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'exterior-weather-resistant-insulation-coating')
UNION ALL
SELECT @cat_felts, '气凝胶柔性毡', 'aerogel-flexible-blanket',
       '适配管道、设备及异形构件的柔性气凝胶复合隔热材料。',
       '<h2>产品概述</h2><p>将气凝胶隔热能力与柔性基材结合，便于包覆曲面和复杂构件。</p><h2>应用方向</h2><p>工业管线、高温设备、新能源热管理等场景。</p>',
       '适配管道、设备及异形构件的柔性气凝胶复合隔热材料。',
       JSON_ARRAY('柔性包覆', '轻质隔热', '适配异形构件'),
       JSON_OBJECT('产品形态', '柔性毡材', '主要方向', '管道与设备隔热', '说明', '具体参数以项目技术资料为准'),
       'PUBLISHED', 1, 30, '气凝胶柔性毡', '奥飞新材柔性气凝胶复合隔热材料。', '气凝胶毡,柔性隔热,工业保温', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_felts IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'aerogel-flexible-blanket')
UNION ALL
SELECT @cat_felts, '气凝胶保温板', 'aerogel-insulation-board',
       '用于墙体和设备隔热构造的板状气凝胶复合产品。',
       '<h2>产品概述</h2><p>以板材形态提供稳定隔热层，便于模块化设计与工程安装。</p><h2>应用方向</h2><p>建筑墙体、工业设施及需要定型隔热层的构造部位。</p>',
       '用于墙体和设备隔热构造的板状气凝胶复合产品。',
       JSON_ARRAY('板状定型', '模块化安装', '适配多种构造'),
       JSON_OBJECT('产品形态', '复合板材', '主要方向', '墙体与设备隔热', '说明', '具体参数以项目技术资料为准'),
       'PUBLISHED', 1, 40, '气凝胶保温板', '用于墙体和设备隔热构造的气凝胶复合板材。', '气凝胶板,保温板,隔热材料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_felts IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'aerogel-insulation-board')
UNION ALL
SELECT @cat_additives, '气凝胶改性浆料', 'modified-aerogel-slurry',
       '面向涂料及复合材料体系的气凝胶改性浆料，支持联合开发与配方适配。',
       '<h2>产品概述</h2><p>围绕分散稳定性和体系相容性进行改性，帮助气凝胶功能进入不同产品体系。</p><h2>合作方式</h2><p>可根据基材、工艺与目标性能开展样品和小试验证。</p>',
       '面向涂料及复合材料体系的气凝胶改性浆料，支持联合开发与配方适配。',
       JSON_ARRAY('分散稳定', '体系适配', '支持联合开发'),
       JSON_OBJECT('产品形态', '改性浆料', '主要方向', '复合产品开发', '说明', '具体参数以项目技术资料为准'),
       'PUBLISHED', 1, 50, '气凝胶改性浆料', '适用于复合产品开发的气凝胶功能浆料。', '气凝胶浆料,分散体,联合开发', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_additives IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'modified-aerogel-slurry')
UNION ALL
SELECT @cat_additives, '气凝胶复合母粒', 'aerogel-composite-masterbatch',
       '为聚合物及功能复合材料开发提供的气凝胶功能母粒。',
       '<h2>产品概述</h2><p>以功能母粒形式提升材料加工适配性，服务新产品开发和性能验证。</p><h2>应用方向</h2><p>功能塑料、复合片材及其他聚合物基材料。</p>',
       '为聚合物及功能复合材料开发提供的气凝胶功能母粒。',
       JSON_ARRAY('便于加工', '功能复合', '支持定制'),
       JSON_OBJECT('产品形态', '功能母粒', '主要方向', '聚合物复合', '说明', '具体参数以项目技术资料为准'),
       'PUBLISHED', 0, 60, '气凝胶复合母粒', '用于聚合物复合开发的气凝胶功能母粒。', '气凝胶母粒,复合材料,功能材料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_additives IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'aerogel-composite-masterbatch')
UNION ALL
SELECT @cat_powders, '气凝胶粉体', 'silica-aerogel-powder',
       '具备纳米多孔结构的气凝胶基础材料，可用于隔热和多种功能复合体系。',
       '<h2>产品概述</h2><p>作为气凝胶复合产品的基础功能材料，可根据下游体系开展粒径、表面和分散适配。</p><h2>应用方向</h2><p>涂料、板材、毡材及其他隔热复合材料。</p>',
       '具备纳米多孔结构的气凝胶基础材料，可用于隔热和多种功能复合体系。',
       JSON_ARRAY('纳米多孔', '轻质隔热', '可进行表面适配'),
       JSON_OBJECT('产品形态', '粉体', '主要方向', '隔热与功能复合', '说明', '具体参数以产品技术资料为准'),
       'PUBLISHED', 1, 70, '气凝胶粉体', '奥飞新材气凝胶粉体及复合应用方向。', '气凝胶粉体,纳米材料,隔热材料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_powders IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'silica-aerogel-powder')
UNION ALL
SELECT @cat_powders, '气凝胶水性分散体', 'waterborne-aerogel-dispersion',
       '适配水性体系的气凝胶分散材料，便于进入涂料及其他液态复合体系。',
       '<h2>产品概述</h2><p>通过分散和界面调控改善气凝胶在水性体系中的使用便利性。</p><h2>应用方向</h2><p>水性隔热涂料、功能浆料及定制化复合产品。</p>',
       '适配水性体系的气凝胶分散材料，便于进入涂料及其他液态复合体系。',
       JSON_ARRAY('水性体系', '分散适配', '便于复合'),
       JSON_OBJECT('产品形态', '水性分散体', '主要方向', '水性涂料与复合体系', '说明', '具体参数以产品技术资料为准'),
       'PUBLISHED', 0, 80, '气凝胶水性分散体', '适配水性产品体系的气凝胶分散材料。', '气凝胶分散体,水性涂料,复合材料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @cat_powders IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product WHERE slug = 'waterborne-aerogel-dispersion');

-- 六类应用场景
INSERT INTO application_scene
    (name, slug, slogan, summary, pain_point, solution, content_html, is_featured, sort_order, status,
     seo_title, seo_description, seo_keywords, version, published_at, created_at, updated_at, deleted)
SELECT '建筑节能', 'building-energy-saving', '让建筑更节能，让居住更舒适',
       '面向新建建筑与既有建筑节能改造，提供薄层隔热涂层和复合保温构造。',
       '建筑围护结构需要兼顾节能、空间利用、施工适配和长期耐候。',
       '结合墙体构造、基层条件与节能目标配置气凝胶涂料或复合板材方案。',
       '<h2>场景说明</h2><p>围绕外墙、内墙和屋面等部位，提供材料选型、样板验证与施工配套建议。</p>',
       1, 10, 'PUBLISHED', '建筑节能气凝胶解决方案', '面向建筑围护结构的气凝胶隔热材料与节能方案。', '建筑节能,气凝胶涂料,墙体保温', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM application_scene WHERE slug = 'building-energy-saving')
UNION ALL
SELECT '工业炉窑', 'industrial-furnace', '降低热损失，提升设备能效',
       '服务高温设备、炉体和热工系统的隔热降耗需求。',
       '高温工况存在热损失、表面温升和检修空间受限等问题。',
       '依据温度区间、设备结构和检修要求配置柔性毡材、板材或隔热涂层。',
       '<h2>场景说明</h2><p>通过工况调研和样品验证，形成适配设备结构的隔热层设计。</p>',
       1, 20, 'PUBLISHED', '工业炉窑气凝胶隔热方案', '面向工业炉窑和高温设备的气凝胶隔热降耗方案。', '工业炉窑,高温隔热,工业降碳', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM application_scene WHERE slug = 'industrial-furnace')
UNION ALL
SELECT '新能源汽车', 'new-energy-vehicles', '以轻量隔热守护热安全',
       '面向新能源汽车热管理和关键部件隔热需求，提供轻量化复合材料方向。',
       '有限空间内需要兼顾隔热、安全、轻量化与结构集成。',
       '围绕目标部位、温度边界和装配方式开展气凝胶毡材及定制复合方案验证。',
       '<h2>场景说明</h2><p>支持从材料样品、复合结构到部件级验证的协同开发。</p>',
       1, 30, 'PUBLISHED', '新能源汽车气凝胶热管理方案', '面向新能源汽车热管理的轻量化气凝胶复合材料方向。', '新能源汽车,热管理,气凝胶毡', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM application_scene WHERE slug = 'new-energy-vehicles')
UNION ALL
SELECT '石油化工', 'petrochemical', '面向复杂管线的可靠隔热',
       '服务管道、储罐和工艺设备的保温、防烫及节能需求。',
       '管线与设备形态复杂，常面临热损失、空间受限和维护便利性要求。',
       '结合介质温度、管径、检修频次与环境条件配置柔性包覆或复合隔热层。',
       '<h2>场景说明</h2><p>针对管线、阀门和设备外表面提供可施工、可维护的隔热建议。</p>',
       1, 40, 'PUBLISHED', '石油化工气凝胶隔热方案', '面向石油化工管道与设备的气凝胶保温、防烫和节能方案。', '石油化工,管道保温,防烫隔热', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM application_scene WHERE slug = 'petrochemical')
UNION ALL
SELECT '粮仓仓储', 'grain-storage', '稳定仓储温度，降低环境扰动',
       '面向粮仓及仓储建筑的围护结构隔热和温度稳定需求。',
       '太阳辐射和外界温差会增加仓内温度波动及能耗。',
       '通过屋面和墙体气凝胶隔热涂层，降低围护结构热传递并改善仓储环境。',
       '<h2>场景说明</h2><p>依据仓体结构和当地气候条件制定涂层选型与施工建议。</p>',
       1, 50, 'PUBLISHED', '粮仓仓储气凝胶隔热方案', '面向粮仓和仓储建筑的气凝胶围护结构隔热方案。', '粮仓隔热,仓储节能,隔热涂料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM application_scene WHERE slug = 'grain-storage')
UNION ALL
SELECT '通信机房', 'communication-data-room', '减轻热负荷，助力稳定运行',
       '面向通信机房及设备空间的围护隔热和热环境改善需求。',
       '设备持续发热叠加外部热负荷，会增加制冷压力与运行能耗。',
       '对屋面、墙体等围护部位配置气凝胶隔热涂层，辅助降低外部热量进入。',
       '<h2>场景说明</h2><p>结合机房围护结构、运行条件和施工窗口提供材料与工艺建议。</p>',
       1, 60, 'PUBLISHED', '通信机房气凝胶隔热方案', '面向通信机房围护结构的气凝胶隔热与节能方向。', '通信机房,隔热节能,气凝胶涂料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM application_scene WHERE slug = 'communication-data-room');

SET @scene_building = (SELECT id FROM application_scene WHERE slug = 'building-energy-saving' LIMIT 1);
SET @scene_furnace = (SELECT id FROM application_scene WHERE slug = 'industrial-furnace' LIMIT 1);
SET @scene_vehicle = (SELECT id FROM application_scene WHERE slug = 'new-energy-vehicles' LIMIT 1);
SET @scene_petrochemical = (SELECT id FROM application_scene WHERE slug = 'petrochemical' LIMIT 1);
SET @scene_grain = (SELECT id FROM application_scene WHERE slug = 'grain-storage' LIMIT 1);
SET @scene_room = (SELECT id FROM application_scene WHERE slug = 'communication-data-room' LIMIT 1);

-- 文档中出现的示范案例（不补写未经文档确认的量化效果）
INSERT INTO case_project
    (scene_id, title, slug, client_name, location, summary, background, customer_need, solution, implementation, result_summary,
     content_html, is_featured, sort_order, status, seo_title, seo_description, seo_keywords, version, published_at, created_at, updated_at, deleted)
SELECT @scene_building, '三一云谷建筑隔热应用项目', 'sany-cloud-valley-building-insulation', '三一集团', '三一云谷',
       '围绕建筑围护结构隔热需求开展气凝胶材料应用与方案验证。',
       '项目关注建筑节能与围护结构隔热性能，需要兼顾施工条件和长期使用需求。',
       '根据具体墙体构造选择适配材料，并通过样板验证确认施工工艺。',
       '采用气凝胶隔热材料与配套施工体系，形成面向项目条件的解决方案。',
       '完成需求梳理、材料选型、样板验证与项目应用。',
       '项目形成可供类似建筑节能场景参考的材料应用路径。',
       '<h2>项目概述</h2><p>项目围绕建筑围护结构隔热需求，开展材料选型、样板验证和施工适配。</p><h2>方案价值</h2><p>以薄层隔热材料为基础，为建筑节能项目提供可落地的技术路线。</p>',
       1, 10, 'PUBLISHED', '三一云谷建筑隔热应用项目', '气凝胶材料在三一云谷建筑隔热场景中的应用案例。', '三一云谷,建筑节能,气凝胶', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM case_project WHERE slug = 'sany-cloud-valley-building-insulation')
UNION ALL
SELECT @scene_building, '明和产业园外墙内保温项目', 'minghe-industrial-park-wall-insulation', '明和产业园', '湖南',
       '针对产业园建筑外墙内保温需求，开展气凝胶隔热材料工程应用。',
       '既有建筑空间和施工条件对保温层厚度、基层适配和施工组织提出综合要求。',
       '需要在满足隔热目标的同时，兼顾室内空间与工程实施条件。',
       '依据基层和构造条件配置气凝胶隔热涂层及配套施工工艺。',
       '通过现场勘察、样板测试和分区施工推进项目落地。',
       '形成产业园建筑内保温场景的气凝胶材料应用示范。',
       '<h2>项目概述</h2><p>围绕产业园建筑外墙内保温需求，完成材料选型、基层处理与施工方案适配。</p><h2>应用方向</h2><p>为既有建筑节能改造和空间受限的保温项目提供参考。</p>',
       1, 20, 'PUBLISHED', '明和产业园外墙内保温项目', '气凝胶隔热材料在产业园建筑内保温场景中的应用案例。', '产业园,内保温,气凝胶涂料', 0, NOW(), NOW(), NOW(), 0
FROM DUAL WHERE @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM case_project WHERE slug = 'minghe-industrial-park-wall-insulation');

-- 产品与场景关联
SET @product_coating = (SELECT id FROM product WHERE slug = 'aerogel-thermal-insulation-coating' LIMIT 1);
SET @product_exterior = (SELECT id FROM product WHERE slug = 'exterior-weather-resistant-insulation-coating' LIMIT 1);
SET @product_blanket = (SELECT id FROM product WHERE slug = 'aerogel-flexible-blanket' LIMIT 1);
SET @product_board = (SELECT id FROM product WHERE slug = 'aerogel-insulation-board' LIMIT 1);
SET @product_slurry = (SELECT id FROM product WHERE slug = 'modified-aerogel-slurry' LIMIT 1);
SET @product_masterbatch = (SELECT id FROM product WHERE slug = 'aerogel-composite-masterbatch' LIMIT 1);
SET @product_powder = (SELECT id FROM product WHERE slug = 'silica-aerogel-powder' LIMIT 1);
SET @product_dispersion = (SELECT id FROM product WHERE slug = 'waterborne-aerogel-dispersion' LIMIT 1);

INSERT INTO product_scene_rel (product_id, scene_id, sort_order)
SELECT @product_coating, @scene_building, 10 FROM DUAL
WHERE @product_coating IS NOT NULL AND @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_coating AND scene_id = @scene_building)
UNION ALL SELECT @product_coating, @scene_grain, 20 FROM DUAL
WHERE @product_coating IS NOT NULL AND @scene_grain IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_coating AND scene_id = @scene_grain)
UNION ALL SELECT @product_coating, @scene_room, 30 FROM DUAL
WHERE @product_coating IS NOT NULL AND @scene_room IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_coating AND scene_id = @scene_room)
UNION ALL SELECT @product_exterior, @scene_building, 10 FROM DUAL
WHERE @product_exterior IS NOT NULL AND @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_exterior AND scene_id = @scene_building)
UNION ALL SELECT @product_blanket, @scene_furnace, 10 FROM DUAL
WHERE @product_blanket IS NOT NULL AND @scene_furnace IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_blanket AND scene_id = @scene_furnace)
UNION ALL SELECT @product_blanket, @scene_petrochemical, 20 FROM DUAL
WHERE @product_blanket IS NOT NULL AND @scene_petrochemical IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_blanket AND scene_id = @scene_petrochemical)
UNION ALL SELECT @product_blanket, @scene_vehicle, 30 FROM DUAL
WHERE @product_blanket IS NOT NULL AND @scene_vehicle IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_blanket AND scene_id = @scene_vehicle)
UNION ALL SELECT @product_board, @scene_building, 10 FROM DUAL
WHERE @product_board IS NOT NULL AND @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_board AND scene_id = @scene_building)
UNION ALL SELECT @product_slurry, @scene_building, 10 FROM DUAL
WHERE @product_slurry IS NOT NULL AND @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_slurry AND scene_id = @scene_building)
UNION ALL SELECT @product_masterbatch, @scene_vehicle, 10 FROM DUAL
WHERE @product_masterbatch IS NOT NULL AND @scene_vehicle IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_masterbatch AND scene_id = @scene_vehicle)
UNION ALL SELECT @product_powder, @scene_furnace, 10 FROM DUAL
WHERE @product_powder IS NOT NULL AND @scene_furnace IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_powder AND scene_id = @scene_furnace)
UNION ALL SELECT @product_dispersion, @scene_building, 10 FROM DUAL
WHERE @product_dispersion IS NOT NULL AND @scene_building IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_scene_rel WHERE product_id = @product_dispersion AND scene_id = @scene_building);

SET @case_sany = (SELECT id FROM case_project WHERE slug = 'sany-cloud-valley-building-insulation' LIMIT 1);
SET @case_minghe = (SELECT id FROM case_project WHERE slug = 'minghe-industrial-park-wall-insulation' LIMIT 1);

INSERT INTO product_case_rel (product_id, case_id, sort_order)
SELECT @product_coating, @case_sany, 10 FROM DUAL
WHERE @product_coating IS NOT NULL AND @case_sany IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_case_rel WHERE product_id = @product_coating AND case_id = @case_sany)
UNION ALL SELECT @product_exterior, @case_sany, 20 FROM DUAL
WHERE @product_exterior IS NOT NULL AND @case_sany IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_case_rel WHERE product_id = @product_exterior AND case_id = @case_sany)
UNION ALL SELECT @product_coating, @case_minghe, 10 FROM DUAL
WHERE @product_coating IS NOT NULL AND @case_minghe IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_case_rel WHERE product_id = @product_coating AND case_id = @case_minghe)
UNION ALL SELECT @product_board, @case_minghe, 20 FROM DUAL
WHERE @product_board IS NOT NULL AND @case_minghe IS NOT NULL AND NOT EXISTS (SELECT 1 FROM product_case_rel WHERE product_id = @product_board AND case_id = @case_minghe);

-- 技术研发页
INSERT INTO technology_page
    (page_key, eyebrow, title, highlight_text, summary, cta_label, cta_target, capability_rows_json, pillars_json,
     content_html, status, published_at, version, created_at, updated_at, deleted)
SELECT 'technology', '核心技术', '新一代常压', '干燥技术',
       '通过溶胶—凝胶调控、环保型疏水改性及复合结构保护，推动气凝胶实现绿色、低成本和规模化生产。',
       '与技术团队交流', '/cooperation',
       JSON_ARRAY(
           JSON_OBJECT('title', '材料基础性能', '孔隙率', '80%–99.8%', '密度', '≤1.5 kg/m³', '导热系数', '≤0.012 W/(m·K)（25°C）'),
           JSON_OBJECT('title', '产业化能力', '干燥路线', '新一代常压干燥', '纳米孔结构保留率', '>95%', '综合生产成本', '降低40%以上')
       ),
       JSON_ARRAY(
           JSON_OBJECT('title', '溶胶—凝胶调控', '技术重点', '调控纳米骨架形成过程，兼顾结构与性能'),
           JSON_OBJECT('title', '环保型疏水改性', '技术重点', '面向常压干燥和复合应用进行界面设计'),
           JSON_OBJECT('title', '复合结构保护', '技术重点', '通过粉碎、分散和结构保护提升复合过程中的孔结构保留')
       ),
       '<h3>技术路线</h3><p>新一代常压干燥技术面向传统超临界工艺的高投入、高能耗瓶颈，通过材料设计与工艺协同提升产业化可行性。</p><h3>复合工艺</h3><p>围绕粉碎、分散和结构保护三个关键环节，让气凝胶纳米多孔结构更好地进入涂料、毡板及其他复合材料。</p>',
       'PUBLISHED', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM technology_page WHERE page_key = 'technology');

-- 合作伙伴（文档列举企业）
INSERT INTO partner
    (name, slug, category, summary, is_featured, sort_order, status, seo_title, seo_description, seo_keywords, published_at, version, created_at, updated_at, deleted)
SELECT '三一集团', 'sany-group', '产业合作伙伴', '围绕气凝胶材料应用和项目场景开展合作。', 1, 10, 'PUBLISHED', '三一集团合作伙伴', '奥飞新材产业合作伙伴。', '合作伙伴,三一集团', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM partner WHERE slug = 'sany-group')
UNION ALL
SELECT '立邦中国', 'nippon-paint-china', '产品合作伙伴', '围绕功能涂层与建筑节能应用开展协同。', 1, 20, 'PUBLISHED', '立邦中国合作伙伴', '奥飞新材产品合作伙伴。', '合作伙伴,立邦中国', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM partner WHERE slug = 'nippon-paint-china')
UNION ALL
SELECT '中国化学', 'china-chemical', '产业合作伙伴', '面向工业与工程应用场景开展材料合作。', 1, 30, 'PUBLISHED', '中国化学合作伙伴', '奥飞新材产业合作伙伴。', '合作伙伴,中国化学', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM partner WHERE slug = 'china-chemical')
UNION ALL
SELECT '中国建筑', 'china-state-construction', '工程合作伙伴', '围绕建筑节能与工程应用探索合作。', 1, 40, 'PUBLISHED', '中国建筑合作伙伴', '奥飞新材工程合作伙伴。', '合作伙伴,中国建筑', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM partner WHERE slug = 'china-state-construction')
UNION ALL
SELECT '中国船舶集团', 'china-state-shipbuilding', '产业合作伙伴', '围绕高性能隔热材料及产业应用开展交流合作。', 1, 50, 'PUBLISHED', '中国船舶集团合作伙伴', '奥飞新材产业合作伙伴。', '合作伙伴,中国船舶集团', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM partner WHERE slug = 'china-state-shipbuilding');

-- 可编辑单页元数据；正文留空以继续使用前端现有的完整团队、历程和合作版式
INSERT INTO content_page
    (page_key, title, category, summary, content_html, is_featured, sort_order, status,
     seo_title, seo_description, seo_keywords, published_at, version, created_at, updated_at, deleted)
SELECT 'about', '关于奥飞新材', '企业介绍',
       '湖南奥飞新材料有限公司成立于2022年7月25日，专注于气凝胶技术开发、复合产品及产业化应用。',
       NULL, 1, 10, 'PUBLISHED', '关于奥飞新材', '了解奥飞新材、研发团队、技术积淀与研产销布局。', '奥飞新材,气凝胶,中南大学,企业介绍', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM content_page WHERE page_key = 'about')
UNION ALL
SELECT 'cooperation', '开放共赢，共建气凝胶产业生态', '商务合作',
       '面向经销商、复合产品开发伙伴和产业链伙伴提供多层次合作模式。',
       NULL, 1, 20, 'PUBLISHED', '奥飞新材商务合作', '经销商合作、复合产品开发与气凝胶产业链合作。', '商务合作,经销商,联合开发,气凝胶产业链', NOW(), 0, NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM content_page WHERE page_key = 'cooperation');

-- 新闻/知识展示内容：采用文档中的技术与应用主题，不虚构外部事件日期
SET @category_company_news = (SELECT id FROM article_category WHERE code = 'company_news' LIMIT 1);
SET @category_research = (SELECT id FROM article_category WHERE code = 'research' LIMIT 1);
SET @category_application = (SELECT id FROM article_category WHERE code = 'application_technology' LIMIT 1);

INSERT INTO article
    (category_id, title, slug, summary, content_html, content_text, author_name, published_at, status, sort_order,
     is_featured, home_slot, home_sort_order, seo_title, seo_description, seo_keywords, version, created_at, updated_at, deleted)
SELECT @category_company_news, '奥飞新材：以新材料之力服务“双碳”战略', 'aofei-materials-serving-dual-carbon-strategy',
       '依托高校科研力量与产业化实践，奥飞新材聚焦气凝胶材料、复合产品和场景解决方案。',
       '<p>湖南奥飞新材料有限公司以节能、环保、安全的底层材料革新为目标，聚焦气凝胶技术开发及产业化应用。</p><h2>从材料到场景</h2><p>公司围绕气凝胶粉体、分散体、涂料、毡材和板材构建产品体系，服务建筑节能、工业降碳、新能源热安全等方向。</p><h2>从研发到交付</h2><p>依托中南大学粉末冶金全国重点实验室科研积淀，推动新一代常压干燥技术与复合材料技术进入真实项目。</p>',
       '湖南奥飞新材料有限公司聚焦气凝胶技术开发及产业化应用，围绕材料、产品和场景解决方案服务双碳战略。',
       '奥飞新材', NOW(), 'PUBLISHED', 10, 1, 'NEWS_PRIMARY', 10,
       '奥飞新材：以新材料之力服务双碳战略', '奥飞新材围绕气凝胶材料、复合产品和应用场景推动节能降碳。', '奥飞新材,双碳,气凝胶,新材料', 0, NOW(), NOW(), 0
FROM DUAL WHERE @category_company_news IS NOT NULL AND NOT EXISTS (SELECT 1 FROM article WHERE slug = 'aofei-materials-serving-dual-carbon-strategy')
UNION ALL
SELECT @category_research, '新一代常压干燥技术：让气凝胶规模化更可行', 'next-generation-ambient-pressure-drying',
       '通过材料设计与工艺协同，降低传统超临界路线在设备、能耗和连续生产方面的门槛。',
       '<p>气凝胶优异的隔热能力来自其纳米多孔结构。如何在干燥和复合过程中保留这一结构，是实现规模化应用的关键。</p><h2>常压干燥路线</h2><p>新一代常压干燥技术通过溶胶—凝胶调控、环保型疏水改性等手段，面向低成本、绿色和连续化生产。</p><h2>复合过程保护</h2><p>在粉碎、分散及产品复合环节进行结构保护，让气凝胶功能更好地进入涂料、毡板和其他材料体系。</p>',
       '新一代常压干燥技术通过材料设计与工艺协同，推动气凝胶低成本、绿色和规模化生产。',
       '奥飞新材技术团队', NOW(), 'PUBLISHED', 20, 1, 'NEWS_SECONDARY', 20,
       '新一代气凝胶常压干燥技术', '了解奥飞新材新一代常压干燥及气凝胶复合结构保护技术。', '气凝胶,常压干燥,纳米多孔,材料技术', 0, NOW(), NOW(), 0
FROM DUAL WHERE @category_research IS NOT NULL AND NOT EXISTS (SELECT 1 FROM article WHERE slug = 'next-generation-ambient-pressure-drying')
UNION ALL
SELECT @category_application, '气凝胶材料如何服务六类典型隔热场景', 'aerogel-six-application-scenarios',
       '从建筑节能到通信机房，气凝胶材料需要围绕工况、构造和施工方式形成场景化方案。',
       '<p>隔热材料的价值不只取决于单一参数，还取决于能否适配真实工况。</p><h2>六类典型场景</h2><p>奥飞新材重点关注建筑节能、工业炉窑、新能源汽车、石油化工、粮仓仓储和通信机房。</p><h2>场景化方法</h2><p>通过需求沟通、材料选型、样品或样板验证和项目实施，将粉体、涂料、毡材、板材等产品与具体构造相匹配。</p>',
       '气凝胶材料在建筑节能、工业炉窑、新能源汽车、石油化工、粮仓仓储和通信机房中的场景化应用。',
       '奥飞新材应用团队', NOW(), 'PUBLISHED', 30, 1, 'NEWS_LIST', 30,
       '气凝胶六类典型应用场景', '气凝胶材料在六类典型隔热场景中的产品选择与应用思路。', '气凝胶应用,建筑节能,工业隔热,新能源汽车', 0, NOW(), NOW(), 0
FROM DUAL WHERE @category_application IS NOT NULL AND NOT EXISTS (SELECT 1 FROM article WHERE slug = 'aerogel-six-application-scenarios');

SET @article_dual_carbon = (SELECT id FROM article WHERE slug = 'aofei-materials-serving-dual-carbon-strategy' LIMIT 1);
SET @article_drying = (SELECT id FROM article WHERE slug = 'next-generation-ambient-pressure-drying' LIMIT 1);
SET @article_scenarios = (SELECT id FROM article WHERE slug = 'aerogel-six-application-scenarios' LIMIT 1);

INSERT INTO article_category_relation (article_id, category_id, sort_order, created_at, updated_at, deleted)
SELECT @article_dual_carbon, @category_company_news, 10, NOW(), NOW(), 0 FROM DUAL
WHERE @article_dual_carbon IS NOT NULL AND @category_company_news IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM article_category_relation WHERE article_id = @article_dual_carbon AND category_id = @category_company_news)
UNION ALL
SELECT @article_drying, @category_research, 20, NOW(), NOW(), 0 FROM DUAL
WHERE @article_drying IS NOT NULL AND @category_research IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM article_category_relation WHERE article_id = @article_drying AND category_id = @category_research)
UNION ALL
SELECT @article_scenarios, @category_application, 30, NOW(), NOW(), 0 FROM DUAL
WHERE @article_scenarios IS NOT NULL AND @category_application IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM article_category_relation WHERE article_id = @article_scenarios AND category_id = @category_application);

COMMIT;

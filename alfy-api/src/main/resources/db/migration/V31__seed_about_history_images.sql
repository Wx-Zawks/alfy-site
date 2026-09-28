-- 发展历程：仅同步公司提供的图片，既有文字内容保持不变。
-- 已由后台保存过的结构化数据不覆盖，避免发布时覆盖运营人员后续编辑。
UPDATE content_page
SET content_json = JSON_OBJECT(
    'historyItems', JSON_ARRAY(
        JSON_OBJECT(
            'date', '2026年1月',
            'title', '新一代气凝胶常压干燥技术发布',
            'text', '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。',
            'imageUrl', '/images/about-history-01.jpg'
        ),
        JSON_OBJECT(
            'date', '2024年6月',
            'title', '新一代气凝胶常压干燥技术突破',
            'text', '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。',
            'imageUrl', '/images/about-history-02.png'
        ),
        JSON_OBJECT(
            'date', '2019年4月',
            'title', '气凝胶柔性复合材料中试',
            'text', '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。',
            'imageUrl', '/images/about-history-03.png'
        ),
        JSON_OBJECT(
            'date', '2015年6月',
            'title', '气凝胶分散体技术开发',
            'text', '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。',
            'imageUrl', '/images/about-history-04.jpg'
        ),
        JSON_OBJECT(
            'date', '2022年7月',
            'title', '奥飞公司成立及产业化验证',
            'text', '“新一代气凝胶及其复合材料技术发布暨产业发展签约大会”在湖南长沙隆重举行。',
            'imageUrl', '/images/about-history-05.png'
        ),
        JSON_OBJECT('date', '2026年', 'imageUrl', '/images/about-history-06.jpg')
    )
)
WHERE page_key = 'about'
  AND (content_json IS NULL OR JSON_LENGTH(content_json) = 0);

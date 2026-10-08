-- 关于我们页面：核心研发团队 与 发展引擎（产业布局）
-- 此前两部分内容硬编码在前端 alfy-web/pages/about.vue，现迁入数据库统一由后台维护。

CREATE TABLE team_member
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role           VARCHAR(100) NOT NULL COMMENT '头衔，如：技术带头人',
    name           VARCHAR(100) NOT NULL COMMENT '姓名',
    bio            VARCHAR(1000) NULL COMMENT '个人简介',
    photo_media_id BIGINT UNSIGNED NULL COMMENT '头像素材，关联 media_asset.id',
    sort_order     INT NOT NULL DEFAULT 0 COMMENT '展示顺序，小值在前',
    enabled        TINYINT NOT NULL DEFAULT 1 COMMENT '1 展示 / 0 隐藏',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT NOT NULL DEFAULT 0,
    version        BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_team_member_public (enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网关于我们-核心研发团队';

CREATE TABLE base_facility
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name           VARCHAR(255) NOT NULL COMMENT '基地/点位名称',
    address        VARCHAR(500) NULL COMMENT '详细地址',
    image_media_id BIGINT UNSIGNED NULL COMMENT '展示图素材，关联 media_asset.id',
    sort_order     INT NOT NULL DEFAULT 0 COMMENT '展示顺序，小值在前',
    enabled        TINYINT NOT NULL DEFAULT 1 COMMENT '1 展示 / 0 隐藏',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT NOT NULL DEFAULT 0,
    version        BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_base_facility_public (enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网关于我们-发展引擎产业布局';

-- 种子数据：与官网当前展示内容一致；图片素材待后台从素材库选择后生效，
-- 前端在 imageUrl 为空时回退到原有静态图片（按名称匹配）。
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术带头人', '周科朝', '教授、博士生导师，原中南大学副校长，粉末冶金全国重点实验室主任。', 10, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member);
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '首席科学家', '宋淼', '中南大学特聘教授、博士生导师、升华学者、国家级高层次青年人才。', 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member);
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术总监', '张丁日', '中南大学博士、奥飞新材董事长、湖南省“优秀创新创业导师”。', 30, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member);
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术顾问', '宋祁朋', '西安电子科技大学副教授，硕士生导师，西安电子科技大学“华山菁英学者”人才基金获得者。', 40, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member);
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术顾问', '蔡圳阳', '中南大学副教授、青年科协秘书长，中国有色金属产业联盟专家委员会委员等。', 50, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member);

INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '湖南省浏阳市研发基地', '湖南省浏阳市永安镇星辰·尚东产业小镇', 10, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility);
INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '湖南省浏阳市生产基地', '湖南省浏阳市永安镇星辰·尚东产业小镇', 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility);
INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '湖南省长沙市天心区销售中心', '长沙市天心区天心数谷创芯中心1-2栋', 30, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility);
INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '中南大学科技园办公点', '中南大学科技园研发总部1栋四楼407房', 40, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility);

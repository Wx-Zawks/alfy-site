-- 修复 V33 种子数据缺陷：多条 INSERT 共用同一 NOT EXISTS 守卫，
-- 实际只写入了第一条记录。此处按名称守卫补齐其余团队成员与基地。

INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '首席科学家', '宋淼', '中南大学特聘教授、博士生导师、升华学者、国家级高层次青年人才。', 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member WHERE name = '宋淼');
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术总监', '张丁日', '中南大学博士、奥飞新材董事长、湖南省“优秀创新创业导师”。', 30, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member WHERE name = '张丁日');
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术顾问', '宋祁朋', '西安电子科技大学副教授，硕士生导师，西安电子科技大学“华山菁英学者”人才基金获得者。', 40, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member WHERE name = '宋祁朋');
INSERT INTO team_member (role, name, bio, sort_order, enabled)
SELECT '技术顾问', '蔡圳阳', '中南大学副教授、青年科协秘书长，中国有色金属产业联盟专家委员会委员等。', 50, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM team_member WHERE name = '蔡圳阳');

INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '湖南省浏阳市生产基地', '湖南省浏阳市永安镇星辰·尚东产业小镇', 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility WHERE name = '湖南省浏阳市生产基地');
INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '湖南省长沙市天心区销售中心', '长沙市天心区天心数谷创芯中心1-2栋', 30, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility WHERE name = '湖南省长沙市天心区销售中心');
INSERT INTO base_facility (name, address, sort_order, enabled)
SELECT '中南大学科技园办公点', '中南大学科技园研发总部1栋四楼407房', 40, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM base_facility WHERE name = '中南大学科技园办公点');

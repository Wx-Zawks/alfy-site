ALTER TABLE product_scene_rel
    ADD COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT FIRST,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_product_scene_rel (product_id, scene_id);

ALTER TABLE product_case_rel
    ADD COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT FIRST,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uk_product_case_rel (product_id, case_id);

ALTER TABLE application_scene ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER seo_keywords;
ALTER TABLE case_project ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER seo_keywords;

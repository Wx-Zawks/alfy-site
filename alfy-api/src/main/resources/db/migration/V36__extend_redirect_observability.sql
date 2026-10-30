ALTER TABLE redirect_rule
    DROP INDEX uk_redirect_source_path,
    ADD COLUMN source_host VARCHAR(255) NOT NULL DEFAULT '' AFTER id,
    ADD COLUMN status_code SMALLINT NOT NULL DEFAULT 301 AFTER target_url,
    ADD COLUMN preserve_query TINYINT NOT NULL DEFAULT 0 AFTER status_code,
    ADD COLUMN note VARCHAR(500) NULL AFTER preserve_query,
    ADD COLUMN hit_count BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER enabled,
    ADD COLUMN last_hit_at DATETIME NULL AFTER hit_count;

CREATE INDEX idx_redirect_lookup
    ON redirect_rule (source_host, source_path(500), enabled, deleted);

CREATE INDEX idx_redirect_last_hit
    ON redirect_rule (last_hit_at);

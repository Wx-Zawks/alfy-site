CREATE TABLE redirect_rule
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_path VARCHAR(700) NOT NULL,
    target_url  VARCHAR(1024) NOT NULL,
    enabled     TINYINT NOT NULL DEFAULT 1,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_redirect_source_path (source_path),
    KEY idx_redirect_enabled (enabled)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '旧站路径 301 跳转规则';

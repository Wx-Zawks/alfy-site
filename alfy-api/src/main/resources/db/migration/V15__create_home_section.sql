CREATE TABLE home_section
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    section_key       VARCHAR(64) NOT NULL,
    label             VARCHAR(100) NOT NULL,
    eyebrow           VARCHAR(255) NULL,
    title             VARCHAR(255) NOT NULL,
    highlight_text    VARCHAR(255) NULL,
    description_text  VARCHAR(1000) NULL,
    image_media_id    BIGINT UNSIGNED NULL,
    mobile_media_id   BIGINT UNSIGNED NULL,
    button_label      VARCHAR(100) NULL,
    button_target     VARCHAR(500) NULL,
    enabled           TINYINT NOT NULL DEFAULT 1,
    sort_order        INT NOT NULL DEFAULT 0,
    version           BIGINT NOT NULL DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_home_section_key (section_key),
    KEY idx_home_section_public (enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '官网首页固定区块配置';

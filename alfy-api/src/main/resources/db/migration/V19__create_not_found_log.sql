CREATE TABLE not_found_log
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    path           VARCHAR(1024)    NOT NULL COMMENT '未找到的访问路径',
    last_referer   VARCHAR(1024)    NULL COMMENT '最近来源页面',
    hit_count      BIGINT UNSIGNED  NOT NULL DEFAULT 1 COMMENT '访问次数',
    first_seen_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次出现时间',
    last_seen_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近出现时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_not_found_log_path (path(255)),
    KEY idx_not_found_log_count_time (hit_count, last_seen_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '官网 404 访问统计';

CREATE TABLE admin_refresh_token
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '管理员 ID',
    token_hash  CHAR(64)        NOT NULL COMMENT '刷新令牌 SHA-256',
    expires_at  DATETIME        NOT NULL COMMENT '过期时间',
    revoked_at  DATETIME        NULL COMMENT '注销或替换时间',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_refresh_token_hash (token_hash),
    KEY idx_admin_refresh_token_user_expires (user_id, expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '管理员刷新令牌';

CREATE TABLE admin_operation_log
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id BIGINT UNSIGNED NULL COMMENT '操作管理员 ID',
    action      VARCHAR(64)     NOT NULL COMMENT '操作类型',
    object_type VARCHAR(64)     NOT NULL COMMENT '对象类型',
    object_id   BIGINT UNSIGNED NULL COMMENT '对象 ID',
    detail      VARCHAR(1000)   NULL COMMENT '不含敏感信息的摘要',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_admin_operation_log_operator_time (operator_id, created_at),
    KEY idx_admin_operation_log_object (object_type, object_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '后台操作审计日志';

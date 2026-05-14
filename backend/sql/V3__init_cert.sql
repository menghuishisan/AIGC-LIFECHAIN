-- =============================================================
-- V3: Certificate & Verify Domain
-- =============================================================

CREATE TABLE `certificate` (
    `id`                BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `cert_no`           VARCHAR(64)  NOT NULL               COMMENT '证书编号',
    `work_id`           BIGINT       NOT NULL               COMMENT '作品ID',
    `work_no`           VARCHAR(64)  NOT NULL               COMMENT '作品编号',
    `claim_id`          BIGINT       NOT NULL               COMMENT '确权申请ID',
    `claim_no`          VARCHAR(64)  NOT NULL               COMMENT '确权编号',
    `holder_account_id` BIGINT       NOT NULL               COMMENT '持有人账户ID',
    `holder_did_id`     BIGINT       NOT NULL               COMMENT '持有人DID ID',
    `status`            VARCHAR(30)  NOT NULL               COMMENT '证书状态',
    `cert_hash`         VARCHAR(128)          DEFAULT NULL  COMMENT '证书哈希',
    `cert_file_url`     VARCHAR(512)          DEFAULT NULL  COMMENT '证书文件地址',
    `version`           INT          NOT NULL DEFAULT 1     COMMENT '版本号',
    `previous_cert_id`  BIGINT                DEFAULT NULL  COMMENT '上一版本证书ID',
    `issue_time`        DATETIME              DEFAULT NULL  COMMENT '签发时间(UTC)',
    `expire_time`       DATETIME              DEFAULT NULL  COMMENT '到期时间(UTC)',
    `revoke_time`       DATETIME              DEFAULT NULL  COMMENT '吊销时间(UTC)',
    `revoke_reason`     VARCHAR(512)          DEFAULT NULL  COMMENT '吊销原因',
    `deleted_flag`      TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`        DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`        DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cert_no` (`cert_no`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_claim_id` (`claim_id`),
    KEY `idx_holder_account_id` (`holder_account_id`),
    KEY `idx_status` (`status`),
    KEY `idx_cert_hash` (`cert_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书表';

CREATE TABLE `certificate_template` (
    `id`               BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `template_name`    VARCHAR(128) NOT NULL               COMMENT '模板名称',
    `template_code`    VARCHAR(64)  NOT NULL               COMMENT '模板编码',
    `template_content` TEXT         NOT NULL               COMMENT '模板内容（HTML/JSON）',
    `status`           VARCHAR(20)  NOT NULL               COMMENT '状态(ACTIVE/INACTIVE)',
    `description`      VARCHAR(512)          DEFAULT NULL  COMMENT '描述',
    `deleted_flag`     TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`       DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`       DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书模板表';

CREATE TABLE `verify_query_log` (
    `id`               BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `query_type`       VARCHAR(30)  NOT NULL               COMMENT '查询类型(CERT_NO/WORK_NO/FILE_HASH)',
    `query_value`      VARCHAR(256) NOT NULL               COMMENT '查询值',
    `query_source`     VARCHAR(30)  NOT NULL               COMMENT '来源(PUBLIC/LOGIN/REGULATOR)',
    `query_account_id` BIGINT                DEFAULT NULL  COMMENT '查询人账户ID（可为空）',
    `query_ip`         VARCHAR(50)           DEFAULT NULL  COMMENT '查询IP',
    `match_found`      TINYINT      NOT NULL               COMMENT '是否匹配到 0-否 1-是',
    `result_summary`   VARCHAR(512)          DEFAULT NULL  COMMENT '结果摘要',
    `query_time`       DATETIME     NOT NULL               COMMENT '查询时间(UTC)',
    `deleted_flag`     TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`       DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`       DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_query_type` (`query_type`),
    KEY `idx_query_value` (`query_value`),
    KEY `idx_query_account_id` (`query_account_id`),
    KEY `idx_query_time` (`query_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证查询日志表';

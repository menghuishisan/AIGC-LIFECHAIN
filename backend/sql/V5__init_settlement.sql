-- =============================================================
-- V5: Settlement Domain
-- =============================================================

CREATE TABLE IF NOT EXISTS `settle_template` (
    `id`            BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `template_name` VARCHAR(128) NOT NULL               COMMENT '模板名称',
    `template_code` VARCHAR(64)  NOT NULL               COMMENT '模板编码',
    `description`   VARCHAR(512)          DEFAULT NULL  COMMENT '描述',
    `status`        VARCHAR(20)  NOT NULL               COMMENT '状态(ACTIVE/INACTIVE)',
    `deleted_flag`  TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`    DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`    DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算模板表';

CREATE TABLE IF NOT EXISTS `settle_template_item` (
    `id`           BIGINT        NOT NULL               COMMENT '主键（雪花ID）',
    `template_id`  BIGINT        NOT NULL               COMMENT '结算模板ID',
    `role_type`    VARCHAR(30)   NOT NULL               COMMENT '角色类型(CREATOR/PLATFORM/OTHER)',
    `ratio`        DECIMAL(5,4)  NOT NULL               COMMENT '分账比例',
    `description`  VARCHAR(256)           DEFAULT NULL  COMMENT '描述',
    `deleted_flag` TINYINT       NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME      NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME      NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算模板明细表';

CREATE TABLE IF NOT EXISTS `work_settle_rule` (
    `id`                 BIGINT        NOT NULL               COMMENT '主键（雪花ID）',
    `work_id`            BIGINT        NOT NULL               COMMENT '作品ID',
    `work_no`            VARCHAR(64)   NOT NULL               COMMENT '作品编号',
    `template_id`        BIGINT                 DEFAULT NULL  COMMENT '结算模板ID',
    `creator_account_id` BIGINT        NOT NULL               COMMENT '创作者账户ID',
    `creator_ratio`      DECIMAL(5,4)  NOT NULL               COMMENT '创作者分成比例',
    `platform_ratio`     DECIMAL(5,4)  NOT NULL               COMMENT '平台分成比例',
    `effective_time`     DATETIME               DEFAULT NULL  COMMENT '生效时间(UTC)',
    `status`             VARCHAR(20)   NOT NULL               COMMENT '状态(ACTIVE/INACTIVE)',
    `deleted_flag`       TINYINT       NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`         DATETIME      NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`         DATETIME      NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_work_no` (`work_no`),
    KEY `idx_creator_account_id` (`creator_account_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品结算规则表';

CREATE TABLE IF NOT EXISTS `settlement_record` (
    `id`            BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `settle_no`     VARCHAR(64)  NOT NULL               COMMENT '结算编号',
    `order_id`      BIGINT       NOT NULL               COMMENT '订单ID',
    `order_no`      VARCHAR(64)  NOT NULL               COMMENT '订单编号',
    `work_id`       BIGINT       NOT NULL               COMMENT '作品ID',
    `work_no`       VARCHAR(64)  NOT NULL               COMMENT '作品编号',
    `total_amount`  BIGINT       NOT NULL               COMMENT '结算总金额（分）',
    `status`        VARCHAR(30)  NOT NULL               COMMENT '结算状态',
    `chain_status`  VARCHAR(30)  NOT NULL DEFAULT ''    COMMENT '链上状态',
    `settle_time`   DATETIME              DEFAULT NULL  COMMENT '结算时间(UTC)',
    `complete_time` DATETIME              DEFAULT NULL  COMMENT '完成时间(UTC)',
    `tx_hash`       VARCHAR(128)          DEFAULT NULL  COMMENT '交易哈希',
    `block_height`  BIGINT                DEFAULT NULL  COMMENT '区块高度',
    `fail_reason`   VARCHAR(512)          DEFAULT NULL  COMMENT '失败原因',
    `retry_count`   INT          NOT NULL DEFAULT 0     COMMENT '重试次数',
    `request_id`    VARCHAR(64)           DEFAULT NULL  COMMENT '幂等ID',
    `deleted_flag`  TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `version`       INT          NOT NULL DEFAULT 0     COMMENT '乐观锁版本号',
    `created_at`    DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`    DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settle_no` (`settle_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_work_no` (`work_no`),
    KEY `idx_status` (`status`),
    KEY `idx_chain_status` (`chain_status`),
    KEY `idx_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算记录表';

CREATE TABLE IF NOT EXISTS `settlement_item` (
    `id`           BIGINT        NOT NULL               COMMENT '主键（雪花ID）',
    `settle_id`    BIGINT        NOT NULL               COMMENT '结算记录ID',
    `settle_no`    VARCHAR(64)   NOT NULL               COMMENT '结算编号',
    `account_id`   BIGINT        NOT NULL               COMMENT '收款账户ID',
    `role_type`    VARCHAR(30)   NOT NULL               COMMENT '角色类型',
    `ratio`        DECIMAL(5,4)  NOT NULL               COMMENT '分账比例',
    `amount`       BIGINT        NOT NULL               COMMENT '金额（分）',
    `status`       VARCHAR(20)   NOT NULL               COMMENT '状态(PENDING/SUCCESS/FAILED)',
    `deleted_flag` TINYINT       NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME      NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME      NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_settle_id` (`settle_id`),
    KEY `idx_settle_no` (`settle_no`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算明细表';

CREATE TABLE IF NOT EXISTS `reverse_settlement_record` (
    `id`             BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `reverse_no`     VARCHAR(64)  NOT NULL               COMMENT '逆分账编号',
    `settle_id`      BIGINT       NOT NULL               COMMENT '原结算ID',
    `settle_no`      VARCHAR(64)  NOT NULL               COMMENT '原结算编号',
    `order_id`       BIGINT       NOT NULL               COMMENT '订单ID',
    `order_no`       VARCHAR(64)  NOT NULL               COMMENT '订单编号',
    `reverse_amount` BIGINT       NOT NULL               COMMENT '逆分账金额（分）',
    `status`         VARCHAR(30)  NOT NULL               COMMENT '状态',
    `chain_status`   VARCHAR(30)  NOT NULL DEFAULT ''    COMMENT '链上状态',
    `reason`         VARCHAR(512)          DEFAULT NULL  COMMENT '原因',
    `reason_code`    VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `tx_hash`        VARCHAR(128)          DEFAULT NULL  COMMENT '交易哈希',
    `block_height`   BIGINT                DEFAULT NULL  COMMENT '区块高度',
    `apply_time`     DATETIME     NOT NULL               COMMENT '申请时间(UTC)',
    `complete_time`  DATETIME              DEFAULT NULL  COMMENT '完成时间(UTC)',
    `fail_reason`    VARCHAR(512)          DEFAULT NULL  COMMENT '失败原因',
    `operator_id`    BIGINT                DEFAULT NULL  COMMENT '操作人ID',
    `request_id`     VARCHAR(64)           DEFAULT NULL  COMMENT '幂等ID',
    `deleted_flag`   TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `version`        INT          NOT NULL DEFAULT 0     COMMENT '乐观锁版本号',
    `created_at`     DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`     DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reverse_no` (`reverse_no`),
    KEY `idx_settle_id` (`settle_id`),
    KEY `idx_settle_no` (`settle_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逆分账记录表';

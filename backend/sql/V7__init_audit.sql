-- =============================================================
-- V7: Audit, Trace, Support Domain
-- =============================================================

CREATE TABLE `trace_event` (
    `id`                BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `biz_type`          VARCHAR(30)  NOT NULL               COMMENT '业务类型',
    `biz_id`            BIGINT       NOT NULL               COMMENT '业务ID',
    `biz_no`            VARCHAR(64)           DEFAULT NULL  COMMENT '业务编号',
    `event_type`        VARCHAR(64)  NOT NULL               COMMENT '事件类型',
    `event_description` VARCHAR(512)          DEFAULT NULL  COMMENT '事件描述',
    `operator_id`       BIGINT                DEFAULT NULL  COMMENT '操作人ID',
    `operator_role`     VARCHAR(30)           DEFAULT NULL  COMMENT '操作人角色',
    `event_time`        DATETIME     NOT NULL               COMMENT '事件时间(UTC)',
    `extra_data`        TEXT                  DEFAULT NULL  COMMENT '扩展数据(JSON)',
    `deleted_flag`      TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`        DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`        DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_biz_type_id` (`biz_type`, `biz_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_event_time` (`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='追踪事件表';

CREATE TABLE `audit_log` (
    `id`            BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `target_type`   VARCHAR(30)  NOT NULL               COMMENT '目标类型',
    `target_id`     BIGINT       NOT NULL               COMMENT '目标ID',
    `target_no`     VARCHAR(64)           DEFAULT NULL  COMMENT '目标编号',
    `action`        VARCHAR(64)  NOT NULL               COMMENT '操作动作',
    `action_detail` TEXT                  DEFAULT NULL  COMMENT '操作详情',
    `operator_id`   BIGINT                DEFAULT NULL  COMMENT '操作人ID',
    `operator_role` VARCHAR(30)           DEFAULT NULL  COMMENT '操作人角色',
    `operator_ip`   VARCHAR(50)           DEFAULT NULL  COMMENT '操作人IP',
    `result`        VARCHAR(20)  NOT NULL               COMMENT '操作结果',
    `reason_code`   VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `log_time`      DATETIME     NOT NULL               COMMENT '日志时间(UTC)',
    `deleted_flag`  TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`    DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`    DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_target_type_id` (`target_type`, `target_id`),
    KEY `idx_target_no` (`target_no`),
    KEY `idx_action` (`action`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_log_time` (`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

CREATE TABLE `status_history` (
    `id`            BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `biz_type`      VARCHAR(30)  NOT NULL               COMMENT '业务类型',
    `biz_id`        BIGINT       NOT NULL               COMMENT '业务ID',
    `biz_no`        VARCHAR(64)           DEFAULT NULL  COMMENT '业务编号',
    `from_status`   VARCHAR(30)  NOT NULL               COMMENT '原状态',
    `to_status`     VARCHAR(30)  NOT NULL               COMMENT '目标状态',
    `change_reason` VARCHAR(512)          DEFAULT NULL  COMMENT '变更原因',
    `reason_code`   VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `operator_id`   BIGINT                DEFAULT NULL  COMMENT '操作人ID',
    `change_time`   DATETIME     NOT NULL               COMMENT '变更时间(UTC)',
    `deleted_flag`  TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`    DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`    DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_biz_type_id` (`biz_type`, `biz_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_change_time` (`change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='状态变更历史表';

CREATE TABLE `sys_attachment` (
    `id`           BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `biz_type`     VARCHAR(30)  NOT NULL               COMMENT '业务类型',
    `biz_id`       BIGINT       NOT NULL               COMMENT '业务ID',
    `biz_no`       VARCHAR(64)           DEFAULT NULL  COMMENT '业务编号',
    `file_name`    VARCHAR(256) NOT NULL               COMMENT '文件名',
    `file_path`    VARCHAR(512) NOT NULL               COMMENT '存储路径',
    `file_size`    BIGINT       NOT NULL               COMMENT '文件大小（字节）',
    `file_type`    VARCHAR(30)  NOT NULL               COMMENT '文件类型',
    `file_hash`    VARCHAR(128)          DEFAULT NULL  COMMENT '文件哈希',
    `file_url`     VARCHAR(512)          DEFAULT NULL  COMMENT '访问地址',
    `upload_time`  DATETIME     NOT NULL               COMMENT '上传时间(UTC)',
    `uploader_id`  BIGINT                DEFAULT NULL  COMMENT '上传人ID',
    `deleted_flag` TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_biz_type_id` (`biz_type`, `biz_id`),
    KEY `idx_file_hash` (`file_hash`),
    KEY `idx_uploader_id` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统附件表';


CREATE TABLE `message_notice` (
    `id`           BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `notice_no`    VARCHAR(64)  NOT NULL               COMMENT '通知编号',
    `account_id`   BIGINT       NOT NULL               COMMENT '接收账户ID',
    `title`        VARCHAR(256) NOT NULL               COMMENT '通知标题',
    `content`      TEXT                  DEFAULT NULL  COMMENT '通知内容',
    `notice_type`  VARCHAR(30)  NOT NULL               COMMENT '通知类型',
    `biz_type`     VARCHAR(30)           DEFAULT NULL  COMMENT '业务类型',
    `biz_no`       VARCHAR(64)           DEFAULT NULL  COMMENT '业务编号',
    `read_flag`    TINYINT      NOT NULL DEFAULT 0     COMMENT '已读标记 0-未读 1-已读',
    `read_time`    DATETIME              DEFAULT NULL  COMMENT '阅读时间(UTC)',
    `send_time`    DATETIME     NOT NULL               COMMENT '发送时间(UTC)',
    `deleted_flag` TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_no` (`notice_no`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_notice_type` (`notice_type`),
    KEY `idx_read_flag` (`read_flag`),
    KEY `idx_account_read` (`account_id`, `read_flag`),
    KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

CREATE TABLE `sys_config` (
    `id`           BIGINT       NOT NULL                   COMMENT '主键（雪花ID）',
    `config_key`   VARCHAR(128) NOT NULL                   COMMENT '配置键',
    `config_value` TEXT         NOT NULL                   COMMENT '配置值',
    `config_type`  VARCHAR(30)  NOT NULL                   COMMENT '配置类型',
    `description`  VARCHAR(256)          DEFAULT NULL      COMMENT '描述',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'  COMMENT '状态(ACTIVE/INACTIVE)',
    `deleted_flag` TINYINT      NOT NULL DEFAULT 0         COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME     NOT NULL                   COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME     NOT NULL                   COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_type` (`config_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE `chain_tx_record` (
    `id`                    BIGINT        NOT NULL               COMMENT '主键（雪花ID）',
    `biz_type`              VARCHAR(50)   NOT NULL               COMMENT '业务类型（BizTypeEnum）',
    `biz_id`                BIGINT        NOT NULL               COMMENT '业务ID',
    `biz_no`                VARCHAR(64)   NOT NULL               COMMENT '业务编号',
    `tx_type`               VARCHAR(20)   NOT NULL               COMMENT '交易类型(REGISTER/UPDATE/QUERY)',
    `channel_name`          VARCHAR(64)   NOT NULL               COMMENT '通道名称',
    `chaincode_name`        VARCHAR(64)   NOT NULL               COMMENT '链码名称',
    `tx_hash`               VARCHAR(128)           DEFAULT NULL  COMMENT '链上交易哈希（Fabric Transaction ID）',
    `block_height`          BIGINT                 DEFAULT NULL  COMMENT '区块高度',
    `chain_status`          VARCHAR(20)   NOT NULL               COMMENT '链上状态（ChainStatusEnum: PENDING/SUCCESS/FAILED）',
    `request_payload_hash`  VARCHAR(64)            DEFAULT NULL  COMMENT '请求负载 SHA-256 哈希',
    `response_payload`      TEXT                   DEFAULT NULL  COMMENT '链码响应负载（JSON）',
    `endorsement_summary`   VARCHAR(256)           DEFAULT NULL  COMMENT '背书节点摘要',
    `fail_reason`           VARCHAR(512)           DEFAULT NULL  COMMENT '失败原因',
    `reason_code`           VARCHAR(64)            DEFAULT NULL  COMMENT '失败原因码',
    `idempotent_key`        VARCHAR(128)           DEFAULT NULL  COMMENT '幂等键（bizType:bizId:txType）',
    `submit_time`           DATETIME               DEFAULT NULL  COMMENT '提交时间(UTC)',
    `confirm_time`          DATETIME               DEFAULT NULL  COMMENT '上链确认时间(UTC)',
    `deleted_flag`          TINYINT       NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`            DATETIME      NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`            DATETIME      NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
    KEY `idx_biz_type_id` (`biz_type`, `biz_id`),
    KEY `idx_biz_no` (`biz_no`),
    KEY `idx_chain_status` (`chain_status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='区块链交易记录表';

-- =============================================================
-- V1: Account & Identity Domain
-- =============================================================

CREATE TABLE IF NOT EXISTS `account` (
    `id`              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    `account_no`      VARCHAR(64)  NOT NULL                COMMENT '账户编号',
    `mobile`          VARCHAR(20)  NOT NULL                COMMENT '手机号',
    `password_hash`   VARCHAR(128) NOT NULL                COMMENT '密码哈希',
    `account_type`    VARCHAR(20)  NOT NULL                COMMENT '账户类型(PERSONAL/ENTERPRISE/PLATFORM/REGULATOR)',
    `status`          VARCHAR(30)  NOT NULL                COMMENT '账户状态(REGISTERED/AUTH_PENDING/AUTH_REJECTED/AUTH_APPROVED/ACCOUNT_FROZEN/ACCOUNT_DISABLED)',
    `auth_status`     VARCHAR(30)  NOT NULL DEFAULT ''     COMMENT '认证状态',
    `avatar_url`      VARCHAR(512)          DEFAULT NULL   COMMENT '头像地址',
    `nickname`        VARCHAR(64)           DEFAULT NULL   COMMENT '昵称',
    `email`           VARCHAR(128)          DEFAULT NULL   COMMENT '邮箱',
    `last_login_time` DATETIME              DEFAULT NULL   COMMENT '最后登录时间(UTC)',
    `last_login_ip`   VARCHAR(50)           DEFAULT NULL   COMMENT '最后登录IP',
    `previous_status` VARCHAR(30)           DEFAULT NULL   COMMENT '冻结前状态（解冻时恢复用）',
    `deleted_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '删除标记 0-未删除 1-已删除',
    `version`         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    `created_at`      DATETIME     NOT NULL                COMMENT '创建时间(UTC)',
    `updated_at`      DATETIME     NOT NULL                COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_no` (`account_no`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    KEY `idx_account_type` (`account_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

CREATE TABLE IF NOT EXISTS `subject_profile` (
    `id`                BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `subject_no`        VARCHAR(64)  NOT NULL               COMMENT '主体编号',
    `account_id`        BIGINT       NOT NULL               COMMENT '关联账户ID',
    `subject_type`      VARCHAR(20)  NOT NULL               COMMENT '主体类型(PERSONAL/ENTERPRISE)',
    `real_name`         VARCHAR(64)  NOT NULL               COMMENT '真实姓名/企业名称',
    `id_card_type`      VARCHAR(20)           DEFAULT NULL  COMMENT '证件类型',
    `id_card_no`        VARCHAR(64)           DEFAULT NULL  COMMENT '证件号码',
    `enterprise_code`   VARCHAR(64)           DEFAULT NULL  COMMENT '企业统一社会信用代码',
    `contact_name`      VARCHAR(64)           DEFAULT NULL  COMMENT '企业联系人',
    `contact_phone`     VARCHAR(20)           DEFAULT NULL  COMMENT '联系电话',
    `auth_material_url` VARCHAR(512)          DEFAULT NULL  COMMENT '认证材料文件地址',
    `deleted_flag`      TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`        DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`        DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subject_no` (`subject_no`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_subject_type` (`subject_type`),
    KEY `idx_id_card_no` (`id_card_no`),
    KEY `idx_enterprise_code` (`enterprise_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主体信息表';

CREATE TABLE IF NOT EXISTS `subject_auth_record` (
    `id`             BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `subject_id`     BIGINT       NOT NULL               COMMENT '关联主体ID',
    `auth_action`    VARCHAR(30)  NOT NULL               COMMENT '认证动作',
    `auth_status`    VARCHAR(30)  NOT NULL               COMMENT '认证状态',
    `reviewer_id`    BIGINT                DEFAULT NULL  COMMENT '审核人ID',
    `review_comment` VARCHAR(512)          DEFAULT NULL  COMMENT '审核意见',
    `reason_code`    VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `submit_time`    DATETIME     NOT NULL               COMMENT '提交时间(UTC)',
    `review_time`    DATETIME              DEFAULT NULL  COMMENT '审核时间(UTC)',
    `deleted_flag`   TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`     DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`     DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_subject_id` (`subject_id`),
    KEY `idx_auth_status` (`auth_status`),
    KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主体认证记录表';

CREATE TABLE IF NOT EXISTS `account_role` (
    `id`           BIGINT      NOT NULL               COMMENT '主键（雪花ID）',
    `account_id`   BIGINT      NOT NULL               COMMENT '账户ID',
    `role_code`    VARCHAR(30) NOT NULL               COMMENT '角色编码(CREATOR/BUYER/PLATFORM_ADMIN/REGULATOR)',
    `status`       VARCHAR(20) NOT NULL               COMMENT '状态(ACTIVE/INACTIVE)',
    `granted_by`   BIGINT               DEFAULT NULL  COMMENT '授予人ID',
    `granted_time` DATETIME              DEFAULT NULL  COMMENT '授予时间(UTC)',
    `deleted_flag` TINYINT     NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME    NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME    NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_role_code` (`role_code`),
    KEY `idx_account_role` (`account_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户角色表';

CREATE TABLE IF NOT EXISTS `did_record` (
    `id`             BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `did_no`         VARCHAR(64)  NOT NULL               COMMENT 'DID编号',
    `did_value`      VARCHAR(256) NOT NULL               COMMENT 'DID标识值',
    `account_id`     BIGINT       NOT NULL               COMMENT '账户ID',
    `subject_id`     BIGINT       NOT NULL               COMMENT '主体ID',
    `status`         VARCHAR(30)  NOT NULL               COMMENT 'DID状态',
    `previous_status` VARCHAR(30)          DEFAULT NULL  COMMENT '发起挂起/吊销前的原状态快照',
    `chain_status`   VARCHAR(30)  NOT NULL DEFAULT ''    COMMENT '链上状态',
    `apply_time`     DATETIME              DEFAULT NULL  COMMENT '申请时间(UTC)',
    `approve_time`   DATETIME              DEFAULT NULL  COMMENT '审批时间(UTC)',
    `active_time`    DATETIME              DEFAULT NULL  COMMENT '激活时间(UTC)',
    `suspend_time`   DATETIME              DEFAULT NULL  COMMENT '挂起时间(UTC)',
    `revoke_time`    DATETIME              DEFAULT NULL  COMMENT '吊销时间(UTC)',
    `tx_hash`        VARCHAR(128)          DEFAULT NULL  COMMENT '交易哈希',
    `block_height`   BIGINT                DEFAULT NULL  COMMENT '区块高度',
    `fail_reason`    VARCHAR(512)          DEFAULT NULL  COMMENT '失败原因',
    `reviewer_id`    BIGINT                DEFAULT NULL  COMMENT '审核人ID',
    `review_comment` VARCHAR(512)          DEFAULT NULL  COMMENT '审核意见',
    `reason_code`    VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `deleted_flag`   TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `version`        INT          NOT NULL DEFAULT 0     COMMENT '乐观锁版本号',
    `created_at`     DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`     DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_did_no` (`did_no`),
    UNIQUE KEY `uk_did_value` (`did_value`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_subject_id` (`subject_id`),
    KEY `idx_status` (`status`),
    KEY `idx_chain_status` (`chain_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DID记录表';

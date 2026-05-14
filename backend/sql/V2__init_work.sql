-- =============================================================
-- V2: Work & Claim Domain
-- =============================================================

CREATE TABLE IF NOT EXISTS `work` (
    `id`                 BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `work_no`            VARCHAR(64)  NOT NULL               COMMENT '作品编号',
    `creator_account_id` BIGINT       NOT NULL               COMMENT '创作者账户ID',
    `creator_subject_id` BIGINT       NOT NULL               COMMENT '创作者主体ID',
    `creator_did_id`     BIGINT       NOT NULL               COMMENT '创作者DID ID',
    `title`              VARCHAR(256) NOT NULL               COMMENT '作品标题',
    `description`        TEXT                  DEFAULT NULL  COMMENT '作品描述',
    `work_type`          VARCHAR(30)  NOT NULL               COMMENT '作品类型(TEXT/IMAGE/AUDIO/VIDEO/MODEL/OTHER)',
    `status`             VARCHAR(30)  NOT NULL               COMMENT '作品状态',
    `file_hash`          VARCHAR(128)          DEFAULT NULL  COMMENT '文件哈希',
    `meta_hash`          VARCHAR(128)          DEFAULT NULL  COMMENT '元数据哈希',
    `cover_url`          VARCHAR(512)          DEFAULT NULL  COMMENT '封面地址',
    `submit_time`        DATETIME              DEFAULT NULL  COMMENT '提交时间(UTC)',
    `deleted_flag`       TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `version`            INT          NOT NULL DEFAULT 0     COMMENT '乐观锁版本号',
    `created_at`         DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`         DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_no` (`work_no`),
    KEY `idx_creator_account_id` (`creator_account_id`),
    KEY `idx_creator_subject_id` (`creator_subject_id`),
    KEY `idx_work_type` (`work_type`),
    KEY `idx_status` (`status`),
    KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品表';

CREATE TABLE IF NOT EXISTS `work_file` (
    `id`           BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `work_id`      BIGINT       NOT NULL               COMMENT '作品ID',
    `file_name`    VARCHAR(256) NOT NULL               COMMENT '文件名',
    `file_path`    VARCHAR(512) NOT NULL               COMMENT '存储路径',
    `file_size`    BIGINT       NOT NULL               COMMENT '文件大小（字节）',
    `file_type`    VARCHAR(30)  NOT NULL               COMMENT '文件类型',
    `file_hash`    VARCHAR(128)          DEFAULT NULL  COMMENT '文件哈希',
    `file_url`     VARCHAR(512)          DEFAULT NULL  COMMENT '访问地址',
    `purpose`      VARCHAR(30)  NOT NULL               COMMENT '用途(ORIGINAL/THUMBNAIL/PREVIEW)',
    `deleted_flag` TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`   DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`   DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_file_hash` (`file_hash`),
    KEY `idx_purpose` (`purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品文件表';

CREATE TABLE IF NOT EXISTS `work_aigc_meta` (
    `id`                BIGINT        NOT NULL               COMMENT '主键（雪花ID）',
    `work_id`           BIGINT        NOT NULL               COMMENT '作品ID',
    `aigc_tool`         VARCHAR(128)           DEFAULT NULL  COMMENT 'AIGC工具名称',
    `aigc_model`        VARCHAR(128)           DEFAULT NULL  COMMENT 'AIGC模型名称',
    `aigc_version`      VARCHAR(64)            DEFAULT NULL  COMMENT 'AIGC版本',
    `prompt_summary`    VARCHAR(1024)          DEFAULT NULL  COMMENT '提示词摘要',
    `generation_params` TEXT                   DEFAULT NULL  COMMENT '生成参数(JSON)',
    `generation_time`   DATETIME               DEFAULT NULL  COMMENT '生成时间(UTC)',
    `deleted_flag`      TINYINT       NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`        DATETIME      NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`        DATETIME      NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_work_id` (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品AIGC元数据表';

CREATE TABLE IF NOT EXISTS `work_feature` (
    `id`              BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `work_id`         BIGINT       NOT NULL               COMMENT '作品ID',
    `feature_type`    VARCHAR(30)  NOT NULL               COMMENT '特征类型(PERCEPTUAL_HASH/FINGERPRINT/VECTOR)',
    `feature_value`   TEXT                  DEFAULT NULL  COMMENT '特征值',
    `perceptual_hash` VARCHAR(128)          DEFAULT NULL  COMMENT '感知哈希',
    `extract_status`  VARCHAR(20)  NOT NULL               COMMENT '提取状态(PENDING/SUCCESS/FAILED)',
    `extract_time`    DATETIME              DEFAULT NULL  COMMENT '提取时间(UTC)',
    `fail_reason`     VARCHAR(512)          DEFAULT NULL  COMMENT '失败原因',
    `deleted_flag`    TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`      DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`      DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_feature_type` (`feature_type`),
    KEY `idx_perceptual_hash` (`perceptual_hash`),
    KEY `idx_extract_status` (`extract_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品特征表';

CREATE TABLE IF NOT EXISTS `work_similarity_check` (
    `id`               BIGINT        NOT NULL               COMMENT '主键（雪花ID）',
    `work_id`          BIGINT        NOT NULL               COMMENT '待检作品ID',
    `compared_work_id` BIGINT        NOT NULL               COMMENT '对比作品ID',
    `similarity_score` DECIMAL(5,4)  NOT NULL               COMMENT '相似度分数',
    `check_result`     VARCHAR(20)   NOT NULL               COMMENT '检测结果(PASS/HIGH_RISK/MANUAL_REVIEW)',
    `check_time`       DATETIME      NOT NULL               COMMENT '检测时间(UTC)',
    `deleted_flag`     TINYINT       NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`       DATETIME      NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`       DATETIME      NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_compared_work_id` (`compared_work_id`),
    KEY `idx_check_result` (`check_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品相似度检测表';

CREATE TABLE IF NOT EXISTS `claim_application` (
    `id`                   BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `claim_no`             VARCHAR(64)  NOT NULL               COMMENT '确权编号',
    `work_id`              BIGINT       NOT NULL               COMMENT '作品ID',
    `work_no`              VARCHAR(64)  NOT NULL               COMMENT '作品编号',
    `applicant_account_id` BIGINT       NOT NULL               COMMENT '申请人账户ID',
    `applicant_did_id`     BIGINT       NOT NULL               COMMENT '申请人DID',
    `status`               VARCHAR(30)  NOT NULL               COMMENT '确权申请状态',
    `chain_status`         VARCHAR(30)  NOT NULL DEFAULT ''    COMMENT '链上状态',
    `submit_time`          DATETIME              DEFAULT NULL  COMMENT '提交时间(UTC)',
    `review_time`          DATETIME              DEFAULT NULL  COMMENT '审核时间(UTC)',
    `approve_time`         DATETIME              DEFAULT NULL  COMMENT '审批通过时间(UTC)',
    `chain_submit_time`    DATETIME              DEFAULT NULL  COMMENT '链上提交时间(UTC)',
    `chain_confirm_time`   DATETIME              DEFAULT NULL  COMMENT '链上确认时间(UTC)',
    `tx_hash`              VARCHAR(128)          DEFAULT NULL  COMMENT '交易哈希',
    `block_height`         BIGINT                DEFAULT NULL  COMMENT '区块高度',
    `summary_hash`         VARCHAR(128)          DEFAULT NULL  COMMENT '确权摘要哈希',
    `reviewer_id`          BIGINT                DEFAULT NULL  COMMENT '审核人ID',
    `review_comment`       VARCHAR(512)          DEFAULT NULL  COMMENT '审核意见',
    `reject_reason`        VARCHAR(512)          DEFAULT NULL  COMMENT '拒绝原因',
    `reason_code`          VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `fail_reason`          VARCHAR(512)          DEFAULT NULL  COMMENT '失败原因',
    `deleted_flag`         TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `version`              INT          NOT NULL DEFAULT 0     COMMENT '乐观锁版本号',
    `created_at`           DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`           DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_claim_no` (`claim_no`),
    KEY `idx_work_id` (`work_id`),
    KEY `idx_work_no` (`work_no`),
    KEY `idx_applicant_account_id` (`applicant_account_id`),
    KEY `idx_status` (`status`),
    KEY `idx_chain_status` (`chain_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='确权申请表';

CREATE TABLE IF NOT EXISTS `claim_review_record` (
    `id`             BIGINT       NOT NULL               COMMENT '主键（雪花ID）',
    `claim_id`       BIGINT       NOT NULL               COMMENT '确权申请ID',
    `reviewer_id`    BIGINT       NOT NULL               COMMENT '审核人ID',
    `review_action`  VARCHAR(30)  NOT NULL               COMMENT '审核动作',
    `review_result`  VARCHAR(20)  NOT NULL               COMMENT '审核结果',
    `review_comment` VARCHAR(512)          DEFAULT NULL  COMMENT '审核意见',
    `reason_code`    VARCHAR(64)           DEFAULT NULL  COMMENT '原因码',
    `review_time`    DATETIME     NOT NULL               COMMENT '审核时间(UTC)',
    `deleted_flag`   TINYINT      NOT NULL DEFAULT 0     COMMENT '删除标记 0-未删除 1-已删除',
    `created_at`     DATETIME     NOT NULL               COMMENT '创建时间(UTC)',
    `updated_at`     DATETIME     NOT NULL               COMMENT '更新时间(UTC)',
    PRIMARY KEY (`id`),
    KEY `idx_claim_id` (`claim_id`),
    KEY `idx_reviewer_id` (`reviewer_id`),
    KEY `idx_review_result` (`review_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='确权审核记录表';

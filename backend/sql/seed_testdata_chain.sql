-- =============================================================
-- 测试初始化数据（账户 + DID + 确权 + 交易 + 监管 + 结算）
-- 目标：提供可直接触发上链流程的前置业务数据。
-- 说明：
--   1) 不预置任何链上核验工件：
--      - 不写入 `chain_tx_record`
--      - 各业务表 `tx_hash`/`block_height` 默认保持 NULL
--   2) 账号密码：
--      - 本脚本所有账号明文密码均为 "123456"
--      - 下方 `password_hash` 为 "123456" 的 BCrypt 值
-- =============================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- -----------------------------
-- 清理（支持重复执行）
-- -----------------------------
DELETE FROM `settlement_item` WHERE `settle_no` IN ('SETTLE-9000001', 'SETTLE-9000002');
DELETE FROM `settlement_record` WHERE `settle_no` IN ('SETTLE-9000001', 'SETTLE-9000002');

DELETE FROM `claim_application` WHERE `claim_no` IN ('CLM-8000001', 'CLM-8000002');
DELETE FROM `work` WHERE `work_no` IN ('WK-7000001', 'WK-7000002');

DELETE FROM `did_record` WHERE `did_no` IN ('DID-6000001', 'DID-6000002', 'DID-6000003');

-- 扩展全链路测试数据清理
DELETE FROM `work_settle_rule` WHERE `work_no` IN ('WK-7000001');

DELETE FROM `license_record` WHERE `license_no` IN ('LIC-9100001', 'LIC-9100002');
DELETE FROM `payment_record` WHERE `order_no` IN ('ORDER-9100001', 'ORDER-9100002', 'ORDER-9100003');
DELETE FROM `trade_order` WHERE `order_no` IN ('ORDER-9100001', 'ORDER-9100002', 'ORDER-9100003');
DELETE FROM `work_listing` WHERE `listing_no` IN ('LST-6200001');
DELETE FROM `license_template` WHERE `template_code` IN ('LTPL-10001');

DELETE FROM `risk_event` WHERE `risk_no` IN ('RISK-9700001');
DELETE FROM `freeze_record` WHERE `freeze_no` IN ('FRZ-9600001', 'FRZ-9600002');
DELETE FROM `dispute_evidence` WHERE `case_no` IN ('DSP-9800001', 'DSP-9800002', 'DSP-9800003', 'DSP-9800004', 'DSP-9800005');
DELETE FROM `dispute_process_record` WHERE `case_no` IN ('DSP-9800001', 'DSP-9800002', 'DSP-9800003', 'DSP-9800004', 'DSP-9800005');
DELETE FROM `dispute_case` WHERE `case_no` IN ('DSP-9800001', 'DSP-9800002', 'DSP-9800003', 'DSP-9800004', 'DSP-9800005');
DELETE FROM `regulator_report` WHERE `report_no` IN ('RPT-9900001');

DELETE FROM `account_role` WHERE `account_id` IN (1000001, 2000001, 2000002, 3000001, 4000001);
DELETE FROM `subject_profile` WHERE `subject_no` IN ('SUBJ-10001', 'SUBJ-20001', 'SUBJ-20002', 'SUBJ-30001', 'SUBJ-40001');
DELETE FROM `account` WHERE `account_no` IN ('PLAT-10001', 'CRE-20001', 'CRE-20002', 'BUY-30001', 'REG-40001');

-- -----------------------------
-- 1) 账户
-- -----------------------------
-- "123456" 对应的 BCrypt：
-- $2a$10$yNCZsnfBVARb9MZNhf4.uuFXqx3I3dFPA1o1wj0cnCmK5yMZwwX7W

INSERT INTO `account`
(
  `id`, `account_no`, `mobile`, `password_hash`, `account_type`, `status`, `auth_status`,
  `avatar_url`, `nickname`, `email`,
  `last_login_time`, `last_login_ip`, `previous_status`,
  `deleted_flag`, `version`, `created_at`, `updated_at`
)
VALUES
  (1000001, 'PLAT-10001', '13800000001', '$2a$10$yNCZsnfBVARb9MZNhf4.uuFXqx3I3dFPA1o1wj0cnCmK5yMZwwX7W', 'PLATFORM', 'AUTH_APPROVED', 'SUCCESS', NULL, '平台管理员', 'admin@lifechain.cn', NULL, NULL, NULL, 0, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (2000001, 'CRE-20001',  '13800000002', '$2a$10$yNCZsnfBVARb9MZNhf4.uuFXqx3I3dFPA1o1wj0cnCmK5yMZwwX7W', 'PERSONAL', 'AUTH_APPROVED', 'SUCCESS', NULL, '创作者甲', 'creator1@lifechain.cn', NULL, NULL, NULL, 0, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (2000002, 'CRE-20002',  '13800000003', '$2a$10$yNCZsnfBVARb9MZNhf4.uuFXqx3I3dFPA1o1wj0cnCmK5yMZwwX7W', 'PERSONAL', 'AUTH_APPROVED', 'SUCCESS', NULL, 'CreatorB', 'creator2@lifechain.cn', NULL, NULL, NULL, 0, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (3000001, 'BUY-30001',  '13800000004', '$2a$10$yNCZsnfBVARb9MZNhf4.uuFXqx3I3dFPA1o1wj0cnCmK5yMZwwX7W', 'PERSONAL', 'AUTH_APPROVED', 'SUCCESS', NULL, '购买方甲', 'buyer1@lifechain.cn', NULL, NULL, NULL, 0, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (4000001, 'REG-40001',  '13800000005', '$2a$10$yNCZsnfBVARb9MZNhf4.uuFXqx3I3dFPA1o1wj0cnCmK5yMZwwX7W', 'REGULATOR', 'AUTH_APPROVED', 'SUCCESS', NULL, '监管员甲', 'regulator1@lifechain.cn', NULL, NULL, NULL, 0, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00');

INSERT INTO `account_role`
(
  `id`, `account_id`, `role_code`, `status`,
  `granted_by`, `granted_time`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (11000001, 1000001, 'PLATFORM_ADMIN', 'ACTIVE', NULL, NULL, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (11000002, 2000001, 'CREATOR',        'ACTIVE', NULL, NULL, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (11000003, 2000002, 'CREATOR',        'ACTIVE', NULL, NULL, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (11000004, 3000001, 'BUYER',          'ACTIVE', NULL, NULL, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),
  (11000005, 4000001, 'REGULATOR',      'ACTIVE', NULL, NULL, 0, '2026-03-21 10:00:00', '2026-03-21 10:00:00');

-- -----------------------------
-- 2) 主体档案
-- -----------------------------
INSERT INTO `subject_profile`
(
  `id`, `subject_no`, `account_id`, `subject_type`,
  `real_name`,
  `id_card_type`, `id_card_no`,
  `enterprise_code`, `contact_name`, `contact_phone`,
  `auth_material_url`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (5000001, 'SUBJ-10001', 1000001, 'ENTERPRISE', 'LifeChain平台运营中心',
   'ID_CARD', '110101199003074321', NULL, '平台管理员', '13800000001',
   'https://assets.lifechain.cn/materials/admin/rea.pdf',
   0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),

  (5000002, 'SUBJ-20001', 2000001, 'PERSONAL', '张伟',
   'ID_CARD', '110105199001017654', NULL, '张伟', '13800000002',
   'https://assets.lifechain.cn/materials/creator1/rea.pdf',
   0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),

  (5000003, 'SUBJ-20002', 2000002, 'PERSONAL', '王芳',
   'ID_CARD', '110105198912120987', NULL, '王芳', '13800000003',
   'https://assets.lifechain.cn/materials/creator2/rea.pdf',
   0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),

  (5000004, 'SUBJ-30001', 3000001, 'PERSONAL', '赵敏',
   'ID_CARD', '110105199512180123', NULL, '赵敏', '13800000004',
   'https://assets.lifechain.cn/materials/buyer1/rea.pdf',
   0, '2026-03-21 10:00:00', '2026-03-21 10:00:00'),

  (5000005, 'SUBJ-40001', 4000001, 'ENTERPRISE', '区域数字内容监管机构',
   NULL, NULL,
   '91350100MA35TESTCODE1', '监管员甲', '13800000005',
   'https://assets.lifechain.cn/materials/regulator1/report.pdf',
   0, '2026-03-21 10:00:00', '2026-03-21 10:00:00');

-- -----------------------------
-- 3) DID 记录
--    - 创作者A：DID_ACTIVE（用于授权上链流程）
--    - 创作者B：DID_PENDING（用于DID审核上链流程）
--    - 买家A：DID_ACTIVE（用于授权上链流程）
-- -----------------------------
INSERT INTO `did_record`
(
  `id`, `did_no`, `did_value`,
  `account_id`, `subject_id`,
  `status`, `chain_status`,
  `apply_time`, `approve_time`, `active_time`, `suspend_time`, `revoke_time`,
  `tx_hash`, `block_height`, `fail_reason`,
  `reviewer_id`, `review_comment`, `reason_code`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (6000001, 'DID-6000001', 'did:lifechain:CRE-20001',
   2000001, 5000002,
   'DID_ACTIVE', '',
   '2026-03-18 10:00:00', '2026-03-18 12:00:00', '2026-03-18 12:10:00', NULL, NULL,
   NULL, NULL, NULL,
   1000001, '初始化导入', 'RC-DID-SEED-ACTIVE',
   0, 0,
   '2026-03-18 10:00:00', '2026-03-18 12:10:00'),

  (6000002, 'DID-6000002', 'did:lifechain:CRE-20002',
   2000002, 5000003,
   'DID_PENDING', '',
   '2026-03-18 11:00:00', NULL, NULL, NULL, NULL,
   NULL, NULL, NULL,
   NULL, NULL, NULL,
   0, 0,
   '2026-03-18 11:00:00', '2026-03-18 11:00:00'),

  (6000003, 'DID-6000003', 'did:lifechain:BUY-30001',
   3000001, 5000004,
   'DID_ACTIVE', '',
   '2026-03-18 09:30:00', '2026-03-18 11:00:00', '2026-03-18 11:10:00', NULL, NULL,
   NULL, NULL, NULL,
   1000001, '初始化导入', 'RC-DID-SEED-ACTIVE',
   0, 0,
   '2026-03-18 09:30:00', '2026-03-18 11:10:00');

-- -----------------------------
-- 4) 作品 + 确权
--    - 作品状态：READY_FOR_CLAIM
--    - 确权状态：CLAIM_SUBMITTED（管理端审核后触发上链）
-- -----------------------------
INSERT INTO `work`
(
  `id`, `work_no`,
  `creator_account_id`, `creator_subject_id`, `creator_did_id`,
  `title`, `description`, `work_type`,
  `status`,
  `file_hash`, `meta_hash`,
  `cover_url`,
  `submit_time`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (7000001, 'WK-7000001',
   2000001, 5000002, 6000001,
   '可信AIGC示例作品A',
   '用于版权确权的AIGC政策分析稿，包含完整作者轨迹元数据',
   'TEXT',
   'READY_FOR_CLAIM',
   '2169a9e1435dd67b320ca081f75e502644cd4b7655253e69c9105a8b276881fa',
   '4866eb5a5fd39d1aa3c7daf926b6937e8fa9bfd2509fdedc340b319fb918a7eb',
   'https://assets.lifechain.cn/covers/WK-7000001.png',
   '2026-03-19 09:00:00',
   0, 0,
   '2026-03-19 09:00:00', '2026-03-19 09:00:00');

INSERT INTO `claim_application`
(
  `id`, `claim_no`,
  `work_id`, `work_no`,
  `applicant_account_id`, `applicant_did_id`,
  `status`, `chain_status`,
  `submit_time`,
  `review_time`, `approve_time`,
  `chain_submit_time`, `chain_confirm_time`,
  `tx_hash`, `block_height`,
  `summary_hash`,
  `reviewer_id`, `review_comment`,
  `reject_reason`, `reason_code`, `fail_reason`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (8000001, 'CLM-8000001',
   7000001, 'WK-7000001',
   2000001, 6000001,
   'CLAIM_SUBMITTED', '',
   '2026-03-19 12:00:00',
   NULL, NULL,
   NULL, NULL,
   NULL, NULL,
   NULL,
   NULL, NULL,
   NULL, NULL, NULL,
   0, 0,
   '2026-03-19 12:00:00', '2026-03-19 12:00:00');

-- -----------------------------
-- 5) 交易域（上架/订单/支付）+ 授权
-- -----------------------------
-- 用于授权发放与争议结案后的授权撤销测试
INSERT INTO `license_template`
(
  `id`, `template_name`, `template_code`,
  `license_type`, `scope_description`, `duration_days`,
  `price_amount`, `currency`, `status`, `description`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (6100001, 'LifeChain个人使用授权模板',
   'LTPL-10001',
   'PERSONAL_USE',
   '授权期内允许个人评估与非商业传播',
   365,
   100000, 'CNY', 'ACTIVE',
   '用于端到端授权发放链路测试的初始化模板',
   0, '2026-03-20 09:00:00', '2026-03-20 09:00:00');

INSERT INTO `work_listing`
(
  `id`, `listing_no`,
  `work_id`, `work_no`, `creator_account_id`,
  `license_template_id`,
  `license_type`,
  `price_amount`, `currency`,
  `status`, `review_status`, `reviewer_id`, `review_comment`,
  `list_time`, `unlist_time`,
  `scope_description`, `duration_days`,
  `version`, `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  (6200001, 'LST-6200001',
   7000001, 'WK-7000001', 2000001,
   6100001,
   'PERSONAL_USE',
   100000, 'CNY',
   'LISTED', 'APPROVED', 1000001, '初始化审核通过',
   '2026-03-20 09:10:00', NULL,
   '授权期内个人使用；允许以评审副本形式再分发',
   365,
   0, 0,
   '2026-03-20 09:10:00', '2026-03-20 09:10:00');

INSERT INTO `work_settle_rule`
(
  `id`, `work_id`, `work_no`,
  `template_id`,
  `creator_account_id`,
  `creator_ratio`, `platform_ratio`,
  `effective_time`,
  `status`,
  `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  (9500001, 7000001, 'WK-7000001',
   NULL,
   2000001,
   0.8000, 0.2000,
   '2026-03-20 00:00:00',
   'ACTIVE',
   0,
   '2026-03-20 00:00:00', '2026-03-20 00:00:00');

INSERT INTO `trade_order`
(
  `id`, `order_no`,
  `work_id`, `work_no`,
  `listing_id`, `listing_no`,
  `buyer_account_id`, `buyer_subject_id`,
  `creator_account_id`, `creator_subject_id`,
  `order_status`, `license_type`,
  `price_amount`, `pay_amount`,
  `currency`, `pay_channel`, `pay_status`,
  `expire_time`, `pay_time`, `complete_time`, `cancel_time`, `cancel_reason`,
  `request_id`, `version`, `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  (9100001, 'ORDER-9100001',
   7000001, 'WK-7000001',
   6200001, 'LST-6200001',
   3000001, 5000004,
   2000001, 5000002,
   'ORDER_CREATED', 'PERSONAL_USE',
   100000, 100000,
   'CNY', 'WECHAT_PAY', 'PAY_PENDING',
   '2026-03-22 00:00:00', NULL, NULL, NULL, NULL,
   'REQ-ORDER-9100001', 0, 0,
   '2026-03-20 10:00:00', '2026-03-20 10:00:00'),

  (9100002, 'ORDER-9100002',
   7000001, 'WK-7000001',
   6200001, 'LST-6200001',
   3000001, 5000004,
   2000001, 5000002,
   'ORDER_CREATED', 'PERSONAL_USE',
   100000, 100000,
   'CNY', 'WECHAT_PAY', 'PAY_PENDING',
   '2026-03-22 00:00:00', NULL, NULL, NULL, NULL,
   'REQ-ORDER-9100002', 0, 0,
   '2026-03-20 11:00:00', '2026-03-20 11:00:00');

INSERT INTO `payment_record`
(
  `id`, `payment_no`,
  `order_id`, `order_no`,
  `pay_channel`, `pay_status`,
  `pay_amount`, `currency`,
  `third_trade_no`, `prepay_id`, `callback_raw_ref`,
  `pay_time`, `callback_time`, `expire_time`,
  `fail_reason`, `request_id`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (9300001, 'PAY-9300001',
   9100001, 'ORDER-9100001',
   'WECHAT_PAY', 'PAY_PENDING',
   100000, 'CNY',
   'wx_third_trade_9100001', 'wx_prepay_9100001', NULL,
   NULL, NULL, '2026-03-22 00:00:00',
   NULL, 'REQ-PAY-9100001',
   0, '2026-03-20 10:01:00', '2026-03-20 10:01:00'),

  (9300002, 'PAY-9300002',
   9100002, 'ORDER-9100002',
   'WECHAT_PAY', 'PAY_PENDING',
   100000, 'CNY',
   'wx_third_trade_9100002', 'wx_prepay_9100002', NULL,
   NULL, NULL, '2026-03-22 00:00:00',
   NULL, 'REQ-PAY-9100002',
   0, '2026-03-20 11:01:00', '2026-03-20 11:01:00');

INSERT INTO `license_record`
(
  `id`, `license_no`,
  `order_id`, `order_no`,
  `work_id`, `work_no`,
  `licensor_account_id`, `licensee_account_id`,
  `license_type`,
  `license_status`, `chain_status`,
  `scope_description`,
  `effective_time`, `expire_time`,
  `license_hash`,
  `tx_hash`, `block_height`,
  `terminate_time`, `terminate_reason`,
  `request_id`,
  `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  (9400001, 'LIC-9100001',
   9100001, 'ORDER-9100001',
   7000001, 'WK-7000001',
   2000001, 3000001,
   'PERSONAL_USE',
   'LICENSE_ACTIVE', '',
   '365天内用于个人评估与内部评审副本使用',
   '2026-03-20 10:30:00', '2027-03-20 10:30:00',
   '3d7a4c2f6a1b8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d',
   NULL, NULL,
   NULL, NULL,
   'REQ-LIC-9100001',
   0,
   '2026-03-20 10:30:00', '2026-03-20 10:30:00'),

  (9400002, 'LIC-9100002',
   9100002, 'ORDER-9100002',
   7000001, 'WK-7000001',
   2000001, 3000001,
   'PERSONAL_USE',
   'LICENSE_ACTIVE', '',
   '365天内用于个人评估与内部评审副本使用',
   '2026-03-20 11:30:00', '2027-03-20 11:30:00',
   '7f6e5d4c3b2a19080706050403020100ffeeddccbbaa99887766554433221100',
   NULL, NULL,
   NULL, NULL,
   'REQ-LIC-9100002',
   0,
   '2026-03-20 11:30:00', '2026-03-20 11:30:00');

-- -----------------------------
-- 6) 监管域（风险 / 冻结 / 争议 / 报告）
-- -----------------------------
INSERT INTO `risk_event`
(
  `id`, `risk_no`, `target_type`, `target_id`, `target_no`,
  `status`, `risk_level`, `risk_type`, `risk_description`,
  `reporter_id`, `reporter_role`, `report_time`,
  `reason_code`, `result_summary`, `resolve_time`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (9700001, 'RISK-9700001', 'WORK', 7000001, 'WK-7000001',
   'RISK_MARKED', 'MEDIUM', 'COPYRIGHT_MONITOR',
   '自动化风控检测发现该作品存在潜在的未授权复用风险',
   4000001, 'REGULATOR',
   '2026-03-20 09:20:00',
   'RC-RISK-MED', NULL, NULL,
   0, '2026-03-20 09:20:00', '2026-03-20 09:20:00');

INSERT INTO `freeze_record`
(
  `id`, `freeze_no`,
  `target_type`, `target_id`, `target_no`,
  `freeze_status`, `freeze_mode`,
  `review_status`, `freeze_reason`, `reason_code`,
  `apply_user_id`, `apply_role`, `apply_time`,
  `approve_user_id`, `approve_time`, `effective_time`,
  `unfreeze_reason`, `unfreeze_time`,
  `urgent_basis_no`,
  `chain_status`, `tx_hash`, `block_height`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (9600001, 'FRZ-9600001',
   'ORDER', 9100002, 'ORDER-9100002',
   'FREEZE_APPROVED', 'REGULATOR_DIRECT',
   'REVIEW_PASSED',
   '争议处理期间冻结该订单，防止继续结算',
   'RC-FREEZE-ORDER',
   4000001, 'REGULATOR', '2026-03-20 12:10:00',
   1000001, '2026-03-20 12:20:00', '2026-03-20 12:20:00',
   NULL, NULL,
   'RISK-9700001',
   '', NULL, NULL,
   0, 0,
   '2026-03-20 12:10:00', '2026-03-20 12:10:00'),
  (9600002, 'FRZ-9600002',
   'LICENSE', 9400001, 'LIC-9100001',
   'FREEZE_APPROVED', 'REGULATOR_DIRECT',
   'PENDING_POST_REVIEW',
   'Direct freeze pending post-review decision',
   'RC-FREEZE-LIC',
   4000001, 'REGULATOR', '2026-03-20 12:30:00',
   1000001, '2026-03-20 12:31:00', '2026-03-20 12:31:00',
   NULL, NULL,
   'RISK-9700001',
   'CHAIN_SUBMITTED', NULL, NULL,
   0, 0,
   '2026-03-20 12:30:00', '2026-03-20 12:30:00');

INSERT INTO `dispute_case`
(
  `id`, `case_no`,
  `order_id`, `order_no`,
  `work_id`, `work_no`,
  `applicant_account_id`, `respondent_account_id`,
  `dispute_type`,
  `status`,
  `description`,
  `submit_time`, `accept_time`, `close_time`,
  `result_summary`,
  `reason_code`,
  `chain_status`, `tx_hash`, `block_height`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (9800001, 'DSP-9800001',
   9100001, 'ORDER-9100001',
   7000001, 'WK-7000001',
   3000001, 2000001,
   'LICENSE_SCOPE_DISPUTE',
   'DISPUTE_SUBMITTED',
   '买方主张授权范围不足以覆盖内部传播场景',
   '2026-03-20 13:00:00', NULL, NULL,
   NULL,
   NULL,
   'CHAIN_PENDING', NULL, NULL,
   0, 0,
   '2026-03-20 13:00:00', '2026-03-20 13:00:00'),
  (9800002, 'DSP-9800002',
   9100001, 'ORDER-9100001',
   7000001, 'WK-7000001',
   3000001, 2000001,
   'LICENSE_SCOPE_DISPUTE',
   'DISPUTE_ACCEPTED',
   '案件已受理，等待补充证据',
   '2026-03-20 13:05:00', '2026-03-20 13:10:00', NULL,
   NULL,
   NULL,
   'CHAIN_PENDING', NULL, NULL,
   0, 0,
   '2026-03-20 13:05:00', '2026-03-20 13:10:00'),
  (9800003, 'DSP-9800003',
   9100001, 'ORDER-9100001',
   7000001, 'WK-7000001',
   3000001, 2000001,
   'LICENSE_SCOPE_DISPUTE',
   'DISPUTE_EVIDENCE_PENDING',
   '终审前要求补充证据',
   '2026-03-20 13:15:00', NULL, NULL,
   NULL,
   NULL,
   'CHAIN_PENDING', NULL, NULL,
   0, 0,
   '2026-03-20 13:15:00', '2026-03-20 13:15:00'),
  (9800004, 'DSP-9800004',
   9100002, 'ORDER-9100002',
   7000001, 'WK-7000001',
   3000001, 2000001,
   'LICENSE_SCOPE_DISPUTE',
   'DISPUTE_REVIEWING',
   '复核处理中，待给出最终结论',
   '2026-03-20 13:30:00', NULL, NULL,
   NULL,
   NULL,
   'CHAIN_PENDING', NULL, NULL,
   0, 0,
   '2026-03-20 13:30:00', '2026-03-20 13:30:00');

INSERT INTO `dispute_evidence`
(
  `id`, `case_id`, `case_no`,
  `submitter_account_id`,
  `evidence_type`,
  `evidence_description`,
  `file_url`, `file_hash`,
  `submit_time`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (9810001, 9800003, 'DSP-9800003',
   3000001,
   'CONTRACT',
   '证据摘要：授权期内允许内部传播的条款摘录',
   'https://cdn.lifechain.local/evidence/DSP-9800003/contract.pdf',
   '1f2e3d4c5b6a79887766554433221100ffeeddccbbaa99887766554433221100',
   '2026-03-20 13:20:00',
   0, '2026-03-20 13:20:00', '2026-03-20 13:20:00');

INSERT INTO `dispute_process_record`
(
  `id`, `case_id`, `case_no`,
  `operator_id`, `action`,
  `action_result`, `comment`, `reason_code`,
  `process_time`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (9820001, 9800001, 'DSP-9800001',
   3000001, 'CREATE',
   'SUCCESS', '案件已创建', NULL,
   '2026-03-20 13:00:00',
   0, '2026-03-20 13:00:00', '2026-03-20 13:00:00'),
  (9820002, 9800002, 'DSP-9800002',
   2000001, 'ACCEPT',
   'SUCCESS', '审核人员已受理案件', NULL,
   '2026-03-20 13:10:00',
   0, '2026-03-20 13:10:00', '2026-03-20 13:10:00'),
  (9820003, 9800003, 'DSP-9800003',
   3000001, 'SUBMIT_EVIDENCE',
   'SUCCESS', '已提交证据', NULL,
   '2026-03-20 13:20:00',
   0, '2026-03-20 13:20:00', '2026-03-20 13:20:00');

INSERT INTO `regulator_report`
(
  `id`, `report_no`,
  `report_type`, `report_title`,
  `report_content`, `report_file_url`,
  `generator_id`,
  `status`, `generate_time`, `summary_hash`,
  `chain_status`, `tx_hash`, `block_height`,
  `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  (9900001, 'RPT-9900001',
   'RISK_NOTICE', '风险审查报告',
   '本报告汇总风险处置结论，并给出监管侧后续处置建议。',
   NULL,
   4000001,
   'GENERATING', '2026-03-21 09:00:00', NULL,
   '',
   NULL, NULL,
   0,
   '2026-03-21 09:00:00', '2026-03-21 09:00:00');

-- -----------------------------
-- 6.5) 状态矩阵扩展
-- -----------------------------
-- 增补多状态记录，用于列表筛选与状态流转测试
INSERT INTO `work`
(
  `id`, `work_no`,
  `creator_account_id`, `creator_subject_id`, `creator_did_id`,
  `title`, `description`, `work_type`,
  `status`,
  `file_hash`, `meta_hash`,
  `cover_url`,
  `submit_time`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (7000002, 'WK-7000002',
   2000001, 5000002, 6000001,
   'AIGC合规手册',
   '已确权作品，用于证书生成与验真链路测试',
   'TEXT',
   'OWNERSHIP_CONFIRMED',
   '89d4df766f8d85c8f4f5c9f6b1ee9ed2c44917f7adf4f03a6f0d8f56e44e3156',
   'd2b23f0f9576abf4ab64cb8c7039cf95b6ab41d86e489f7756fd8ef8f8e6b950',
   'https://assets.lifechain.cn/covers/WK-7000002.png',
   '2026-03-18 08:30:00',
   0, 0,
   '2026-03-18 08:30:00', '2026-03-19 08:30:00');

INSERT INTO `claim_application`
(
  `id`, `claim_no`,
  `work_id`, `work_no`,
  `applicant_account_id`, `applicant_did_id`,
  `status`, `chain_status`,
  `submit_time`,
  `review_time`, `approve_time`,
  `chain_submit_time`, `chain_confirm_time`,
  `tx_hash`, `block_height`,
  `summary_hash`,
  `reviewer_id`, `review_comment`,
  `reject_reason`, `reason_code`, `fail_reason`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (8000002, 'CLM-8000002',
   7000002, 'WK-7000002',
   2000001, 6000001,
   'CLAIM_SUCCESS', 'CHAIN_SUCCESS',
   '2026-03-18 09:00:00',
   '2026-03-18 10:00:00', '2026-03-18 10:00:00',
   '2026-03-18 10:01:00', '2026-03-18 10:05:00',
   NULL, NULL,
   '5f4dcc3b5aa765d61d8327deb882cf995f4dcc3b5aa765d61d8327deb882cf99',
   1000001, '管理员审核通过',
   NULL, NULL, NULL,
   0, 0,
   '2026-03-18 09:00:00', '2026-03-18 10:05:00');

INSERT INTO `trade_order`
(
  `id`, `order_no`,
  `work_id`, `work_no`,
  `listing_id`, `listing_no`,
  `buyer_account_id`, `buyer_subject_id`,
  `creator_account_id`, `creator_subject_id`,
  `order_status`, `license_type`,
  `price_amount`, `pay_amount`,
  `currency`, `pay_channel`, `pay_status`,
  `expire_time`, `pay_time`, `complete_time`, `cancel_time`, `cancel_reason`,
  `request_id`, `version`, `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  (9100003, 'ORDER-9100003',
   7000001, 'WK-7000001',
   6200001, 'LST-6200001',
   3000001, 5000004,
   2000001, 5000002,
   'ORDER_EXCEPTION', 'PERSONAL_USE',
   100000, 100000,
   'CNY', 'WECHAT_PAY', 'PAY_FAILED',
   '2026-03-22 00:00:00', NULL, NULL, NULL, '支付回调金额不一致',
   'REQ-ORDER-9100003', 0, 0,
   '2026-03-20 14:00:00', '2026-03-20 14:05:00');

INSERT INTO `payment_record`
(
  `id`, `payment_no`,
  `order_id`, `order_no`,
  `pay_channel`, `pay_status`,
  `pay_amount`, `currency`,
  `third_trade_no`, `prepay_id`, `callback_raw_ref`,
  `pay_time`, `callback_time`, `expire_time`,
  `fail_reason`, `request_id`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (9300003, 'PAY-9300003',
   9100003, 'ORDER-9100003',
   'WECHAT_PAY', 'PAY_FAILED',
   100000, 'CNY',
   'wx_third_trade_9100003', 'wx_prepay_9100003', NULL,
   NULL, '2026-03-20 14:05:00', '2026-03-22 00:00:00',
   '支付回调实付金额不匹配',
   'REQ-PAY-9100003',
   0, '2026-03-20 14:01:00', '2026-03-20 14:05:00');

INSERT INTO `dispute_case`
(
  `id`, `case_no`,
  `order_id`, `order_no`,
  `work_id`, `work_no`,
  `applicant_account_id`, `respondent_account_id`,
  `dispute_type`,
  `status`,
  `description`,
  `submit_time`, `accept_time`, `close_time`,
  `result_summary`,
  `reason_code`,
  `chain_status`, `tx_hash`, `block_height`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (9800005, 'DSP-9800005',
   9100002, 'ORDER-9100002',
   7000001, 'WK-7000001',
   3000001, 2000001,
   'LICENSE_SCOPE_DISPUTE',
   'DISPUTE_RESOLVED_PENDING_CHAIN',
   '争议结论已提交链上，等待回执确认',
   '2026-03-20 15:00:00', '2026-03-20 15:05:00', '2026-03-20 15:20:00',
   '建议执行退款与逆分账',
   'RC-DSP-RESOLVE',
   'CHAIN_SUBMITTED', NULL, NULL,
   0, 0,
   '2026-03-20 15:00:00', '2026-03-20 15:20:00');

INSERT INTO `dispute_process_record`
(
  `id`, `case_id`, `case_no`,
  `operator_id`, `action`,
  `action_result`, `comment`, `reason_code`,
  `process_time`,
  `deleted_flag`, `created_at`, `updated_at`
)
VALUES
  (9820005, 9800005, 'DSP-9800005',
   4000001, 'RESOLVE',
   'DISPUTE_RESOLVED_PENDING_CHAIN', '争议结论已提交链上',
   'RC-DSP-RESOLVE',
   '2026-03-20 15:20:00',
   0, '2026-03-20 15:20:00', '2026-03-20 15:20:00');

-- -----------------------------
-- 7) 结算（用于上链重试测试）
--    - 结算记录状态为 SETTLE_FAILED（管理端可重试并重新提交上链）
-- -----------------------------
INSERT INTO `settlement_record`
(
  `id`, `settle_no`,
  `order_id`, `order_no`,
  `work_id`, `work_no`,
  `total_amount`,
  `status`, `chain_status`,
  `settle_time`,
  `complete_time`,
  `tx_hash`, `block_height`,
  `fail_reason`,
  `retry_count`,
  `request_id`,
  `deleted_flag`, `version`,
  `created_at`, `updated_at`
)
VALUES
  (9000001, 'SETTLE-9000001',
   9100001, 'ORDER-9100001',
   7000001, 'WK-7000001',
   100000,
   'SETTLE_FAILED', 'CHAIN_FAILED',
   '2026-03-20 10:00:00',
   NULL,
   NULL, NULL,
   'CHAIN_GATEWAY_TIMEOUT_ON_INITIAL_SUBMISSION',
   2,
   'REQ-SETTLE-9000001',
   0, 0,
   '2026-03-20 10:00:00', '2026-03-20 10:00:00'),

  -- 用于逆分账测试（可选）：
  (9000002, 'SETTLE-9000002',
   9100002, 'ORDER-9100002',
   7000001, 'WK-7000001',
   50000,
   'SETTLE_SUCCESS', 'CHAIN_SUCCESS',
   '2026-03-20 11:00:00',
   NULL,
   NULL, NULL,
   NULL,
   0,
   'REQ-SETTLE-9000002',
   0, 0,
   '2026-03-20 11:00:00', '2026-03-20 11:00:00');

INSERT INTO `settlement_item`
(
  `id`, `settle_id`, `settle_no`,
  `account_id`, `role_type`,
  `ratio`, `amount`,
  `status`,
  `deleted_flag`,
  `created_at`, `updated_at`
)
VALUES
  -- SETTLE-9000001：总额=100000，创作者=0.8000 => 80000，平台=0.2000 => 20000
  (9010001, 9000001, 'SETTLE-9000001',
   2000001, 'CREATOR',
   0.8000, 80000,
   'FAILED',
   0, '2026-03-20 10:00:00', '2026-03-20 10:00:00'),
  (9010002, 9000001, 'SETTLE-9000001',
   0, 'PLATFORM',
   0.2000, 20000,
   'FAILED',
   0, '2026-03-20 10:00:00', '2026-03-20 10:00:00'),

  -- SETTLE-9000002：总额=50000，创作者=0.8000 => 40000，平台=0.2000 => 10000
  (9010003, 9000002, 'SETTLE-9000002',
   2000001, 'CREATOR',
   0.8000, 40000,
   'SUCCESS',
   0, '2026-03-20 11:00:00', '2026-03-20 11:00:00'),
  (9010004, 9000002, 'SETTLE-9000002',
   0, 'PLATFORM',
   0.2000, 10000,
   'SUCCESS',
   0, '2026-03-20 11:00:00', '2026-03-20 11:00:00');

COMMIT;


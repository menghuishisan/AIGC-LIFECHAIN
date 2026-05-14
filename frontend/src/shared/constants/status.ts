/**
 * 状态枚举中文映射
 * 与后端所有状态枚举对齐，前端统一从此获取状态文案
 */

/** 账户状态 */
export const AccountStatusMap: Record<string, string> = {
  REGISTERED: '已注册',
  AUTH_PENDING: '认证审核中',
  AUTH_REJECTED: '认证驳回',
  AUTH_APPROVED: '认证通过',
  ACCOUNT_FROZEN: '账户已冻结',
  ACCOUNT_DISABLED: '账户已停用'
}

/** 账户类型 */
export const AccountTypeMap: Record<string, string> = {
  PERSONAL: '个人账户',
  ENTERPRISE: '企业账户',
  PLATFORM: '平台账户',
  REGULATOR: '监管账户'
}

/** 角色 */
export const RoleMap: Record<string, string> = {
  CREATOR: '创作者',
  BUYER: '购买者',
  PLATFORM_ADMIN: '平台管理员',
  REGULATOR: '监管员'
}

/** DID 状态 */
export const DidStatusMap: Record<string, string> = {
  DID_NOT_APPLIED: '未申请',
  DID_PENDING: '审核中',
  DID_APPROVED_PENDING_CHAIN: '审核通过待上链',
  DID_ACTIVE: '已生效',
  DID_CHAIN_FAILED: '上链失败',
  DID_SUSPEND_PENDING_CHAIN: '挂起待上链确认',
  DID_SUSPENDED: '已暂停',
  DID_REVOKE_PENDING_CHAIN: '吊销待上链确认',
  DID_REVOKED: '已撤销'
}

/** 作品状态 */
export const WorkStatusMap: Record<string, string> = {
  DRAFT: '草稿',
  UPLOADED: '已上传',
  FEATURE_PENDING: '特征提取中',
  READY_FOR_CLAIM: '可确权',
  SIMILARITY_HIGH_RISK: '高相似度待人工复核',
  CLAIM_REVIEWING: '确权审核中',
  CLAIM_CHAIN_PENDING: '确权上链中',
  OWNERSHIP_CONFIRMED: '确权成功',
  CLAIM_FAILED: '确权失败',
  LISTED: '已上架',
  UNLISTED: '已下架',
  RISK_FROZEN: '风险冻结',
  ARCHIVED: '已归档'
}

/** 确权状态 */
export const ClaimStatusMap: Record<string, string> = {
  CLAIM_DRAFT: '草稿',
  CLAIM_SUBMITTED: '已提交',
  CLAIM_REVIEWING: '审核中',
  CLAIM_REJECTED: '已驳回',
  CLAIM_APPROVED_PENDING_CHAIN: '审核通过待上链',
  CLAIM_CHAIN_FAILED: '上链失败',
  CLAIM_SUCCESS: '确权成功',
  CLAIM_CANCELLED: '已取消'
}

/** 证书状态 */
export const CertStatusMap: Record<string, string> = {
  CERT_PENDING: '待生成',
  CERT_GENERATING: '生成中',
  CERT_ACTIVE: '有效',
  CERT_INVALID: '失效',
  CERT_REVOKED: '已撤销',
  CERT_REGENERATING: '重新生成中'
}

/** 订单状态 */
export const OrderStatusMap: Record<string, string> = {
  ORDER_CREATED: '已创建',
  ORDER_EXPIRED: '已过期',
  PAY_PENDING_CONFIRM: '支付确认中',
  PAY_CONFIRMED: '支付已确认',
  AUTH_GRANTING: '授权中',
  AUTH_GRANTED: '已授权',
  SETTLEMENT_PENDING: '待结算',
  ORDER_COMPLETED: '已完成',
  REFUND_PENDING: '退款中',
  REFUNDED: '已退款',
  ORDER_FROZEN: '订单冻结',
  ORDER_CANCELLED: '已取消',
  ORDER_EXCEPTION: '异常'
}

/** 支付状态 */
export const PayStatusMap: Record<string, string> = {
  PAY_INIT: '待创建支付单',
  PAY_PENDING: '待支付确认',
  PAY_SUCCESS: '支付成功',
  PAY_FAILED: '支付失败',
  PAY_CLOSED: '支付关闭'
}

/** 支付渠道 */
export const PayChannelMap: Record<string, string> = {
  WECHAT_PAY: '微信支付',
  ALIPAY: '支付宝'
}

/** 授权类型 */
export const LicenseTypeMap: Record<string, string> = {
  PERSONAL_USE: '个人使用',
  COMMERCIAL_USE: '商业使用',
  EXCLUSIVE: '独占授权'
}

/** 上架状态 */
export const ListingStatusMap: Record<string, string> = {
  PENDING_REVIEW: '待审核',
  LISTED: '已上架',
  UNLISTED: '已下架',
  REJECTED: '已驳回',
  FROZEN: '已冻结'
}

/** 授权状态 */
export const LicenseStatusMap: Record<string, string> = {
  LICENSE_PENDING: '待生效',
  LICENSE_ACTIVE: '已生效',
  LICENSE_EXPIRED: '已过期',
  LICENSE_TERMINATED: '已终止',
  LICENSE_FROZEN: '已冻结',
  LICENSE_REVOKED: '已撤销'
}

/** 结算状态 */
export const SettlementStatusMap: Record<string, string> = {
  SETTLE_NOT_STARTED: '未开始',
  SETTLE_READY: '待结算',
  SETTLE_PROCESSING: '结算中',
  SETTLE_PARTIAL_SUCCESS: '部分成功',
  SETTLE_SUCCESS: '结算成功',
  SETTLE_FAILED: '结算失败',
  REVERSE_PENDING: '逆分账处理中',
  REVERSE_SUCCESS: '逆分账成功',
  REVERSE_FAILED: '逆分账失败',
  SETTLE_FROZEN: '结算冻结'
}

/** 争议状态 */
export const DisputeStatusMap: Record<string, string> = {
  DISPUTE_SUBMITTED: '已提交',
  DISPUTE_ACCEPTED: '已受理',
  DISPUTE_EVIDENCE_PENDING: '待补证',
  DISPUTE_REVIEWING: '审查中',
  DISPUTE_RESOLVED: '已解决',
  DISPUTE_RESOLVED_PENDING_CHAIN: '解决待上链确认',
  DISPUTE_REJECTED: '已驳回',
  DISPUTE_REJECTED_PENDING_CHAIN: '驳回待上链确认',
  DISPUTE_CLOSED: '已关闭',
  DISPUTE_CLOSED_PENDING_CHAIN: '关闭待上链确认'
}

/** 冻结状态 */
export const FreezeStatusMap: Record<string, string> = {
  FREEZE_APPLIED: '已发起',
  FREEZE_APPROVED: '已批准',
  FREEZE_APPROVED_PENDING_CHAIN: '冻结待上链确认',
  FREEZE_REJECTED: '已拒绝',
  UNFREEZE_APPLIED: '已发起解冻',
  UNFREEZE_PENDING_CHAIN: '解冻待上链确认',
  UNFREEZE_APPROVED: '已解冻'
}

/** 冻结模式 */
export const FreezeModeMap: Record<string, string> = {
  REVIEW_REQUIRED: '需复核',
  REGULATOR_DIRECT: '紧急直接冻结'
}

/** 风险状态 */
export const RiskStatusMap: Record<string, string> = {
  RISK_NORMAL: '正常',
  RISK_MARKED: '已标记',
  RISK_REVIEWING: '审查中',
  RISK_FROZEN: '已冻结',
  RISK_RELEASED: '已释放',
  RISK_CONFIRMED: '已确认'
}

/** 链上状态 */
export const ChainStatusMap: Record<string, string> = {
  CHAIN_PENDING: '待上链',
  CHAIN_SUBMITTED: '已提交待回执',
  CHAIN_SUCCESS: '链上成功',
  CHAIN_FAILED: '链上失败'
}

/** 目标类型 */
export const TargetTypeMap: Record<string, string> = {
  ACCOUNT: '账户',
  WORK: '作品',
  ORDER: '订单',
  LICENSE: '授权'
}

/** 业务类型 */
export const BizTypeMap: Record<string, string> = {
  ACCOUNT: '账户',
  DID: 'DID',
  WORK: '作品',
  CLAIM: '确权',
  CERTIFICATE: '证书',
  ORDER: '订单',
  LISTING: '上架',
  LICENSE: '授权',
  PAYMENT: '支付',
  SETTLEMENT: '结算',
  REVERSE_SETTLEMENT: '逆分账',
  RISK: '风险',
  FREEZE: '冻结',
  DISPUTE: '争议',
  REPORT: '报告'
}

/** 作品类型 */
export const WorkTypeMap: Record<string, string> = {
  TEXT: '文本',
  IMAGE: '图片',
  AUDIO: '音频',
  VIDEO: '视频',
  MODEL: '模型',
  OTHER: '其他'
}

/** 审核结果 */
export const ReviewResultMap: Record<string, string> = {
  APPROVED: '通过',
  REJECTED: '驳回'
}

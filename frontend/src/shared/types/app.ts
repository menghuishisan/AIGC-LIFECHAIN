/**
 * 应用聚合模块类型定义
 * 与后端 app 模块 DTO 对齐：统计、收益、通知、轨迹、审计、配置、附件等
 */

/** 平台概览统计 */
export interface StatsOverviewVO {
  totalAccounts: number
  totalWorks: number
  totalOrders: number
  totalSettlements: number
  totalTradeAmount: number
  todayNewAccounts: number
  todayNewOrders: number
}

/** 趋势点 */
export interface TrendPointVO {
  date: string
  count: number
}

/** 统计计数 */
export interface StatsCountVO {
  total: number
  pending: number
  success: number
  failed: number
}

/** 分布统计 */
export interface DistributionVO {
  workTypeDistribution: Record<string, number>
  orderStatusDistribution: Record<string, number>
  riskStatusDistribution: Record<string, number>
}

/** 收益汇总 */
export interface IncomeSummaryVO {
  totalIncome: number
  totalSuccessCount: number
  totalCount: number
}

/** 收益明细（与后端 IncomeItemVO 对齐） */
export interface IncomeDetailVO {
  settleNo: string
  roleType: string
  ratio: number
  amount: number
  status: string
  createdAt: string
}

/** 通知消息 */
export interface MessageNoticeVO {
  noticeNo: string
  noticeType: string
  title: string
  content: string
  readFlag: boolean
  bizType?: string
  bizNo?: string
  createdAt: string
}

/** 轨迹事件 */
export interface TraceEventVO {
  eventType: string
  description: string
  operator?: string
  eventTime: string
  extraData?: string
}

/** 状态变更历史 */
export interface StatusHistoryVO {
  bizType: string
  bizNo: string
  fromStatus: string
  toStatus: string
  changeReason?: string
  reasonCode?: string
  changeTime: string
}

/** 审计日志 */
export interface AuditLogVO {
  targetType: string
  targetNo: string
  action: string
  actionDetail?: string
  operatorRole?: string
  result?: string
  reasonCode?: string
  logTime: string
}

/** 系统配置 */
export interface SysConfigVO {
  configKey: string
  configValue: string
  configType?: string
  description?: string
}

/** 创建/更新系统配置请求 */
export interface UpsertSysConfigRequest {
  configKey: string
  configValue: string
  configType?: string
  description?: string
}

/** 附件 */
export interface AttachmentVO {
  objectName: string
  accessUrl: string
  fileSize: number
  contentType: string
  bizType?: string
  bizNo?: string
}

/** 文件上传回调请求 */
export interface FileCallbackRequest {
  objectName: string
  fileSize: number
  contentType: string
  bizType?: string
  bizNo?: string
  requestId: string
}

/** 链交易记录（与后端 ChainTxRecordVO 对齐） */
export interface ChainReceiptVO {
  bizType: string
  bizNo: string
  txType?: string
  txHash: string
  blockHeight?: number
  chainStatus: string
  failReason?: string
  channelName?: string
  chaincodeName?: string
  endorsementSummary?: string
  submitTime?: string
  confirmTime?: string
  createdAt: string
}

/** 链交易重试请求 */
export interface ChainRetryRequest {
  txHash: string
  requestId: string
}

/** 结算记录（与后端 SettlementRecordVO 对齐） */
export interface SettlementDetailVO {
  settleNo: string
  orderNo: string
  workNo: string
  totalAmount: number
  settleStatus: string
  chainStatus?: string
  txHash?: string
  blockHeight?: number
  settleTime?: string
  completeTime?: string
  items: SettlementItemVO[]
}

/** 结算明细项 */
export interface SettlementItemVO {
  roleType: string
  accountNo: string
  ratio: number
  amount: number
  itemStatus: string
}

/** 上传策略响应 */
export interface UploadPolicyVO {
  uploadUrl: string
  objectName: string
  accessUrl: string
}

/** 大屏实时数据（与后端 ScreenRealtimeVO 对齐） */
export interface ScreenRealtimeVO {
  totalAccounts: number
  totalWorks: number
  totalOrders: number
  totalSettlements: number
  totalClaims: number
  totalRiskEvents: number
  totalFreezeRecords: number
  todayNewWorks: number
  todayNewOrders: number
  todayNewClaims: number
}

/** 分账模板项 */
export interface TemplateItemDTO {
  roleType: string
  ratio: number
  description?: string
}

/** 创建分账模板请求 */
export interface CreateSettleTemplateRequest {
  templateName: string
  description?: string
  items: TemplateItemDTO[]
  requestId: string
}

/** 分账模板 */
export interface SettleTemplateVO {
  templateCode: string
  templateName: string
  description?: string
  status: string
  items: TemplateItemDTO[]
  createdAt: string
}

/** 监管综合搜索结果（与后端 RegulatorSearchResultVO 对齐） */
export interface RegulatorSearchResultVO {
  objectType: string
  objectNo: string
  title: string
  status: string
  matchField: string
  createdAt: string
}

/** 作品结算规则（与后端 WorkSettleRuleVO 对齐） */
export interface WorkSettleRuleVO {
  workNo: string
  templateCode: string
  platformRatio: number
  creatorRatio: number
  effectiveTime: string
  ruleStatus: string
}

/** 绑定结算规则请求（与后端 BindSettleRuleRequest 对齐） */
export interface BindSettleRuleRequest {
  workNo: string
  templateCode?: string
  platformRatio: number
  creatorRatio: number
  creatorAccountNo: string
  requestId: string
}

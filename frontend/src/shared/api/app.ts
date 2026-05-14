/**
 * 应用聚合模块 API
 * 对接后端 app 模块所有接口：统计、收益、通知、轨迹、审计、配置、链管理等
 */
import { get, post, del } from './http'
import type {
  StatsOverviewVO, TrendPointVO, StatsCountVO, DistributionVO,
  IncomeSummaryVO, IncomeDetailVO, MessageNoticeVO,
  TraceEventVO, StatusHistoryVO, AuditLogVO,
  SysConfigVO, UpsertSysConfigRequest, AttachmentVO, FileCallbackRequest,
  ChainReceiptVO, ChainRetryRequest, SettlementDetailVO,
  UploadPolicyVO, ScreenRealtimeVO,
  CreateSettleTemplateRequest, SettleTemplateVO,
  RegulatorSearchResultVO, WorkSettleRuleVO, BindSettleRuleRequest,
  PageResult, PageQuery
} from '@/shared/types'

/* ========== 统计接口 ========== */

/** 平台概览 */
export const getStatsOverview = () =>
  get<StatsOverviewVO>('/api/stats/overview')

/** 确权趋势 */
export const getClaimsTrend = (days?: number) =>
  get<TrendPointVO[]>('/api/stats/claims/trend', { days })

/** 订单趋势 */
export const getOrdersTrend = (days?: number) =>
  get<TrendPointVO[]>('/api/stats/orders/trend', { days })

/** 结算统计 */
export const getSettlementsStats = () =>
  get<StatsCountVO>('/api/stats/settlements')

/** 验真统计 */
export const getVerifyStats = () =>
  get<StatsCountVO>('/api/stats/verify')

/** 风险统计 */
export const getRiskStats = () =>
  get<StatsCountVO>('/api/stats/risk')

/** 分布统计 */
export const getDistribution = () =>
  get<DistributionVO>('/api/stats/distribution')

/** 大屏实时数据 */
export const getScreenRealtime = () =>
  get<ScreenRealtimeVO>('/api/screen/realtime')

/* ========== 收益接口 ========== */

/** 收益汇总 */
export const getIncomeSummary = () =>
  get<IncomeSummaryVO>('/api/income/summary')

/** 收益明细 */
export const getIncomeDetails = (params: PageQuery) =>
  get<PageResult<IncomeDetailVO>>('/api/income/details', params)

/* ========== 通知接口 ========== */

/** 通知列表 */
export const getNotices = (params: PageQuery & { noticeType?: string; readFlag?: boolean | number }) =>
  get<PageResult<MessageNoticeVO>>('/api/notices', {
    ...params,
    readFlag: params.readFlag !== undefined ? (params.readFlag ? 1 : 0) : undefined
  })

/** 通知详情 */
export const getNoticeDetail = (noticeNo: string) =>
  get<MessageNoticeVO>(`/api/notices/${noticeNo}`)

/** 标记已读 */
export const markNoticeRead = (noticeNo: string, requestId: string) =>
  post<void>(`/api/notices/${noticeNo}/read`, null, { params: { requestId } })

/* ========== 轨迹接口 ========== */

/** 作品全链路追踪 */
export const getWorkTraces = (workNo: string) =>
  get<TraceEventVO[]>(`/api/traces/work/${workNo}`)

/** 订单全链路追踪 */
export const getOrderTraces = (orderNo: string) =>
  get<TraceEventVO[]>(`/api/traces/order/${orderNo}`)

/* ========== 审计接口 ========== */

/** 状态变更历史 */
export const getStatusHistory = (params: PageQuery & { bizType: string; bizNo: string }) =>
  get<PageResult<StatusHistoryVO>>('/api/status-history', params)

/** 审计日志 */
export const getAuditLogs = (params: PageQuery & { targetType?: string; action?: string }) =>
  get<PageResult<AuditLogVO>>('/api/admin/audit/logs', params)

/* ========== 系统配置接口 ========== */

/** 查询配置列表 */
export const getConfigs = (configType?: string) =>
  get<SysConfigVO[]>('/api/admin/configs', { configType })

/** 查询单个配置（返回配置值字符串） */
export const getConfig = (configKey: string) =>
  get<string>(`/api/admin/configs/${configKey}`)

/** 创建/更新配置 */
export const upsertConfig = (data: UpsertSysConfigRequest) =>
  post<void>('/api/admin/configs', data)

/** 删除配置 */
export const deleteConfig = (configKey: string) =>
  del<void>(`/api/admin/configs/${configKey}`)

/* ========== 文件接口 ========== */

/** 获取上传策略 */
export const getUploadPolicy = (fileName: string, bizType?: string) =>
  get<UploadPolicyVO>('/api/files/upload-policy', { fileName, bizType })

/** 文件上传回调 */
export const fileCallback = (data: FileCallbackRequest) =>
  post<AttachmentVO>('/api/files/callback', data)

/* ========== 附件接口 ========== */

/** 管理员查询附件列表 */
export const getAttachments = (params: PageQuery & { bizType?: string; bizNo?: string }) =>
  get<PageResult<AttachmentVO>>('/api/admin/attachments', params)

/* ========== 链管理接口 ========== */

/** 链回执查询（返回列表） */
export const getChainReceipt = (bizType: string, bizNo: string) =>
  get<ChainReceiptVO[]>(`/api/admin/chain/receipts/${bizType}/${bizNo}`)

/** 链交易重试 */
export const retryChainTx = (data: ChainRetryRequest) =>
  post<ChainReceiptVO>('/api/admin/chain/retry', data)

/** 链交易详情 */
export const getChainTxDetail = (txHash: string) =>
  get<ChainReceiptVO>(`/api/admin/chain/tx/${txHash}`)

/* ========== 结算接口 ========== */

/** 订单结算信息 */
export const getOrderSettlement = (orderNo: string) =>
  get<SettlementDetailVO>(`/api/orders/${orderNo}/settlement`)

/** 管理员结算列表 */
export const getAdminSettlements = (params: PageQuery & { status?: string }) =>
  get<PageResult<SettlementDetailVO>>('/api/admin/settlements', params)

/** 管理员结算详情 */
export const getSettlementDetail = (settleNo: string) =>
  get<SettlementDetailVO>(`/api/admin/settlements/${settleNo}`)

/** 结算重试 */
export const retrySettlement = (data: { settleNo: string; requestId: string }) =>
  post<void>('/api/admin/settlements/retry', data)

/** 逆分账 */
export const reverseSettlement = (data: { settleNo: string; reason: string; requestId: string }) =>
  post<void>('/api/admin/settlements/reverse', data)

/* ========== 综合搜索接口 ========== */

/** 监管综合搜索 */
export const regulatorSearch = (params: PageQuery & { keyword?: string; targetType?: string }) =>
  get<PageResult<RegulatorSearchResultVO>>('/api/regulator/search', params)

/* ========== 分账模板接口 ========== */

/** 创建分账模板 */
export const createSettleTemplate = (data: CreateSettleTemplateRequest) =>
  post<SettleTemplateVO>('/api/admin/settle/templates', data)

/** 查询分账模板列表 */
export const getSettleTemplates = () =>
  get<SettleTemplateVO[]>('/api/admin/settle/templates')

/** 查询分账模板详情 */
export const getSettleTemplate = (templateCode: string) =>
  get<SettleTemplateVO>(`/api/admin/settle/templates/${templateCode}`)

/* ========== 结算规则接口 ========== */

/** 绑定作品结算规则（管理员） */
export const bindSettleRule = (workNo: string, data: BindSettleRuleRequest) =>
  post<WorkSettleRuleVO>(`/api/works/${workNo}/settle-rule`, data)

/** 查询作品生效结算规则 */
export const getSettleRule = (workNo: string) =>
  get<WorkSettleRuleVO>(`/api/works/${workNo}/settle-rule`)

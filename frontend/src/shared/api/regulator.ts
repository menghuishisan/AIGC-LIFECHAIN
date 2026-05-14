/**
 * 监管模块 API
 * 对接后端 regulator 模块所有接口
 */
import { get, post } from './http'
import type {
  CreateDisputeRequest, AddEvidenceRequest, DisputeProcessRequest,
  DisputeCaseVO, RegulatorDisputeQuery, RegulatorDisputeListVO,
  FreezeRequest, UnfreezeRequest, ReviewFreezeRequest, FreezeRecordVO,
  CreateRiskEventRequest, HandleRiskEventRequest, RiskEventVO,
  CreateReportRequest, HandleReportRequest, ReportVO,
  PageResult, PageQuery
} from '@/shared/types'

/* ========== 争议接口 ========== */

/** 创建争议 */
export const createDispute = (data: CreateDisputeRequest) =>
  post<DisputeCaseVO>('/api/disputes', data)

/** 补充证据 */
export const addEvidence = (caseNo: string, data: AddEvidenceRequest) =>
  post<DisputeCaseVO>(`/api/disputes/${caseNo}/evidences`, data)

/** 管理员处理争议 */
export const processDispute = (data: DisputeProcessRequest) =>
  post<DisputeCaseVO>('/api/admin/disputes/process', data)

/** 争议详情 */
export const getDisputeDetail = (caseNo: string) =>
  get<DisputeCaseVO>(`/api/disputes/${caseNo}`)

/** 我的争议列表 */
export const getMyDisputes = (params: PageQuery & { status?: string }) =>
  get<PageResult<DisputeCaseVO>>('/api/disputes', params)

/** 监管方全量争议列表 */
export const getRegulatorDisputes = (params: PageQuery & RegulatorDisputeQuery) =>
  get<PageResult<RegulatorDisputeListVO>>('/api/regulator/disputes', params)

/* ========== 冻结接口 ========== */

/** 冻结申请 */
export const applyFreeze = (data: FreezeRequest) =>
  post<FreezeRecordVO>('/api/regulator/freeze/apply', data)

/** 账户冻结申请 */
export const applyAccountFreeze = (data: FreezeRequest) =>
  post<FreezeRecordVO>('/api/regulator/account/freeze/apply', data)

/** 紧急直接冻结 */
export const directFreeze = (data: FreezeRequest) =>
  post<FreezeRecordVO>('/api/regulator/freeze/direct', data)

/** 解冻申请 */
export const applyUnfreeze = (data: UnfreezeRequest) =>
  post<FreezeRecordVO>('/api/regulator/unfreeze/apply', data)

/** 冻结记录详情 */
export const getFreezeDetail = (freezeNo: string) =>
  get<FreezeRecordVO>(`/api/regulator/freeze/${freezeNo}`)

/** 冻结记录列表 */
export const getFreezeList = (params: PageQuery & { targetType?: string; status?: string }) =>
  get<PageResult<FreezeRecordVO>>('/api/regulator/freeze', params)

/** 事后复核 */
export const reviewFreeze = (data: ReviewFreezeRequest) =>
  post<FreezeRecordVO>('/api/regulator/freeze/review', data)

/* ========== 风险事件接口 ========== */

/** 风险标记 */
export const markRisk = (data: CreateRiskEventRequest) =>
  post<RiskEventVO>('/api/regulator/risk/mark', data)

/** 处理风险事件 */
export const handleRisk = (data: HandleRiskEventRequest) =>
  post<RiskEventVO>('/api/regulator/risk/handle', data)

/** 风险事件列表 */
export const getRiskList = (params: PageQuery) =>
  get<PageResult<RiskEventVO>>('/api/regulator/risk', params)

/** 风险事件详情 */
export const getRiskDetail = (riskNo: string) =>
  get<RiskEventVO>(`/api/regulator/risk/${riskNo}`)

/* ========== 监管报告接口 ========== */

/** 创建监管报告 */
export const createReport = (data: CreateReportRequest) =>
  post<ReportVO>('/api/regulator/reports', data)

/** 创建并生成报告 */
export const generateReport = (data: CreateReportRequest) =>
  post<ReportVO>('/api/regulator/reports/generate', data)

/** 处理报告 */
export const handleReport = (reportNo: string, data: HandleReportRequest) =>
  post<ReportVO>(`/api/regulator/reports/${reportNo}/handle`, data)

/** 报告列表 */
export const getReportList = (params: PageQuery & { reportType?: string; status?: string }) =>
  get<PageResult<ReportVO>>('/api/regulator/reports', params)

/** 报告详情 */
export const getReportDetail = (reportNo: string) =>
  get<ReportVO>(`/api/regulator/reports/${reportNo}`)

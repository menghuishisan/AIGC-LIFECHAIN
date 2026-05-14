/**
 * 监管与争议模块类型定义
 * 与后端 regulator 模块 DTO 对齐
 */

/** 创建争议请求 */
export interface CreateDisputeRequest {
  orderNo?: string
  workNo?: string
  respondentAccountNo: string
  disputeType: string
  description: string
  evidenceUrls?: string[]
  requestId: string
}

/** 争议补充证据请求 */
export interface AddEvidenceRequest {
  evidenceType: string
  fileUrl: string
  fileHash?: string
  description?: string
  requestId: string
}

/** 争议处理请求 */
export interface DisputeProcessRequest {
  caseNo: string
  action: string
  reasonCode?: string
  resultSummary?: string
  comment?: string
  requestId: string
}

/** 争议处理记录 */
export interface DisputeProcessRecord {
  action: string
  operatorAccountNo: string
  comment?: string
  processTime: string
}

/** 争议证据 */
export interface DisputeEvidence {
  evidenceUrl: string
  description?: string
  uploadTime: string
  uploaderAccountNo: string
}

/** 争议基础信息 */
export interface DisputeBasicInfo {
  caseNo: string
  disputeType: string
  description: string
  resultSummary?: string
  evidences: DisputeEvidence[]
  processRecords: DisputeProcessRecord[]
}

/** 争议详情 */
export interface DisputeCaseVO {
  basicInfo: DisputeBasicInfo
  statusInfo: { status: string }
  timeInfo: Record<string, string>
  relationInfo: {
    orderNo?: string
    workNo?: string
    applicantAccountNo: string
    respondentAccountNo: string
  }
  chainInfo?: import('./work').ChainInfo
  allowedActions: string[]
}

/** 监管争议查询参数 */
export interface RegulatorDisputeQuery {
  status?: string
  disputeType?: string
  applicantAccountNo?: string
}

/** 监管争议列表项 */
export interface RegulatorDisputeListVO {
  caseNo: string
  disputeType: string
  status: string
  applicantAccountNo: string
  respondentAccountNo: string
  createdAt: string
}

/** 冻结请求 */
export interface FreezeRequest {
  targetType: string
  targetNo: string
  freezeMode: string
  freezeReason: string
  urgentBasisNo?: string
  reasonCode?: string
  requestId: string
}

/** 解冻请求 */
export interface UnfreezeRequest {
  freezeNo: string
  unfreezeReason: string
  requestId: string
}

/** 冻结复核请求 */
export interface ReviewFreezeRequest {
  freezeNo: string
  approved: boolean
  reviewNote?: string
  requestId: string
}

/** 冻结记录 */
export interface FreezeRecordVO {
  freezeNo: string
  targetType: string
  targetNo: string
  freezeStatus: string
  freezeMode: string
  reviewStatus?: string
  applyRole: string
  reasonCode?: string
  freezeReason: string
  urgentBasisNo?: string
  applyTime?: string
  approveTime?: string
  effectiveTime?: string
  unfreezeTime?: string
  unfreezeReason?: string
  chainStatus?: string
  txHash?: string
  blockHeight?: number
  createdAt: string
}

/** 风险标记请求 */
export interface CreateRiskEventRequest {
  targetType: string
  targetNo: string
  riskLevel: string
  riskType: string
  riskDescription: string
  requestId: string
}

/** 风险处理请求 */
export interface HandleRiskEventRequest {
  riskNo: string
  action: string
  resultSummary?: string
  reasonCode?: string
  requestId: string
}

/** 风险事件 */
export interface RiskEventVO {
  riskNo: string
  targetType: string
  targetNo: string
  riskLevel: string
  riskType: string
  riskDescription: string
  status: string
  resultSummary?: string
  resolveTime?: string
  createdAt: string
}

/** 创建监管报告请求 */
export interface CreateReportRequest {
  reportType: string
  reportTitle: string
  reportContent: string
  targetType?: string
  targetNo?: string
  requestId: string
}

/** 处理监管报告请求 */
export interface HandleReportRequest {
  action: string
  requestId: string
}

/** 监管报告 */
export interface ReportVO {
  reportNo: string
  reportType: string
  reportTitle: string
  reportContent: string
  reportFileUrl?: string
  status: string
  generateTime?: string
  summaryHash?: string
  chainStatus?: string
  txHash?: string
  blockHeight?: number
  createdAt: string
}

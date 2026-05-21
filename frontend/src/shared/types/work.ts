/**
 * 作品、确权、证书、验真模块类型定义
 * 与后端 work 模块 DTO 对齐
 */

/** AIGC 元数据 */
export interface AigcMetaDTO {
  aigcTool?: string
  aigcModel?: string
  aigcVersion?: string
  promptSummary?: string
  generationParams?: string
  generationTime?: string
}

/** 作品上传请求 */
export interface WorkUploadRequest {
  title: string
  workType: string
  description?: string
  coverUrl?: string
  aigcMeta?: AigcMetaDTO
  requestId: string
}

/** 作品元数据更新请求 */
export interface WorkMetaUpdateRequest {
  title?: string
  description?: string
  coverUrl?: string
  aigcMeta?: AigcMetaDTO
  requestId: string
}

/** 作品列表项 */
export interface WorkListVO {
  workNo: string
  title: string
  workType: string
  coverUrl?: string
  status: string
  createdAt: string
}

/** 作品特征信息 */
export interface WorkFeatureVO {
  featureType: string
  perceptualHash: string
  extractStatus: string
  extractTime?: string
}

/** 作品文件信息 */
export interface WorkFileVO {
  fileId: number
  fileName: string
  fileUrl?: string
  fileType: string
  fileSize: number
  purpose: string
}

/** 预览URL响应 */
export interface PreviewUrlVO {
  previewUrl: string | null
  accessLevel: 'FULL' | 'LIMITED'
  fileType: string
  fileName: string
  fileSize: number
  previewDurationSeconds?: number
}

/** 作品详情——基础信息 */
export interface WorkBasicInfo {
  workNo: string
  title: string
  description?: string
  workType: string
  coverUrl?: string
  files?: WorkFileVO[]
  aigcMeta?: AigcMetaDTO
  feature?: WorkFeatureVO
}

/** 作品详情——状态信息 */
export interface WorkStatusInfo {
  status: string
}

/** 作品详情——时间信息 */
export interface WorkTimeInfo {
  createdAt?: string
  updatedAt?: string
}

/** 作品详情——关联信息 */
export interface WorkRelationInfo {
  claimNo?: string
  certNo?: string
  listingNo?: string
}

/** 作品详情——链上信息 */
export interface ChainInfo {
  txHash?: string
  blockHeight?: number
  chainStatus?: string
}

/** 作品详情 */
export interface WorkDetailVO {
  basicInfo: WorkBasicInfo
  statusInfo: WorkStatusInfo
  timeInfo: WorkTimeInfo
  relationInfo: WorkRelationInfo
  chainInfo?: ChainInfo
  allowedActions: string[]
}

/** 确权申请请求 */
export interface ClaimSubmitRequest {
  workNo: string
  requestId: string
}

/** 确权基础信息 */
export interface ClaimBasicInfo {
  claimNo: string
  summaryHash?: string
}

/** 确权状态信息 */
export interface ClaimStatusInfo {
  status: string
  reviewComment?: string
  rejectReason?: string
}

/** 确权详情 */
export interface ClaimDetailVO {
  basicInfo: ClaimBasicInfo
  statusInfo: ClaimStatusInfo
  timeInfo: Record<string, string>
  relationInfo: { workNo: string }
  chainInfo?: ChainInfo
  allowedActions: string[]
}

/** 确权审核请求 */
export interface ClaimReviewRequest {
  claimNo: string
  reviewResult: string
  reviewComment?: string
  reasonCode?: string
  requestId: string
}

/** 证书生成请求 */
export interface GenerateCertificateRequest {
  claimNo: string
  requestId: string
}

/** 证书基础信息 */
export interface CertBasicInfo {
  certNo: string
  certHash?: string
  certFileUrl?: string
  version?: number
}

/** 证书状态信息 */
export interface CertStatusInfo {
  status: string
}

/** 证书时间信息 */
export interface CertTimeInfo {
  issueTime?: string
  expireTime?: string
}

/** 证书详情 */
export interface CertDetailVO {
  basicInfo: CertBasicInfo
  statusInfo: CertStatusInfo
  timeInfo: CertTimeInfo
  relationInfo: { workNo: string; claimNo: string }
  chainInfo?: ChainInfo
  allowedActions: string[]
}

/** 验真请求 */
export interface VerifyRequest {
  queryType: string
  queryValue: string
}

/** 验真结果 */
export interface VerifyResultVO {
  verified: boolean
  certNo?: string
  workNo?: string
  creatorDid?: string
  claimTime?: string
  certStatus?: string
  chainTxHash?: string
  blockHeight?: number
  verifyLevel?: string
  summaryHash?: string
}

/** 验真日志 */
export interface VerifyQueryLogVO {
  queryType: string
  queryValue: string
  querySource: string
  queryIp?: string
  matchFound: number
  resultSummary?: string
  queryTime: string
}

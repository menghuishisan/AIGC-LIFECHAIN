/**
 * 作品、确权、证书、验真 API
 * 对接后端 work 模块所有接口
 */
import { get, post, put, download } from './http'
import type {
  WorkUploadRequest, WorkMetaUpdateRequest, WorkDetailVO, WorkListVO,
  WorkFeatureVO, ClaimSubmitRequest, ClaimDetailVO, ClaimReviewRequest,
  GenerateCertificateRequest, CertDetailVO, VerifyRequest, VerifyResultVO,
  VerifyQueryLogVO, PageResult, PageQuery, PreviewUrlVO
} from '@/shared/types'

/* ========== 作品接口 ========== */

/** 上传作品（multipart） */
export const uploadWork = (formData: FormData) =>
  post<WorkDetailVO>('/api/works/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/** 更新作品元数据 */
export const updateWorkMeta = (workNo: string, data: WorkMetaUpdateRequest) =>
  put<void>(`/api/works/${workNo}/meta`, data)

/** 触发特征提取 */
export const extractFeature = (workNo: string, requestId: string) =>
  post<void>(`/api/works/${workNo}/feature-extract`, null, { params: { requestId } })

/** 查询作品特征 */
export const getWorkFeature = (workNo: string) =>
  get<WorkFeatureVO>(`/api/works/${workNo}/feature`)

/** 我的作品列表 */
export const getMyWorks = (params: PageQuery & { status?: string }) =>
  get<PageResult<WorkListVO>>('/api/works/mine', params)

/** 作品详情 */
export const getWorkDetail = (workNo: string) =>
  get<WorkDetailVO>(`/api/works/${workNo}`)

/** 获取作品文件预览URL（需登录） */
export const getPreviewUrl = (workNo: string, fileId: number) =>
  get<PreviewUrlVO>(`/api/works/${workNo}/preview-url`, { fileId })

/** 获取市场作品文件预览URL（公开） */
export const getMarketPreviewUrl = (workNo: string, fileId: number) =>
  get<PreviewUrlVO>(`/api/market/works/${workNo}/preview-url`, { fileId })

/* ========== 市场接口 ========== */

/** 市场作品列表（后端仅支持 workType 过滤） */
export const getMarketWorks = (params: PageQuery & { workType?: string; keyword?: string }) =>
  get<PageResult<WorkListVO>>('/api/market/works', params)

/** 市场作品详情 */
export const getMarketWorkDetail = (workNo: string) =>
  get<WorkDetailVO>(`/api/market/works/${workNo}`)

/* ========== 确权接口 ========== */

/** 提交确权申请 */
export const submitClaim = (data: ClaimSubmitRequest) =>
  post<ClaimDetailVO>('/api/claims/submit', data)

/** 确权详情 */
export const getClaimDetail = (claimNo: string) =>
  get<ClaimDetailVO>(`/api/claims/${claimNo}`)

/** 确权链上回执（返回 ClaimDetailVO，含 chainInfo） */
export const getClaimChainReceipt = (claimNo: string) =>
  get<ClaimDetailVO>(`/api/claims/${claimNo}/chain-receipt`)

/** 我的确权列表 */
export const getMyClaims = (params: PageQuery & { status?: string }) =>
  get<PageResult<ClaimDetailVO>>('/api/claims', params)

/** 管理员审核确权 */
export const reviewClaim = (data: ClaimReviewRequest) =>
  post<void>('/api/admin/claims/review', data)

/** 管理员确权审核列表 */
export const getAdminClaims = (params: PageQuery & { status?: string }) =>
  get<PageResult<ClaimDetailVO>>('/api/admin/claims', params)

/* ========== 证书接口 ========== */

/** 生成证书 */
export const generateCertificate = (data: GenerateCertificateRequest) =>
  post<CertDetailVO>('/api/admin/certificates/generate', data)

/** 证书详情 */
export const getCertDetail = (certNo: string) =>
  get<CertDetailVO>(`/api/certificates/${certNo}`)

/** 下载证书 PDF */
export const downloadCert = (certNo: string) =>
  download(`/api/certificates/${certNo}/download`)

/* ========== 验真接口 ========== */

/** 公开验真 */
export const publicVerify = (params: VerifyRequest) =>
  get<VerifyResultVO>('/public/verify', params as any)

/** 登录用户验真 */
export const userVerify = (params: VerifyRequest) =>
  get<VerifyResultVO>('/api/verify/detail', params as any)

/** 监管方验真 */
export const regulatorVerify = (params: VerifyRequest) =>
  get<VerifyResultVO>('/api/regulator/verify/detail', params as any)

/** 批量验真 */
export const batchVerify = (data: VerifyRequest[], requestId: string) =>
  post<VerifyResultVO[]>('/api/regulator/verify/batch', data, { params: { requestId } })

/** 验真日志（后端仅支持分页，不支持 querySource/status 过滤） */
export const getVerifyLogs = (params: PageQuery) =>
  get<PageResult<VerifyQueryLogVO>>('/api/admin/verify/logs', params)

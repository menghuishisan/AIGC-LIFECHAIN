/**
 * 认证模块类型定义
 * 与后端 auth 模块 DTO 对齐
 */

/** 管理员创建账户请求（平台管理员/监管员，无需短信验证码） */
export interface CreateAccountRequest {
  mobile: string
  password: string
  nickname: string
  requestId: string
}

/** 注册请求 */
export interface RegisterRequest {
  mobile: string
  password: string
  nickname: string
  accountType?: string
  smsCode: string
  requestId: string
}

/** 登录请求 */
export interface LoginRequest {
  mobile: string
  password: string
}

/** 登录响应 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  accountNo: string
  nickname: string
  accountType: string
  roles: string[]
}

/** 更新资料请求 */
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  avatarUrl?: string
}

/** 提交实名认证请求 */
export interface AuthSubmitRequest {
  subjectType: string
  realName: string
  idCardType: string
  idCardNo: string
  enterpriseCode?: string
  contactName?: string
  contactPhone?: string
  authMaterialUrl?: string
  requestId: string
}

/** 认证审核请求 */
export interface AuthReviewRequest {
  accountNo: string
  reviewResult: string
  reviewComment?: string
  reasonCode?: string
  requestId: string
}

/** 账户冻结请求 */
export interface AccountFreezeRequest {
  accountNo: string
  reason: string
  reasonCode?: string
  requestId: string
}

/** 主体信息 */
export interface SubjectInfoVO {
  subjectNo: string
  subjectType: string
  realName: string
  idCardType: string
  idCardNo: string
}

/** DID 信息 */
export interface DidInfoVO {
  didNo: string
  didValue: string
  status: string
  chainStatus: string
  activeTime?: string
  applyTime?: string
}

/** 账户资料 */
export interface AccountProfileVO {
  accountNo: string
  mobile: string
  nickname: string
  email?: string
  avatarUrl?: string
  accountType: string
  status: string
  authStatus: string
  subjectInfo?: SubjectInfoVO
  didInfo?: DidInfoVO
  roles: string[]
  allowedActions: string[]
}

/** DID 申请请求 */
export interface DidApplyRequest {
  requestId: string
}

/** DID 审核请求 */
export interface DidReviewRequest {
  didNo: string
  reviewResult: string
  reviewComment?: string
  reasonCode?: string
  requestId: string
}

/** DID 操作请求 */
export interface DidOperationRequest {
  didNo: string
  reason: string
  reasonCode?: string
  requestId: string
}

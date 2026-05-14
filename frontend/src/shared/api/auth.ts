/**
 * 认证与账户 API
 * 对接后端 auth 模块所有接口
 */
import { get, post, put } from './http'
import type {
  LoginRequest, LoginResponse, RegisterRequest, CreateAccountRequest,
  AccountProfileVO, UpdateProfileRequest, AuthSubmitRequest,
  AuthReviewRequest, AccountFreezeRequest, DidApplyRequest,
  DidInfoVO, DidReviewRequest, DidOperationRequest, PageResult, PageQuery
} from '@/shared/types'

/* ========== 公开认证接口 ========== */

/** 发送短信验证码 */
export const sendSmsCode = (mobile: string) =>
  post<void>(`/api/auth/sms/send?mobile=${encodeURIComponent(mobile)}`)

/** 用户注册 */
export const register = (data: RegisterRequest) =>
  post<LoginResponse>('/api/auth/register', data)

/** 用户登录 */
export const login = (data: LoginRequest) =>
  post<LoginResponse>('/api/auth/login', data)

/** 刷新访问令牌 */
export const refreshTokenApi = (refreshToken: string) =>
  post<LoginResponse>('/api/auth/refresh', { refreshToken })

/* ========== 账户接口 ========== */

/** 获取当前用户资料 */
export const getProfile = () =>
  get<AccountProfileVO>('/api/account/profile')

/** 更新个人资料 */
export const updateProfile = (data: UpdateProfileRequest) =>
  put<void>('/api/account/profile', data)

/** 提交实名认证 */
export const submitAuth = (data: AuthSubmitRequest) =>
  post<void>('/api/account/auth/submit', data)

/* ========== 管理员账户接口 ========== */

/** 审核实名认证 */
export const reviewAuth = (data: AuthReviewRequest) =>
  post<void>('/api/admin/account/auth/review', data)

/** 冻结账户 */
export const freezeAccount = (data: AccountFreezeRequest) =>
  post<void>('/api/admin/account/freeze', data)

/** 解冻账户 */
export const unfreezeAccount = (data: AccountFreezeRequest) =>
  post<void>('/api/admin/account/unfreeze', data)

/** 管理员查询账户详情 */
export const getAccountDetail = (accountNo: string) =>
  get<AccountProfileVO>(`/api/admin/account/${accountNo}`)

/** 管理员分页查询账户列表 */
export const getAccountList = (params: PageQuery & { status?: string; accountType?: string; keyword?: string }) =>
  get<PageResult<AccountProfileVO>>('/api/admin/account/list', params)

/** 创建平台管理员账户 */
export const createPlatformAccount = (data: CreateAccountRequest) =>
  post<void>('/api/admin/account/create-platform', data)

/** 创建监管账户 */
export const createRegulatorAccount = (data: CreateAccountRequest) =>
  post<void>('/api/admin/account/create-regulator', data)

/* ========== DID 接口 ========== */

/** 申请 DID */
export const applyDid = (data: DidApplyRequest) =>
  post<void>('/api/did/apply', data)

/** 查询 DID 信息 */
export const getDidInfo = (didNo: string) =>
  get<DidInfoVO>(`/api/did/${didNo}`)

/** 管理员 DID 列表 */
export const getDidList = (params: PageQuery & { status?: string; accountNo?: string }) =>
  get<PageResult<DidInfoVO>>('/api/admin/did/list', params)

/** 审核 DID */
export const reviewDid = (data: DidReviewRequest) =>
  post<void>('/api/admin/did/review', data)

/** 挂起 DID */
export const suspendDid = (data: DidOperationRequest) =>
  post<void>('/api/admin/did/suspend', data)

/** 吊销 DID */
export const revokeDid = (data: DidOperationRequest) =>
  post<void>('/api/admin/did/revoke', data)

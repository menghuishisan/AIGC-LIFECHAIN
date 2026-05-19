/**
 * 用户状态管理
 * 单一职责：登录态、用户资料、角色集合，以及由角色派生的默认主页能力。
 * 所有"角色 → 默认页 / 是否可进入路由"判断必须从此处读取。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse, AccountProfileVO } from '@/shared/types'
import { authApi } from '@/shared/api'

/** 系统内全部角色（与后端 RoleCodeEnum 对齐） */
export type RoleCode = 'CREATOR' | 'BUYER' | 'PLATFORM_ADMIN' | 'REGULATOR'

/** 角色 → 默认主页（唯一映射，登录、根路径、越权回退都从此读取） */
const ROLE_HOME: Record<RoleCode, string> = {
  PLATFORM_ADMIN: '/admin/dashboard',
  REGULATOR: '/regulator/dashboard',
  CREATOR: '/creator/dashboard',
  BUYER: '/buyer/dashboard'
}

/** 默认主页优先级：管理员 > 监管员 > 创作者 > 购买者 */
const HOME_PRIORITY: RoleCode[] = ['PLATFORM_ADMIN', 'REGULATOR', 'CREATOR', 'BUYER']

export const useUserStore = defineStore('user', () => {
  /* ========== 状态 ========== */
  const token = ref<string>(sessionStorage.getItem('access_token') || '')
  const refreshToken = ref<string>(sessionStorage.getItem('refresh_token') || '')
  const profile = ref<AccountProfileVO | null>(null)

  /* ========== 计算属性 ========== */

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 用户角色列表 */
  const roles = computed<RoleCode[]>(() => (profile.value?.roles || []) as RoleCode[])

  /** 是否为创作者 */
  const isCreator = computed(() => roles.value.includes('CREATOR'))

  /** 是否为购买者 */
  const isBuyer = computed(() => roles.value.includes('BUYER'))

  /** 是否为平台管理员 */
  const isAdmin = computed(() => roles.value.includes('PLATFORM_ADMIN'))

  /** 是否为监管员 */
  const isRegulator = computed(() => roles.value.includes('REGULATOR'))

  /** 账户状态 */
  const accountStatus = computed(() => profile.value?.status || '')

  /** DID 状态 */
  const didStatus = computed(() => profile.value?.didInfo?.status || 'DID_NOT_APPLIED')

  /** 认证状态 */
  const authStatus = computed(() => profile.value?.authStatus || '')

  /** 默认主页：登录跳转、根路径分发、越权回退共用 */
  const defaultHome = computed<string>(
    () => ROLE_HOME[HOME_PRIORITY.find(r => roles.value.includes(r))!]
  )

  /* ========== 方法 ========== */

  /** 设置登录信息 */
  function setLogin(data: LoginResponse) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    sessionStorage.setItem('access_token', data.accessToken)
    sessionStorage.setItem('refresh_token', data.refreshToken)
  }

  /** 加载用户资料 */
  async function loadProfile() {
    const res = await authApi.getProfile()
    profile.value = res.data
  }

  /** 退出登录 */
  function logout() {
    token.value = ''
    refreshToken.value = ''
    profile.value = null
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('refresh_token')
  }

  /**
   * 判断当前账号是否拥有指定角色之一。
   * 路由 meta.roles 通过此方法做唯一判定。
   */
  function hasAnyRole(required: RoleCode[]): boolean {
    return required.some(r => roles.value.includes(r))
  }

  return {
    token,
    refreshToken,
    profile,
    isLoggedIn,
    roles,
    isCreator,
    isBuyer,
    isAdmin,
    isRegulator,
    accountStatus,
    didStatus,
    authStatus,
    defaultHome,
    setLogin,
    loadProfile,
    logout,
    hasAnyRole
  }
})

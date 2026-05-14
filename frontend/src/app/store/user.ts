/**
 * 用户状态管理
 * 管理登录态、用户资料、角色权限
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse, AccountProfileVO } from '@/shared/types'
import { authApi } from '@/shared/api'

export const useUserStore = defineStore('user', () => {
  /* ========== 状态 ========== */
  const token = ref<string>(sessionStorage.getItem('access_token') || '')
  const refreshToken = ref<string>(sessionStorage.getItem('refresh_token') || '')
  const profile = ref<AccountProfileVO | null>(null)

  /* ========== 计算属性 ========== */

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 用户角色列表 */
  const roles = computed(() => profile.value?.roles || [])

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

  /* ========== 方法 ========== */

  /** 设置登录信息 */
  function setLogin(data: LoginResponse) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    sessionStorage.setItem('access_token', data.accessToken)
    sessionStorage.setItem('refresh_token', data.refreshToken)
  }

  /** 用 refreshToken 续期，返回新 accessToken，失败返回 null */
  async function tryRefreshToken(): Promise<string | null> {
    const rt = refreshToken.value
    if (!rt) return null
    try {
      const res = await authApi.refreshTokenApi(rt)
      setLogin(res.data)
      return res.data.accessToken
    } catch {
      logout()
      return null
    }
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
    setLogin,
    tryRefreshToken,
    loadProfile,
    logout
  }
})

/** HTTP 客户端：统一认证、单次续期、错误提示和请求返回类型。 */
import axios from 'axios'
import type {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosResponse
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, LoginResponse } from '@/shared/types'
import { ErrorCodeMap } from '@/shared/constants'
import router from '@/app/router'
import { useUserStore } from '@/app/store/user'
import { RequestError } from './RequestError'

type AuthRequest = InternalAxiosRequestConfig & { authRetried?: boolean; authSession?: number }
const http: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})
let refreshPromise: Promise<string> | null = null

/** 认证入口和公开验真不携带旧访问令牌，避免失效登录态阻断公开功能。 */
function isPublicRequest(url = '') {
  return (
    /^\/api\/auth\/(?:login|register|refresh|sms\/send)(?:[/?]|$)/.test(url) ||
    url.startsWith('/public/') ||
    url.startsWith('/api/public/')
  )
}

function goLogin(expectedToken?: string) {
  const user = useUserStore()
  if (expectedToken !== undefined && user.token !== expectedToken) return
  const hadToken = user.isLoggedIn
  user.clearSession()
  if (hadToken) ElMessage.warning('登录已过期，请重新登录')
  if (router.currentRoute.value.meta.public !== true) {
    void router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

/** 独立请求不进入本客户端的拦截器；所有等待者共用成功或失败结果。 */
function renewToken(): Promise<string> {
  if (!refreshPromise) {
    const user = useUserStore()
    const refreshToken = user.refreshToken
    const accessToken = user.token
    refreshPromise = (async () => {
      if (!refreshToken) throw new RequestError('登录已失效')
      const response = await axios.post<ApiResponse<LoginResponse>>(
        '/api/auth/refresh',
        { refreshToken },
        { timeout: 15000 }
      )
      const data = response.data
      if (!data.success || !data.data?.accessToken)
        throw new RequestError(data.message || '登录续期失败')
      // 续期过程中若用户已退出或改用其他账户，不恢复旧账户。
      if (user.refreshToken !== refreshToken) throw new RequestError('登录状态已变更')
      user.setLogin(data.data, true)
      return data.data.accessToken
    })()
      .catch((error) => {
        goLogin(accessToken)
        throw error
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

/** 退出登录必须在轮换完成后撤销最新令牌；续期失败时登录态已由统一处理清理。 */
export async function waitForTokenRenewal() {
  await refreshPromise?.catch(() => undefined)
}

async function retryAuthentication(config: AuthRequest): Promise<AxiosResponse> {
  const sentToken = String(config.headers.Authorization || '').replace(/^Bearer /, '')
  const user = useUserStore()
  if (config.url === '/api/auth/logout' || config.authSession !== user.sessionVersion) {
    throw new RequestError('登录状态已变更')
  }
  if (config.authRetried || isPublicRequest(config.url)) {
    goLogin(sentToken)
    throw new RequestError('登录认证失败')
  }
  config.authRetried = true
  // 同一会话的较慢旧请求在续期结束后才返回时，直接使用已更新的令牌重试。
  const token = sentToken !== user.token ? user.token : await renewToken()
  config.headers.Authorization = 'Bearer ' + token
  return http.request(config)
}

http.interceptors.request.use((config) => {
  const user = useUserStore()
  const token = user.token
  ;(config as AuthRequest).authSession ??= user.sessionVersion
  if (token && !isPublicRequest(config.url)) config.headers.Authorization = 'Bearer ' + token
  return config
})

http.interceptors.response.use(
  async (response) => {
    // 文件下载接口也可能返回标准 JSON 错误，先还原后走相同的认证和错误处理。
    if (response.data instanceof Blob && response.data.type.includes('json')) {
      response.data = JSON.parse(await response.data.text())
    }
    const data = response.data as ApiResponse<unknown>
    if (data?.code === undefined || data.success) return response
    if (data.code === '100007') {
      goLogin(String(response.config.headers.Authorization || '').replace(/^Bearer /, ''))
      throw new RequestError(data.message || '登录已失效')
    }
    if (['100001', '100002'].includes(data.code) && !isPublicRequest(response.config.url)) {
      return retryAuthentication(response.config as AuthRequest)
    }
    const message = ErrorCodeMap[data.code] || data.message || '操作失败'
    ElMessage.error(message)
    throw new RequestError(message)
  },
  async (error) => {
    if (error.response?.data instanceof Blob && error.response.data.type.includes('json')) {
      error.response.data = JSON.parse(await error.response.data.text())
    }
    const status = error.response?.status
    const config = error.config as AuthRequest | undefined
    if (error.response?.data?.code === '100007') {
      goLogin(String(config?.headers.Authorization || '').replace(/^Bearer /, ''))
      throw new RequestError('登录已失效')
    }
    if (status === 401 && config && !isPublicRequest(config.url)) return retryAuthentication(config)
    const message =
      error.response?.data?.message ||
      (status === 403
        ? '无权访问该资源'
        : status === 404
          ? '请求资源不存在'
          : status >= 500
            ? '服务暂时不可用，请稍后重试'
            : error.code === 'ECONNABORTED'
              ? '请求超时，请检查网络'
              : '请求失败，请检查输入或网络连接')
    ElMessage.error(message)
    throw new RequestError(message)
  }
)

/**
 * 封装 GET 请求
 */
export function get<T = unknown>(
  url: string,
  params?: object,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return http.get(url, { params, ...config }).then((res) => res.data)
}

/**
 * 封装 POST 请求
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return http.post(url, data, config).then((res) => res.data)
}

/**
 * 封装 PUT 请求
 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return http.put(url, data, config).then((res) => res.data)
}

/**
 * 封装 DELETE 请求
 */
export function del<T = unknown>(
  url: string,
  params?: object,
  config?: AxiosRequestConfig
): Promise<ApiResponse<T>> {
  return http.delete(url, { params, ...config }).then((res) => res.data)
}

/**
 * 下载文件（返回 Blob）
 */
export function download(url: string, params?: object): Promise<Blob> {
  return http.get(url, { params, responseType: 'blob' }).then((res) => res.data)
}

export default http

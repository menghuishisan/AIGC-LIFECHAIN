/**
 * HTTP 请求客户端
 * 统一封装 axios，处理 token 注入、响应拦截、错误码处理
 */
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/shared/types'
import { ErrorCodeMap } from '@/shared/constants'
import router from '@/app/router'
import { useUserStore } from '@/app/store/user'

/** 创建 axios 实例 */
const http: AxiosInstance = axios.create({
  baseURL: '',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

/** 是否正在刷新 token（防止并发请求同时触发多次刷新） */
let isRefreshing = false
/** 等待刷新期间挂起的请求队列 */
let pendingQueue: Array<(token: string) => void> = []

function subscribePending(cb: (token: string) => void) {
  pendingQueue.push(cb)
}

function releasePending(token: string) {
  pendingQueue.forEach(cb => cb(token))
  pendingQueue = []
}

/** 跳转登录并清空登录态（store + sessionStorage 同步清空） */
function goLogin(msg = '登录已过期，请重新登录') {
  useUserStore().logout()
  ElMessage.warning(msg)
  router.push('/login')
}

/** 请求拦截器：注入 JWT token */
http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useUserStore().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应拦截器：统一处理业务错误码 */
http.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse
    /* 如果不是标准 ApiResponse（如文件下载），直接返回 */
    if (data.code === undefined) {
      return response
    }
    /* 业务成功 */
    if (data.success) {
      return response
    }
    /* 未登录或令牌失效 — 尝试用 refreshToken 续期 */
    if (data.code === '100001' || data.code === '100002') {
      const originalRequest = response.config
      const refreshToken = useUserStore().refreshToken
      if (!refreshToken) {
        goLogin()
        return Promise.reject(new Error(data.message))
      }
      if (isRefreshing) {
        /* 已在刷新中，挂起当前请求 */
        return new Promise((resolve) => {
          subscribePending((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(http(originalRequest))
          })
        })
      }
      isRefreshing = true
      return http
        .post<any, any>('/api/auth/refresh', { refreshToken })
        .then((res) => {
          const respData = res.data
          if (respData.success) {
            const newToken: string = respData.data.accessToken
            useUserStore().setLogin(respData.data)
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            releasePending(newToken)
            return http(originalRequest)
          } else {
            goLogin()
            return Promise.reject(new Error('刷新令牌失败'))
          }
        })
        .catch(() => {
          goLogin()
          return Promise.reject(new Error('刷新令牌失败'))
        })
        .finally(() => {
          isRefreshing = false
        })
    }
    /* 权限不足 */
    if (data.code === '100003') {
      ElMessage.error('权限不足')
      return Promise.reject(new Error(data.message))
    }
    /* 其他业务错误，使用错误码映射获取中文提示 */
    const errorMsg = ErrorCodeMap[data.code] || data.message || '操作失败'
    ElMessage.error(errorMsg)
    return Promise.reject(new Error(errorMsg))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        goLogin()
      } else if (status === 403) {
        ElMessage.error('权限不足')
      } else if (status === 404) {
        ElMessage.error('请求资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器异常，请稍后重试')
      }
    } else if (error.message?.includes('timeout')) {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

/**
 * 封装 GET 请求
 */
export function get<T = any>(url: string, params?: Record<string, any>, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return http.get(url, { params, ...config }).then(res => res.data)
}

/**
 * 封装 POST 请求
 */
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return http.post(url, data, config).then(res => res.data)
}

/**
 * 封装 PUT 请求
 */
export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return http.put(url, data, config).then(res => res.data)
}

/**
 * 封装 DELETE 请求
 */
export function del<T = any>(url: string, params?: Record<string, any>, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return http.delete(url, { params, ...config }).then(res => res.data)
}

/**
 * 下载文件（返回 Blob）
 */
export function download(url: string, params?: Record<string, any>): Promise<Blob> {
  return http.get(url, { params, responseType: 'blob' }).then(res => res.data)
}

export default http

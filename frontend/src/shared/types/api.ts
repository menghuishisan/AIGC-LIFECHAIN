/**
 * 统一API响应结构
 * 与后端 ApiResponse<T> 对齐
 */
export interface ApiResponse<T = any> {
  /** 业务码，000000 表示成功 */
  code: string
  /** 提示消息 */
  message: string
  /** 是否成功 */
  success: boolean
  /** 链路追踪ID */
  traceId?: string
  /** 幂等请求ID */
  requestId?: string
  /** 响应数据 */
  data: T
  /** 失败时的原因码 */
  reasonCode?: string
  /** 失败时的当前状态 */
  currentStatus?: string
  /** 失败时允许的操作列表 */
  allowedActions?: string[]
  /** 字段级错误信息 */
  fieldErrors?: Record<string, string>
}

/**
 * 统一分页结果
 * 与后端 PageResult<T> 对齐
 */
export interface PageResult<T = any> {
  /** 数据记录列表 */
  records: T[]
  /** 总记录数 */
  total: number
  /** 当前页码（从1开始） */
  pageNo: number
  /** 每页大小 */
  pageSize: number
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  /** 页码（从1开始） */
  pageNo?: number
  /** 每页大小 */
  pageSize?: number
}

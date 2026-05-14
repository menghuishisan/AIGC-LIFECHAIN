/**
 * 格式化工具函数
 */
import dayjs from 'dayjs'
import utc from 'dayjs/plugin/utc'

dayjs.extend(utc)

/**
 * 格式化金额（分转元，保留两位小数）
 * @param amount 金额（分）
 * @returns 格式化后的金额字符串
 */
export function formatAmount(amount?: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return (amount / 100).toFixed(2)
}

/**
 * 格式化金额带人民币符号
 * @param amount 金额（分）
 * @returns 格式化后的金额字符串
 */
export function formatCurrency(amount?: number): string {
  return `¥${formatAmount(amount)}`
}

/**
 * 格式化 ISO 8601 时间为本地展示格式
 * @param time ISO 8601 时间字符串
 * @param format 格式化模板
 * @returns 格式化后的时间字符串
 */
export function formatTime(time?: string, format = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!time) return '-'
  return dayjs.utc(time).local().format(format)
}

/**
 * 格式化日期（不含时间）
 */
export function formatDate(time?: string): string {
  return formatTime(time, 'YYYY-MM-DD')
}

/**
 * 脱敏手机号
 */
export function maskMobile(mobile?: string): string {
  if (!mobile || mobile.length < 7) return mobile || ''
  return mobile.replace(/(\d{3})\d{4}(\d+)/, '$1****$2')
}

/**
 * 截断文本
 */
export function truncateText(text?: string, maxLength = 50): string {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

/**
 * 生成唯一请求ID（使用 crypto.randomUUID 保证全局唯一性）
 */
export function generateRequestId(): string {
  return crypto.randomUUID()
}

/**
 * 复制文本到剪贴板
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

/** 付款上下文只定位本人的订单，支付结果始终由后端确认。 */
const PENDING_ORDER_KEY = 'alipay_pending_order'
const PAID_ORDER_STATES = ['PAY_CONFIRMED', 'AUTH_GRANTING', 'AUTH_GRANTED', 'SETTLEMENT_PENDING', 'ORDER_COMPLETED']

export function isPaymentConfirmed(status: string): boolean {
  return PAID_ORDER_STATES.includes(status)
}

export function readPendingPayment(): string {
  try {
    const value = JSON.parse(sessionStorage.getItem(PENDING_ORDER_KEY) || 'null')
    if (typeof value?.orderNo === 'string' && /^[A-Za-z0-9_-]{1,64}$/.test(value.orderNo)
      && Number.isFinite(value.createdAt) && Date.now() - value.createdAt >= 0
      && Date.now() - value.createdAt < 2 * 60 * 60 * 1000) return value.orderNo
  } catch { /* 无有效上下文时展示中性结果页。 */ }
  return ''
}

/** 重建官方 POST 表单，只复制隐藏字段，不执行返回 HTML 的脚本或事件。 */
export function submitAlipayForm(formHtml: string, orderNo: string): void {
  const parsed = new DOMParser().parseFromString(formHtml, 'text/html')
  const forms = parsed.querySelectorAll('form')
  if (forms.length !== 1) throw new Error('支付表单无效，请重新发起支付')
  const source = forms[0]!
  const action = new URL(source.getAttribute('action') || '')
  if (source.getAttribute('method')?.toLowerCase() !== 'post'
    || action.protocol !== 'https:' || action.username || action.password || action.port || action.hash
    || !['openapi.alipay.com', 'openapi-sandbox.dl.alipaydev.com'].includes(action.hostname)
    || action.pathname !== '/gateway.do') throw new Error('支付表单地址无效')
  const form = document.createElement('form')
  form.method = 'POST'
  form.action = action.href
  form.acceptCharset = 'UTF-8'
  form.hidden = true
  const names = new Set<string>()
  for (const input of source.querySelectorAll('input')) {
    if (input.type !== 'hidden') continue
    if (!input.name || names.has(input.name)) throw new Error('支付表单字段无效')
    names.add(input.name)
    const field = document.createElement('input')
    field.type = 'hidden'
    field.name = input.name
    field.value = input.value
    form.appendChild(field)
  }
  if (!names.size) throw new Error('支付表单缺少字段')
  sessionStorage.setItem(PENDING_ORDER_KEY, JSON.stringify({ orderNo, createdAt: Date.now() }))
  document.body.appendChild(form)
  try { HTMLFormElement.prototype.submit.call(form) }
  finally { form.remove() }
}

/** 人民币元只在输入边界转换；通过十进制文本拆分，禁止浮点乘100和静默四舍五入。 */
export const MAX_AMOUNT_FEN = 10_000_000_000
export const MAX_AMOUNT_YUAN = MAX_AMOUNT_FEN / 100

export function yuanToFen(value: number | string): number {
  const text = String(value)
  if (!/^\d+(?:\.\d{1,2})?$/.test(text)) throw new Error('金额须为至多两位小数的人民币元')
  const [whole = '0', fraction = ''] = text.split('.')
  const fen = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  if (!Number.isSafeInteger(fen) || fen < 1 || fen > MAX_AMOUNT_FEN)
    throw new Error('金额超出允许范围')
  return fen
}

/** 请求层已向用户展示的业务或网络错误，供页面和事件边界识别。 */
export class RequestError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'RequestError'
  }
}

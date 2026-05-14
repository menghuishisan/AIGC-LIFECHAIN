/**
 * 交易模块类型定义
 * 与后端 trade 模块 DTO 对齐
 */

/** 创建上架请求 */
export interface CreateListingRequest {
  workNo?: string
  licenseTemplateCode?: string
  licenseType: string
  priceAmount: number
  scopeDescription?: string
  durationDays?: number
  requestId: string
}

/** 上架审核请求 */
export interface ListingReviewRequest {
  listingNo: string
  reviewResult: string
  reviewComment?: string
  reasonCode?: string
  requestId: string
}

/** 上架详情 */
export interface ListingDetailVO {
  listingNo: string
  workNo: string
  workTitle: string
  coverUrl?: string
  licenseType: string
  priceAmount: number
  currency: string
  scopeDescription?: string
  durationDays?: number
  status: string
  listTime?: string
  creatorAccountNo: string
}

/** 创建订单请求 */
export interface CreateOrderRequest {
  listingNo: string
  payChannel: string
  requestId: string
}

/** 订单基础信息 */
export interface OrderBasicInfo {
  orderNo: string
  workTitle: string
  licenseType: string
  priceAmount: number
  payAmount: number
  currency: string
  payChannel: string
}

/** 订单状态信息 */
export interface OrderStatusInfo {
  orderStatus: string
  payStatus: string
}

/** 订单时间信息 */
export interface OrderTimeInfo {
  createdAt?: string
  expireTime?: string
  payTime?: string
  completeTime?: string
}

/** 订单关联信息 */
export interface OrderRelationInfo {
  workNo: string
  listingNo: string
  buyerAccountNo: string
  creatorAccountNo: string
  licenseNo?: string
  settleNo?: string
}

/** 订单详情 */
export interface OrderDetailVO {
  basicInfo: OrderBasicInfo
  statusInfo: OrderStatusInfo
  timeInfo: OrderTimeInfo
  relationInfo: OrderRelationInfo
  chainInfo?: import('./work').ChainInfo
  allowedActions: string[]
}

/** 订单列表项 */
export interface OrderListVO {
  orderNo: string
  workTitle: string
  orderStatus: string
  payAmount: number
  payChannel: string
  createdAt: string
}

/** 管理员订单列表项 */
export interface AdminOrderListVO extends OrderListVO {
  buyerAccountNo: string
  creatorAccountNo: string
}

/** 管理员订单查询参数 */
export interface AdminOrderQuery {
  orderStatus?: string
  payChannel?: string
  buyerAccountNo?: string
  creatorAccountNo?: string
}

/** 支付请求 */
export interface PayRequest {
  orderNo?: string
  payChannel: string
  clientIp?: string
  requestId: string
}

/** 支付结果 */
export interface PayResultVO {
  orderNo: string
  prepayId?: string
  payParams?: Record<string, string>
  payUrl?: string
}

/** 退款申请请求 */
export interface RefundApplyRequest {
  orderNo?: string
  reason: string
  requestId: string
}

/** 退款处理请求 */
export interface RefundProcessRequest {
  refundNo: string
  action: string
  reviewComment?: string
  reasonCode?: string
  requestId: string
}

/** 退款详情 */
export interface RefundDetailVO {
  refundNo: string
  orderNo: string
  paymentNo?: string
  payChannel: string
  refundAmount: number
  currency: string
  refundStatus: string
  refundReason: string
  thirdRefundNo?: string
  applyTime?: string
  completeTime?: string
  failReason?: string
}

/** 创建授权模板请求 */
export interface CreateLicenseTemplateRequest {
  templateName: string
  licenseType: string
  scopeDescription: string
  durationDays?: number
  priceAmount?: number
  description?: string
  requestId: string
}

/** 授权模板 */
export interface LicenseTemplateVO {
  templateName: string
  templateCode: string
  licenseType: string
  scopeDescription: string
  durationDays?: number
  priceAmount?: number
  currency: string
  status: string
  description?: string
  createdAt: string
}

/** 授权基础信息 */
export interface LicenseBasicInfo {
  licenseNo: string
  licenseType: string
  scopeDescription?: string
}

/** 授权状态信息 */
export interface LicenseStatusInfo {
  licenseStatus: string
}

/** 授权时间信息 */
export interface LicenseTimeInfo {
  effectiveTime?: string
  expireTime?: string
}

/** 授权详情 */
export interface LicenseDetailVO {
  basicInfo: LicenseBasicInfo
  statusInfo: LicenseStatusInfo
  timeInfo: LicenseTimeInfo
  relationInfo: { orderNo: string; workNo: string }
  chainInfo?: import('./work').ChainInfo & { licenseHash?: string }
  allowedActions: string[]
}

/**
 * 交易模块 API
 * 对接后端 trade 模块所有接口
 */
import { get, post, del } from './http'
import type {
  CreateListingRequest, ListingReviewRequest, ListingDetailVO,
  CreateOrderRequest, OrderDetailVO, OrderListVO, AdminOrderListVO, AdminOrderQuery,
  PayRequest, PayResultVO, RefundApplyRequest, RefundProcessRequest, RefundDetailVO,
  CreateLicenseTemplateRequest, LicenseTemplateVO, LicenseDetailVO,
  PageResult, PageQuery
} from '@/shared/types'

/* ========== 上架接口 ========== */

/** 创建上架申请 */
export const createListing = (workNo: string, data: CreateListingRequest) =>
  post<ListingDetailVO>(`/api/works/${workNo}/listing`, data)

/** 管理员审核上架 */
export const reviewListing = (data: ListingReviewRequest) =>
  post<void>('/api/admin/listings/review', data)

/** 上架详情 */
export const getListingDetail = (listingNo: string) =>
  get<ListingDetailVO>(`/api/listings/${listingNo}`)

/** 我的上架列表 */
export const getMyListings = (params: PageQuery & { status?: string }) =>
  get<PageResult<ListingDetailVO>>('/api/listings/mine', params)

/** 管理员上架列表（仅支持 reviewStatus 过滤） */
export const getAdminListings = (params: PageQuery & { reviewStatus?: string }) =>
  get<PageResult<ListingDetailVO>>('/api/admin/listings', params)

/** 下架作品 */
export const removeListing = (listingNo: string, requestId: string) =>
  del<void>(`/api/listings/${listingNo}`, { requestId })

/* ========== 订单接口 ========== */

/** 创建订单 */
export const createOrder = (data: CreateOrderRequest) =>
  post<OrderDetailVO>('/api/orders', data)

/** 订单详情 */
export const getOrderDetail = (orderNo: string) =>
  get<OrderDetailVO>(`/api/orders/${orderNo}`)

/** 我的订单列表 */
export const getMyOrders = (params: PageQuery & { role?: string; status?: string }) =>
  get<PageResult<OrderListVO>>('/api/orders', params)

/** 管理员全量订单列表 */
export const getAdminOrders = (params: PageQuery & AdminOrderQuery) =>
  get<PageResult<AdminOrderListVO>>('/api/admin/orders', params)

/** 发起支付 */
export const payOrder = (orderNo: string, data: PayRequest) =>
  post<PayResultVO>(`/api/orders/${orderNo}/pay`, data)

/** 取消订单 */
export const cancelOrder = (orderNo: string, requestId: string) =>
  del<void>(`/api/orders/${orderNo}`, { requestId })

/** 申请退款 */
export const applyRefund = (orderNo: string, data: RefundApplyRequest) =>
  post<void>(`/api/orders/${orderNo}/refund`, data)

/* ========== 退款接口 ========== */

/** 退款列表 */
export const getRefundList = (params: PageQuery & { status?: string }) =>
  get<PageResult<RefundDetailVO>>('/api/admin/refunds', params)

/** 退款详情 */
export const getRefundDetail = (refundNo: string) =>
  get<RefundDetailVO>(`/api/admin/refunds/${refundNo}`)

/** 处理退款 */
export const processRefund = (data: RefundProcessRequest) =>
  post<void>('/api/admin/refunds/process', data)

/* ========== 授权模板接口 ========== */

/** 创建授权模板 */
export const createLicenseTemplate = (data: CreateLicenseTemplateRequest) =>
  post<LicenseTemplateVO>('/api/licenses/templates', data)

/** 授权模板列表 */
export const getLicenseTemplates = (params: PageQuery) =>
  get<PageResult<LicenseTemplateVO>>('/api/licenses/templates', params)

/** 授权模板详情 */
export const getLicenseTemplateDetail = (templateCode: string) =>
  get<LicenseTemplateVO>(`/api/licenses/templates/${templateCode}`)

/* ========== 授权接口 ========== */

/** 授权详情 */
export const getLicenseDetail = (licenseNo: string) =>
  get<LicenseDetailVO>(`/api/licenses/${licenseNo}`)

/** 我的授权列表 */
export const getMyLicenses = (params: PageQuery) =>
  get<PageResult<LicenseDetailVO>>('/api/licenses/mine', params)

/**
 * 路由配置
 * 按 portal / admin / regulator / verify 四个 app 分组
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupGuards } from '@/app/guards/authGuard'
import { useUserStore } from '@/app/store/user'

/* ========== 布局组件 ========== */
import PortalLayout from '@/app/layouts/PortalLayout.vue'
import AdminLayout from '@/app/layouts/AdminLayout.vue'
import RegulatorLayout from '@/app/layouts/RegulatorLayout.vue'
import VerifyLayout from '@/app/layouts/VerifyLayout.vue'

/* ========== Portal 路由 ========== */
const portalRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: PortalLayout,
    redirect: () => {
      try {
        const userStore = useUserStore()
        if (userStore.isAdmin) return '/admin/dashboard'
        if (userStore.isRegulator) return '/regulator/dashboard'
      } catch { /* pinia not ready yet */ }
      return '/creator/dashboard'
    },
    children: [
      /* 创作者工作台 */
      { path: 'creator/dashboard', component: () => import('@/apps/portal/pages/CreatorDashboard.vue') },
      /* 我的作品 */
      { path: 'creator/works', component: () => import('@/apps/portal/pages/WorkList.vue') },
      /* 上传作品 */
      { path: 'creator/works/create', component: () => import('@/apps/portal/pages/WorkCreate.vue') },
      /* 作品详情 */
      { path: 'creator/works/:workNo', component: () => import('@/apps/portal/pages/WorkDetail.vue'), props: true },
      /* 我的确权 */
      { path: 'creator/claims', component: () => import('@/apps/portal/pages/ClaimList.vue') },
      /* 确权详情 */
      { path: 'creator/claims/:claimNo', component: () => import('@/apps/portal/pages/ClaimDetail.vue'), props: true },
      /* 我的上架 */
      { path: 'creator/listings', component: () => import('@/apps/portal/pages/ListingList.vue') },
      /* 上架申请 */
      { path: 'creator/listings/create', component: () => import('@/apps/portal/pages/ListingCreate.vue') },
      /* 授权模板列表 */
      { path: 'creator/license-templates', component: () => import('@/apps/portal/pages/LicenseTemplateList.vue') },
      /* 授权模板详情 */
      { path: 'creator/license-templates/:templateCode', component: () => import('@/apps/portal/pages/LicenseTemplateDetail.vue'), props: true },
      /* 新建授权模板 */
      { path: 'creator/license-templates/create', component: () => import('@/apps/portal/pages/LicenseTemplateCreate.vue') },
      /* 创作者订单 */
      { path: 'creator/orders', component: () => import('@/apps/portal/pages/CreatorOrders.vue') },
      /* 收益管理 */
      { path: 'creator/income', component: () => import('@/apps/portal/pages/IncomePage.vue') },
      /* 证书详情 */
      { path: 'creator/certificates/:certNo', component: () => import('@/apps/portal/pages/CertDetail.vue'), props: true },
      /* 购买者工作台 */
      { path: 'buyer/dashboard', component: () => import('@/apps/portal/pages/BuyerDashboard.vue') },
      /* 买家订单详情 */
      { path: 'buyer/orders/:orderNo', component: () => import('@/apps/portal/pages/OrderDetail.vue'), props: true },
      /* 我的授权 */
      { path: 'buyer/licenses', component: () => import('@/apps/portal/pages/LicenseList.vue') },
      /* 授权详情 */
      { path: 'buyer/licenses/:licenseNo', component: () => import('@/apps/portal/pages/LicenseDetail.vue'), props: true },
      /* 内容市场 */
      { path: 'market', component: () => import('@/apps/portal/pages/MarketList.vue') },
      /* 市场作品详情 */
      { path: 'market/works/:workNo', component: () => import('@/apps/portal/pages/MarketDetail.vue'), props: true },
      /* 下单确认 */
      { path: 'orders/create', component: () => import('@/apps/portal/pages/OrderCreate.vue') },
      /* 支付 */
      { path: 'orders/:orderNo/pay', component: () => import('@/apps/portal/pages/PayPage.vue'), props: true },
      /* 支付结果页 */
      { path: 'payment/success', component: () => import('@/apps/portal/pages/PaymentSuccess.vue') },
      { path: 'payment/cancel', component: () => import('@/apps/portal/pages/PaymentCancel.vue') },
      /* 个人中心 */
      { path: 'profile', component: () => import('@/apps/portal/pages/Profile.vue') },
      /* 实名认证 */
      { path: 'profile/auth', component: () => import('@/apps/portal/pages/AuthSubmit.vue') },
      /* DID 申请 */
      { path: 'profile/did/apply', component: () => import('@/apps/portal/pages/DidApply.vue') },
      /* 通知列表 */
      { path: 'notices', component: () => import('@/apps/portal/pages/NoticeList.vue') },
      /* 通知详情 */
      { path: 'notices/:noticeNo', component: () => import('@/apps/portal/pages/NoticeDetail.vue'), props: true },
    ]
  }
]

/* ========== Admin 路由 ========== */
const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/apps/admin/pages/AdminDashboard.vue') },
      { path: 'accounts', component: () => import('@/apps/admin/pages/AccountList.vue') },
      { path: 'accounts/:accountNo', component: () => import('@/apps/admin/pages/AccountDetail.vue'), props: true },
      { path: 'dids', component: () => import('@/apps/admin/pages/DidList.vue') },
      { path: 'dids/:didNo', component: () => import('@/apps/admin/pages/DidDetail.vue'), props: true },
      { path: 'claims', component: () => import('@/apps/admin/pages/ClaimReviewList.vue') },
      { path: 'claims/:claimNo', component: () => import('@/apps/admin/pages/ClaimReview.vue'), props: true },
      { path: 'certificates', component: () => import('@/apps/admin/pages/CertManage.vue') },
      { path: 'certificates/generate', component: () => import('@/apps/admin/pages/CertGenerate.vue') },
      { path: 'verify-logs', component: () => import('@/apps/admin/pages/VerifyLogs.vue') },
      { path: 'listings', component: () => import('@/apps/admin/pages/ListingReviewList.vue') },
      { path: 'listings/:listingNo', component: () => import('@/apps/admin/pages/ListingReview.vue'), props: true },
      { path: 'orders', component: () => import('@/apps/admin/pages/AdminOrderList.vue') },
      { path: 'orders/:orderNo', component: () => import('@/apps/admin/pages/AdminOrderDetail.vue'), props: true },
      { path: 'refunds', component: () => import('@/apps/admin/pages/RefundList.vue') },
      { path: 'refunds/:refundNo', component: () => import('@/apps/admin/pages/RefundProcess.vue'), props: true },
      { path: 'disputes', component: () => import('@/apps/admin/pages/DisputeList.vue') },
      { path: 'disputes/:caseNo', component: () => import('@/apps/admin/pages/DisputeProcess.vue'), props: true },
      { path: 'settlements', component: () => import('@/apps/admin/pages/SettlementList.vue') },
      { path: 'settlements/:settleNo', component: () => import('@/apps/admin/pages/SettlementDetail.vue'), props: true },
      { path: 'settle-templates', component: () => import('@/apps/admin/pages/SettleTemplateList.vue') },
      { path: 'settle-templates/create', component: () => import('@/apps/admin/pages/SettleTemplateCreate.vue') },
      { path: 'screen', component: () => import('@/apps/admin/pages/AdminScreen.vue') },
      { path: 'audit-logs', component: () => import('@/apps/admin/pages/AuditLogs.vue') },
      { path: 'status-history', component: () => import('@/apps/admin/pages/StatusHistory.vue') },
      { path: 'chain-receipts', component: () => import('@/apps/admin/pages/ChainReceipts.vue') },
      { path: 'configs', component: () => import('@/apps/admin/pages/SysConfig.vue') },
    ]
  }
]

/* ========== Regulator 路由 ========== */
const regulatorRoutes: RouteRecordRaw[] = [
  {
    path: '/regulator',
    component: RegulatorLayout,
    redirect: '/regulator/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/apps/regulator/pages/RegulatorDashboard.vue') },
      { path: 'search', component: () => import('@/apps/regulator/pages/RegulatorSearch.vue') },
      { path: 'verify', component: () => import('@/apps/regulator/pages/RegulatorVerify.vue') },
      { path: 'risks', component: () => import('@/apps/regulator/pages/RiskList.vue') },
      { path: 'risks/:riskNo', component: () => import('@/apps/regulator/pages/RiskDetail.vue'), props: true },
      { path: 'freezes', component: () => import('@/apps/regulator/pages/FreezeList.vue') },
      { path: 'freezes/:freezeNo', component: () => import('@/apps/regulator/pages/FreezeDetail.vue'), props: true },
      { path: 'disputes', component: () => import('@/apps/regulator/pages/RegulatorDisputeList.vue') },
      { path: 'disputes/:caseNo', component: () => import('@/apps/regulator/pages/RegulatorDisputeDetail.vue'), props: true },
      { path: 'disputes/:caseNo/review', component: () => import('@/apps/regulator/pages/RegulatorDisputeReview.vue'), props: true },
      { path: 'reports', component: () => import('@/apps/regulator/pages/ReportList.vue') },
      { path: 'reports/:reportNo', component: () => import('@/apps/regulator/pages/ReportDetail.vue'), props: true },
      { path: 'reports/create', component: () => import('@/apps/regulator/pages/ReportCreate.vue') },
    ]
  }
]

/* ========== Verify 路由 ========== */
const verifyRoutes: RouteRecordRaw[] = [
  {
    path: '/verify',
    component: VerifyLayout,
    children: [
      { path: '', component: () => import('@/apps/verify/pages/VerifyHome.vue') },
      { path: 'result', component: () => import('@/apps/verify/pages/VerifyResult.vue') },
      { path: 'not-found', component: () => import('@/apps/verify/pages/VerifyNotFound.vue') },
      { path: 'help', component: () => import('@/apps/verify/pages/VerifyHelp.vue') },
    ]
  }
]

/* ========== 公共路由（无布局） ========== */
const publicRoutes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/apps/portal/pages/Login.vue') },
  { path: '/register', component: () => import('@/apps/portal/pages/Register.vue') },
]

/* ========== 创建路由 ========== */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...publicRoutes,
    ...portalRoutes,
    ...adminRoutes,
    ...regulatorRoutes,
    ...verifyRoutes,
    /* 404 */
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

/* 安装路由守卫 */
setupGuards(router)

export default router

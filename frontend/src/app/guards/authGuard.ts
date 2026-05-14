/**
 * 路由守卫
 * 处理登录态校验、角色权限校验
 */
import type { Router } from 'vue-router'
import { useUserStore } from '@/app/store/user'

/** 不需要登录就能访问的路由 */
const PUBLIC_ROUTES = ['/login', '/register', '/verify', '/verify/result', '/verify/not-found', '/verify/help', '/payment/success', '/payment/cancel']

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()

    /* 公开路由直接放行 */
    if (PUBLIC_ROUTES.some(path => to.path === path || to.path.startsWith('/verify'))) {
      return next()
    }

    /* 未登录跳转登录页 */
    if (!userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    /* 如果没有用户资料则加载 */
    if (!userStore.profile) {
      try {
        await userStore.loadProfile()
      } catch {
        userStore.logout()
        return next('/login')
      }
    }

    /* 管理后台页面校验管理员角色 */
    if (to.path.startsWith('/admin') && !userStore.isAdmin) {
      return next('/creator/dashboard')
    }

    /* 监管后台页面校验监管员角色 */
    if (to.path.startsWith('/regulator') && !userStore.isRegulator && !userStore.isAdmin) {
      return next('/creator/dashboard')
    }

    next()
  })
}

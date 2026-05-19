/**
 * 路由守卫
 * 单一规则：
 *   1. meta.public 为 true 的路由直接放行（公开页：登录、注册、verify 端口）。
 *   2. 未登录跳 /login，并附带 redirect 以便登录后回跳。
 *   3. profile 缺失时拉取，拉取失败强制登出。
 *   4. 路由 meta.roles 存在时，按角色白名单判定，不通过统一跳 /login?forbidden=1。
 *   5. 根路径 / 按 user store.defaultHome 唯一映射前往主页。
 *
 * 依赖 vue-router 4 的 meta 浅合并行为：to.meta 自动反映 matched 链上最近一级的 meta。
 */
import type { Router } from 'vue-router'
import { useUserStore, type RoleCode } from '@/app/store/user'

/** 安全的登录后回跳：必须是站内绝对路径，避免 open-redirect */
function safeRedirect(target: unknown): string | null {
  if (typeof target !== 'string') return null
  if (!target.startsWith('/') || target.startsWith('//')) return null
  return target
}

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()

    /* 公开路由直接放行 */
    if (to.meta.public === true) {
      return next()
    }

    /* 未登录跳登录页 */
    if (!userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    /* 加载 profile */
    if (!userStore.profile) {
      try {
        await userStore.loadProfile()
      } catch {
        userStore.logout()
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    /* 根路径按角色分发到默认主页 */
    if (to.path === '/') {
      return next(userStore.defaultHome)
    }

    /* 校验角色白名单 */
    const required = to.meta.roles as RoleCode[] | undefined
    if (required && !userStore.hasAnyRole(required)) {
      return next({ path: '/login', query: { forbidden: '1' } })
    }

    next()
  })
}

export { safeRedirect }

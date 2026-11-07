<template>
  <!-- Admin 管理后台布局：深色侧栏 + 顶部 + 主内容 -->
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside
      ref="navPanel"
      :inert="mobileNavigation && !navOpen"
      :aria-hidden="mobileNavigation && !navOpen ? true : undefined"
      :role="mobileNavigation && navOpen ? 'dialog' : undefined"
      :aria-modal="mobileNavigation && navOpen ? true : undefined"
      aria-label="业务导航"
      :class="['admin-sidebar', { 'is-open': navOpen }]"
    >
      <button class="admin-sidebar__logo" type="button" @click="router.push('/admin/dashboard')">
        <span class="admin-sidebar__logo-icon">◈</span>
        <span class="admin-sidebar__logo-text">LifeChain</span>
        <span class="admin-sidebar__logo-sub">管理后台</span>
      </button>
      <el-menu
        :default-active="route.path"
        router
        class="admin-sidebar__menu"
        @select="navOpen = false"
      >
        <el-menu-item tabindex="0" index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>总览</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/admin/screen">
          <el-icon><DataAnalysis /></el-icon>
          <span>实时数据</span>
        </el-menu-item>
        <el-sub-menu index="/admin/accounts-group">
          <template #title>
            <el-icon><User /></el-icon>
            <span>账户与认证</span>
          </template>
          <el-menu-item tabindex="0" index="/admin/accounts">账户列表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/did-group">
          <template #title>
            <el-icon><Postcard /></el-icon>
            <span>DID管理</span>
          </template>
          <el-menu-item tabindex="0" index="/admin/dids">DID列表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/work-group">
          <template #title>
            <el-icon><Picture /></el-icon>
            <span>作品与确权</span>
          </template>
          <el-menu-item tabindex="0" index="/admin/claims">确权审核</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/certificates">证书管理</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/verify-logs">验真日志</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/trade-group">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>上架与交易</span>
          </template>
          <el-menu-item tabindex="0" index="/admin/listings">上架审核</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/orders">订单管理</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/refunds">退款处理</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/disputes">争议处理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/settlement-group">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>分账与收益</span>
          </template>
          <el-menu-item tabindex="0" index="/admin/settlements">结算记录</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/settle-templates">分账模板</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/audit-group">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>审计与链路</span>
          </template>
          <el-menu-item tabindex="0" index="/admin/audit-logs">审计日志</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/status-history">状态历史</el-menu-item>
          <el-menu-item tabindex="0" index="/admin/chain-receipts">链回执</el-menu-item>
        </el-sub-menu>
        <el-menu-item tabindex="0" index="/admin/configs">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <button
      v-if="navOpen"
      class="admin-scrim"
      type="button"
      aria-label="关闭导航"
      @click="navOpen = false"
    />

    <!-- 右侧区域 -->
    <div class="admin-right">
      <!-- 顶部导航 -->
      <header class="admin-header">
        <el-button
          ref="navToggle"
          class="admin-header__menu-toggle"
          circle
          :aria-label="navOpen ? '关闭导航' : '打开导航'"
          :aria-expanded="navOpen"
          @click="navOpen = !navOpen"
        >
          <el-icon><Close v-if="navOpen" /><Menu v-else /></el-icon>
        </el-button>
        <div class="admin-header__left">
          <el-input
            v-model="searchKeyword"
            placeholder="按审计操作类型查询"
            :prefix-icon="Search"
            size="default"
            style="width: 240px"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="admin-header__right">
          <el-badge :value="alertCount" :hidden="!alertCount" :max="99">
            <el-button
              circle
              size="default"
              aria-label="查看审计日志"
              @click="router.push('/admin/audit-logs')"
            >
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <div class="admin-header__user" aria-label="账户菜单">
              <el-avatar :size="30">{{ userStore.profile?.nickname?.charAt(0) || 'A' }}</el-avatar>
              <span>{{ userStore.profile?.nickname || '管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容 -->
      <main class="admin-main">
        <router-view :key="route.path" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useNavigationDrawer } from '@/shared/composables/useNavigationDrawer'
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Close,
  DataAnalysis,
  Document,
  Menu,
  Odometer,
  Picture,
  Postcard,
  Search,
  Setting,
  ShoppingCart,
  User,
  Wallet
} from '@element-plus/icons-vue'
import { useUserStore } from '@/app/store/user'
import { appApi } from '@/shared/api'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const alertCount = ref(0)
const { navOpen, navPanel, navToggle, mobileNavigation } = useNavigationDrawer()

watch(
  () => route.fullPath,
  () => {
    navOpen.value = false
  }
)

/** 全局搜索跳转到审计日志 */
function handleSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({ path: '/admin/audit-logs', query: { action: searchKeyword.value.trim() } })
}

/** 加载待处理事项数 */
onMounted(async () => {
  try {
    const res = await appApi.getNotices({ pageNo: 1, pageSize: 1, readFlag: false })
    alertCount.value = res.data?.total || 0
  } catch {
    /* 降级 */
  }
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await userStore.logout()
    } catch {
      // 撤销失败已由请求层提示，本地凭证仍须清理。
    } finally {
      await router.push('/login')
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.admin-layout {
  display: flex;
  min-height: 100vh;
  --lc-accent: #925923;
  --lc-accent-strong: #8e592d;
  --lc-accent-soft: #f5ece1;
  --lc-canvas: #f1f2ee;
}

.admin-sidebar {
  width: $admin-sidebar-width;
  background: $admin-sidebar-bg;
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  overflow-y: auto;
  z-index: 100;

  &__logo {
    width: 100%;
    height: $admin-header-height;
    display: flex;
    align-items: center;
    padding: 0 20px;
    cursor: pointer;
    gap: 8px;
    border: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    background: transparent;
    text-align: left;

    &-icon {
      font-size: 22px;
      color: #d49a5c;
    }
    &-text {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
    }
    &-sub {
      font-size: 11px;
      color: #93a9a3;
      margin-left: 4px;
    }
  }

  &__menu {
    --el-menu-bg-color: transparent;
    border-right: none;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      height: 42px;
      line-height: 42px;
      font-size: 13px;
    }

    :deep(.el-menu-item.is-active) {
      background: $admin-sidebar-active !important;
    }
  }
}

.admin-right {
  flex: 1;
  min-width: 0;
  margin-left: $admin-sidebar-width;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.admin-header {
  min-height: $admin-header-height;
  flex: 0 0 $admin-header-height;
  background: $content-white;
  border-bottom: 1px solid $card-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 90;

  &__left {
    display: flex;
    align-items: center;
  }

  &__menu-toggle {
    display: none;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 13px;
    color: $admin-text-primary;
  }
}

.admin-main {
  flex: 1;
  min-width: 0;
  padding: $content-padding;
  overflow-y: auto;
  background: $admin-content-bg;
}

.admin-sidebar__menu {
  background: transparent;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    margin: 2px 8px;
    border-radius: 3px;
    color: #b9c2bd;
  }

  :deep(.el-menu-item.is-active) {
    border-left: 2px solid #d49a5c;
    background: $admin-sidebar-active !important;
    color: #fff !important;
  }
}

.admin-scrim {
  display: none;
}

@media (max-width: 860px) {
  .admin-sidebar {
    z-index: 120;
    width: min(280px, calc(100vw - 48px));
    transform: translateX(-102%);
    transition: transform 180ms ease;

    &.is-open {
      transform: translateX(0);
    }
  }

  .admin-header {
    gap: 10px;
    padding: 0 16px;

    &__menu-toggle {
      display: inline-flex;
      flex: 0 0 auto;
    }
  }

  .admin-scrim {
    position: fixed;
    inset: 0;
    z-index: 110;
    display: block;
    border: 0;
    background: rgba(22, 29, 27, 0.42);
  }

  .admin-right {
    margin-left: 0;
  }

  .admin-main {
    padding: 22px 18px;
  }
}

@media (max-width: 560px) {
  .admin-header {
    padding: 0 10px;

    &__left {
      min-width: 0;
      flex: 1;
    }

    &__left .el-input {
      width: 100% !important;
    }

    &__user span {
      display: none;
    }

    &__right {
      gap: 8px;
    }
  }

  .admin-main {
    padding: 18px 12px;
  }
}
</style>

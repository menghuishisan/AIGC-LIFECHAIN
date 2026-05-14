<template>
  <!-- Admin 管理后台布局：深色侧栏 + 顶部 + 主内容 -->
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="admin-sidebar__logo" @click="router.push('/admin/dashboard')">
        <span class="admin-sidebar__logo-icon">◈</span>
        <span class="admin-sidebar__logo-text">LifeChain</span>
        <span class="admin-sidebar__logo-sub">管理后台</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#15232A"
        text-color="#A0B3BA"
        active-text-color="#FFFFFF"
        class="admin-sidebar__menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>总览</span>
        </el-menu-item>
        <el-menu-item index="/admin/screen">
          <el-icon><DataAnalysis /></el-icon>
          <span>实时数据</span>
        </el-menu-item>
        <el-sub-menu index="/admin/accounts-group">
          <template #title>
            <el-icon><User /></el-icon>
            <span>账户与认证</span>
          </template>
          <el-menu-item index="/admin/accounts">账户列表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/did-group">
          <template #title>
            <el-icon><Postcard /></el-icon>
            <span>DID管理</span>
          </template>
          <el-menu-item index="/admin/dids">DID列表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/work-group">
          <template #title>
            <el-icon><Picture /></el-icon>
            <span>作品与确权</span>
          </template>
          <el-menu-item index="/admin/claims">确权审核</el-menu-item>
          <el-menu-item index="/admin/certificates">证书管理</el-menu-item>
          <el-menu-item index="/admin/verify-logs">验真日志</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/trade-group">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>上架与交易</span>
          </template>
          <el-menu-item index="/admin/listings">上架审核</el-menu-item>
          <el-menu-item index="/admin/orders">订单管理</el-menu-item>
          <el-menu-item index="/admin/refunds">退款处理</el-menu-item>
          <el-menu-item index="/admin/disputes">争议处理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/settlement-group">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>分账与收益</span>
          </template>
          <el-menu-item index="/admin/settlements">结算记录</el-menu-item>
          <el-menu-item index="/admin/settle-templates">分账模板</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/admin/audit-group">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>审计与链路</span>
          </template>
          <el-menu-item index="/admin/audit-logs">审计日志</el-menu-item>
          <el-menu-item index="/admin/status-history">状态历史</el-menu-item>
          <el-menu-item index="/admin/chain-receipts">链回执</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/admin/configs">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 右侧区域 -->
    <div class="admin-right">
      <!-- 顶部导航 -->
      <header class="admin-header">
        <div class="admin-header__left">
          <el-input
            v-model="searchKeyword"
            placeholder="全局搜索..."
            :prefix-icon="Search"
            size="default"
            style="width: 240px"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="admin-header__right">
          <el-badge :value="alertCount" :hidden="!alertCount" :max="99">
            <el-button circle size="default" @click="router.push('/admin/audit-logs')">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <div class="admin-header__user">
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
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  DataAnalysis,
  Document,
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

/** 全局搜索跳转到审计日志 */
function handleSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({ path: '/admin/audit-logs', query: { keyword: searchKeyword.value.trim() } })
}

/** 加载待处理事项数 */
onMounted(async () => {
  try {
    const res = await appApi.getNotices({ pageNo: 1, pageSize: 1, readFlag: false })
    alertCount.value = res.data?.total || 0
  } catch { /* 降级 */ }
})

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.admin-layout {
  display: flex;
  height: 100%;
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
    height: $admin-header-height;
    display: flex;
    align-items: center;
    padding: 0 20px;
    cursor: pointer;
    gap: 8px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);

    &-icon {
      font-size: 22px;
      color: #5CC1C9;
    }
    &-text {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
    }
    &-sub {
      font-size: 11px;
      color: #6B8A94;
      margin-left: 4px;
    }
  }

  &__menu {
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
  margin-left: $admin-sidebar-width;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.admin-header {
  height: $admin-header-height;
  background: $content-white;
  border-bottom: 1px solid #E8ECEF;
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
  padding: $content-padding;
  overflow-y: auto;
  background: $admin-content-bg;
}
</style>

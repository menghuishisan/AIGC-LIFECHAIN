<template>
  <!-- Portal 门户布局：顶部导航 + 侧边栏 + 主内容区 -->
  <div class="portal-layout">
    <!-- 顶部导航 -->
    <header class="portal-header">
      <div class="portal-header__left">
        <div class="portal-header__logo" @click="router.push('/creator/dashboard')">
          <span class="portal-header__logo-icon">◈</span>
          <span class="portal-header__logo-text">LifeChain</span>
        </div>
        <!-- 全局导航胶囊 -->
        <div class="portal-header__nav-capsule">
          <span
            :class="['portal-header__nav-item', { active: activeCenter === 'creator' }]"
            @click="router.push('/creator/dashboard')"
          >创作中心</span>
          <span
            :class="['portal-header__nav-item', { active: activeCenter === 'buyer' }]"
            @click="router.push('/buyer/dashboard')"
          >交易中心</span>
        </div>
      </div>
      <div class="portal-header__right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索作品、订单..."
          :prefix-icon="Search"
          size="default"
          style="width: 200px"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
          <el-button circle @click="router.push('/notices')">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
        <el-dropdown @command="handleUserCommand">
          <div class="portal-header__user">
            <el-avatar :size="32">{{ userStore.profile?.nickname?.charAt(0) || 'U' }}</el-avatar>
            <span>{{ userStore.profile?.nickname || '用户' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="portal-body">
      <!-- 侧边栏 -->
      <aside class="portal-sidebar">
        <el-menu
          :default-active="route.path"
          router
          :collapse="false"
          class="portal-sidebar__menu"
        >
          <!-- 创作中心菜单 -->
          <el-menu-item index="/creator/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/creator/works">
            <el-icon><Picture /></el-icon>
            <span>我的作品</span>
          </el-menu-item>
          <el-menu-item index="/creator/claims">
            <el-icon><Stamp /></el-icon>
            <span>我的确权</span>
          </el-menu-item>
          <el-menu-item index="/creator/listings">
            <el-icon><Goods /></el-icon>
            <span>我的上架</span>
          </el-menu-item>
          <el-menu-item index="/creator/license-templates">
            <el-icon><Document /></el-icon>
            <span>授权模板</span>
          </el-menu-item>
          <el-menu-item index="/creator/orders">
            <el-icon><List /></el-icon>
            <span>创作者订单</span>
          </el-menu-item>
          <el-menu-item index="/creator/income">
            <el-icon><Wallet /></el-icon>
            <span>收益管理</span>
          </el-menu-item>
          <el-divider />
          <el-menu-item index="/market">
            <el-icon><Shop /></el-icon>
            <span>内容市场</span>
          </el-menu-item>
          <el-menu-item index="/buyer/dashboard">
            <el-icon><ShoppingCart /></el-icon>
            <span>购买中心</span>
          </el-menu-item>
          <el-menu-item index="/buyer/licenses">
            <el-icon><Key /></el-icon>
            <span>我的授权</span>
          </el-menu-item>
          <el-divider />
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="portal-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Document,
  Goods,
  Key,
  List,
  Odometer,
  Picture,
  Search,
  Shop,
  ShoppingCart,
  Stamp,
  User,
  Wallet
} from '@element-plus/icons-vue'
import { useUserStore } from '@/app/store/user'
import { appApi } from '@/shared/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const unreadCount = ref(0)

/** 判断当前在哪个中心 */
const activeCenter = computed(() => {
  if (route.path.startsWith('/buyer') || route.path.startsWith('/market')) return 'buyer'
  return 'creator'
})

/** 加载未读通知数 */
onMounted(async () => {
  try {
    const res = await appApi.getNotices({ pageNo: 1, pageSize: 1, readFlag: false })
    unreadCount.value = res.data?.total || 0
  } catch { /* 忽略 */ }
})

/** 用户菜单命令处理 */
function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

/** 全局搜索 */
function handleSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({ path: '/market', query: { keyword: searchKeyword.value.trim() } })
}
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.portal-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.portal-header {
  height: $header-height;
  background: $content-white;
  border-bottom: 1px solid $card-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

  &__left {
    display: flex;
    align-items: center;
    gap: 32px;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    &-icon {
      font-size: 24px;
      color: $brand-primary;
    }
    &-text {
      font-size: 18px;
      font-weight: 700;
      color: $brand-dark;
    }
  }

  &__nav-capsule {
    display: flex;
    background: #F0F3F5;
    border-radius: 8px;
    padding: 4px;
  }

  &__nav-item {
    padding: 6px 16px;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
    color: $text-secondary;
    transition: all 0.2s;

    &.active {
      background: $brand-primary;
      color: #fff;
    }
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
    font-size: 14px;
    color: $text-primary;
  }
}

.portal-body {
  display: flex;
  margin-top: $header-height;
  height: calc(100% - #{$header-height});
}

.portal-sidebar {
  width: $sidebar-width;
  background: $content-white;
  border-right: 1px solid $card-border;
  position: fixed;
  top: $header-height;
  bottom: 0;
  left: 0;
  overflow-y: auto;
  z-index: 90;

  &__menu {
    border-right: none;
    padding-top: 8px;

    :deep(.el-menu-item) {
      height: 44px;
      line-height: 44px;
      margin: 2px 8px;
      border-radius: 8px;

      &.is-active {
        background: $brand-light-bg;
        color: $brand-primary;
        font-weight: 500;
      }
    }
  }
}

.portal-main {
  flex: 1;
  margin-left: $sidebar-width;
  padding: $content-padding;
  max-width: $content-max-width;
  overflow-y: auto;
  background: $page-bg;
}

.el-divider {
  margin: 8px 16px;
}
</style>

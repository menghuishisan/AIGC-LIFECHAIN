<template>
  <!-- Regulator 监管后台布局 -->
  <div class="regulator-layout">
    <!-- 侧边栏 -->
    <aside class="regulator-sidebar">
      <div class="regulator-sidebar__logo" @click="router.push('/regulator/dashboard')">
        <span class="regulator-sidebar__logo-icon">◈</span>
        <span class="regulator-sidebar__logo-text">LifeChain</span>
        <span class="regulator-sidebar__logo-sub">监管后台</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1B242B"
        text-color="#96AAB3"
        active-text-color="#FFFFFF"
        class="regulator-sidebar__menu"
      >
        <el-menu-item index="/regulator/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/regulator/search">
          <el-icon><Search /></el-icon>
          <span>综合搜索</span>
        </el-menu-item>
        <el-menu-item index="/regulator/verify">
          <el-icon><CircleCheck /></el-icon>
          <span>验真核查</span>
        </el-menu-item>
        <el-menu-item index="/regulator/risks">
          <el-icon><WarningFilled /></el-icon>
          <span>风险事件</span>
        </el-menu-item>
        <el-menu-item index="/regulator/freezes">
          <el-icon><Lock /></el-icon>
          <span>冻结与解冻</span>
        </el-menu-item>
        <el-menu-item index="/regulator/disputes">
          <el-icon><ChatDotRound /></el-icon>
          <span>争议查看</span>
        </el-menu-item>
        <el-menu-item index="/regulator/reports">
          <el-icon><Tickets /></el-icon>
          <span>监管报告</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 右侧区域 -->
    <div class="regulator-right">
      <header class="regulator-header">
        <div class="regulator-header__left">
          <el-input
            v-model="searchKeyword"
            placeholder="综合搜索..."
            :prefix-icon="Search"
            size="default"
            style="width: 240px"
            clearable
            @keyup.enter="goSearch"
          />
        </div>
        <div class="regulator-header__right">
          <el-dropdown @command="handleCommand">
            <div class="regulator-header__user">
              <el-avatar :size="30">{{ userStore.profile?.nickname?.charAt(0) || 'R' }}</el-avatar>
              <span>{{ userStore.profile?.nickname || '监管员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="regulator-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound,
  CircleCheck,
  Lock,
  Odometer,
  Search,
  Tickets,
  WarningFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/app/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')

/** 跳转综合搜索 */
function goSearch() {
  if (searchKeyword.value) {
    router.push({ path: '/regulator/search', query: { keyword: searchKeyword.value } })
  }
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.regulator-layout {
  display: flex;
  height: 100%;
}

.regulator-sidebar {
  width: $admin-sidebar-width;
  background: $regulator-sidebar-bg;
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

    &-icon { font-size: 22px; color: #5CC1C9; }
    &-text { font-size: 16px; font-weight: 700; color: #fff; }
    &-sub { font-size: 11px; color: #6B8A94; margin-left: 4px; }
  }

  &__menu {
    border-right: none;

    :deep(.el-menu-item) {
      height: 44px;
      line-height: 44px;
      font-size: 13px;
      margin: 2px 8px;
      border-radius: 6px;
    }

    :deep(.el-menu-item.is-active) {
      background: $regulator-sidebar-active !important;
    }
  }
}

.regulator-right {
  flex: 1;
  margin-left: $admin-sidebar-width;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.regulator-header {
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

  &__left { display: flex; align-items: center; }
  &__right { display: flex; align-items: center; gap: 16px; }
  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #17242A;
  }
}

.regulator-main {
  flex: 1;
  padding: $content-padding;
  overflow-y: auto;
  background: $regulator-content-bg;
}
</style>

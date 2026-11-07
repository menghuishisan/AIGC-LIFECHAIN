<template>
  <!-- Regulator 监管后台布局 -->
  <div class="regulator-layout">
    <!-- 侧边栏 -->
    <aside
      ref="navPanel"
      :inert="mobileNavigation && !navOpen"
      :aria-hidden="mobileNavigation && !navOpen ? true : undefined"
      :role="mobileNavigation && navOpen ? 'dialog' : undefined"
      :aria-modal="mobileNavigation && navOpen ? true : undefined"
      aria-label="业务导航"
      :class="['regulator-sidebar', { 'is-open': navOpen }]"
    >
      <button
        class="regulator-sidebar__logo"
        type="button"
        @click="router.push('/regulator/dashboard')"
      >
        <span class="regulator-sidebar__logo-icon">◈</span>
        <span class="regulator-sidebar__logo-text">LifeChain</span>
        <span class="regulator-sidebar__logo-sub">监管后台</span>
      </button>
      <el-menu
        :default-active="route.path"
        router
        class="regulator-sidebar__menu"
        @select="navOpen = false"
      >
        <el-menu-item tabindex="0" index="/regulator/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/regulator/search">
          <el-icon><Search /></el-icon>
          <span>综合搜索</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/regulator/verify">
          <el-icon><CircleCheck /></el-icon>
          <span>验真核查</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/regulator/risks">
          <el-icon><WarningFilled /></el-icon>
          <span>风险事件</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/regulator/freezes">
          <el-icon><Lock /></el-icon>
          <span>冻结与解冻</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/regulator/disputes">
          <el-icon><ChatDotRound /></el-icon>
          <span>争议查看</span>
        </el-menu-item>
        <el-menu-item tabindex="0" index="/regulator/reports">
          <el-icon><Tickets /></el-icon>
          <span>监管报告</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <button
      v-if="navOpen"
      class="regulator-scrim"
      type="button"
      aria-label="关闭导航"
      @click="navOpen = false"
    />

    <!-- 右侧区域 -->
    <div class="regulator-right">
      <header class="regulator-header">
        <el-button
          ref="navToggle"
          class="regulator-header__menu-toggle"
          circle
          :aria-label="navOpen ? '关闭导航' : '打开导航'"
          :aria-expanded="navOpen"
          @click="navOpen = !navOpen"
        >
          <el-icon><Close v-if="navOpen" /><Menu v-else /></el-icon>
        </el-button>
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
            <div class="regulator-header__user" aria-label="账户菜单">
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
        <router-view :key="route.path" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useNavigationDrawer } from '@/shared/composables/useNavigationDrawer'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatDotRound,
  CircleCheck,
  Close,
  Lock,
  Menu,
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
const { navOpen, navPanel, navToggle, mobileNavigation } = useNavigationDrawer()

watch(
  () => route.fullPath,
  () => {
    navOpen.value = false
  }
)

/** 跳转综合搜索 */
function goSearch() {
  if (searchKeyword.value) {
    router.push({ path: '/regulator/search', query: { keyword: searchKeyword.value } })
  }
}

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

.regulator-layout {
  display: flex;
  min-height: 100vh;
  --lc-accent: #a55249;
  --lc-accent-strong: #783e39;
  --lc-accent-soft: #f4eae7;
  --lc-canvas: #f2f0ed;
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
      color: #d28b70;
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
  min-width: 0;
  margin-left: $admin-sidebar-width;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.regulator-header {
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
  &__right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  &__menu-toggle {
    display: none;
  }
  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #17242a;
  }
}

.regulator-main {
  flex: 1;
  min-width: 0;
  padding: $content-padding;
  overflow-y: auto;
  background: $regulator-content-bg;
}

.regulator-sidebar__menu {
  background: transparent;

  :deep(.el-menu-item) {
    margin: 2px 8px;
    border-radius: 3px;
    color: #c7b9b5;
  }

  :deep(.el-menu-item.is-active) {
    border-left: 2px solid #d28b70;
    background: $regulator-sidebar-active !important;
    color: #fff !important;
  }
}

.regulator-scrim {
  display: none;
}

@media (max-width: 860px) {
  .regulator-sidebar {
    z-index: 120;
    width: min(280px, calc(100vw - 48px));
    transform: translateX(-102%);
    transition: transform 180ms ease;

    &.is-open {
      transform: translateX(0);
    }
  }

  .regulator-header {
    gap: 10px;
    padding: 0 16px;

    &__menu-toggle {
      display: inline-flex;
      flex: 0 0 auto;
    }
  }

  .regulator-scrim {
    position: fixed;
    inset: 0;
    z-index: 110;
    display: block;
    border: 0;
    background: rgba(34, 27, 26, 0.44);
  }

  .regulator-right {
    margin-left: 0;
  }

  .regulator-main {
    padding: 22px 18px;
  }
}

@media (max-width: 560px) {
  .regulator-header {
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
  }

  .regulator-main {
    padding: 18px 12px;
  }
}
</style>

<template>
  <!-- Portal 门户布局：顶部导航 + 侧边栏 + 主内容区 -->
  <div :class="['portal-layout', { 'portal-layout--buyer': activeCenter === 'buyer' }]">
    <!-- 顶部导航 -->
    <header class="portal-header">
      <div class="portal-header__left">
        <button class="portal-header__logo" type="button" @click="router.push(homePath)">
          <span class="portal-header__logo-icon">◈</span>
          <span class="portal-header__logo-text">LifeChain</span>
        </button>
        <!-- 全局导航胶囊：仅在账号同时拥有创作者与购买者角色时显示 -->
        <div v-if="userStore.isCreator && userStore.isBuyer" class="portal-header__nav-capsule">
          <button
            type="button"
            :class="['portal-header__nav-item', { active: activeCenter === 'creator' }]"
            @click="router.push('/creator/dashboard')"
          >
            创作中心
          </button>
          <button
            type="button"
            :class="['portal-header__nav-item', { active: activeCenter === 'buyer' }]"
            @click="router.push('/buyer/dashboard')"
          >
            交易中心
          </button>
        </div>
      </div>
      <div class="portal-header__right">
        <el-input
          class="portal-header__search"
          v-model="searchKeyword"
          placeholder="搜索市场作品"
          :prefix-icon="Search"
          size="default"
          style="width: 200px"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
          <el-button circle aria-label="查看通知" @click="router.push('/notices')">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>
        <el-dropdown @command="handleUserCommand">
          <div class="portal-header__user" aria-label="账户菜单">
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
        <el-button
          ref="navToggle"
          class="portal-header__menu-toggle"
          circle
          :aria-label="navOpen ? '关闭导航' : '打开导航'"
          :aria-expanded="navOpen"
          @click="navOpen = !navOpen"
        >
          <el-icon><Close v-if="navOpen" /><Menu v-else /></el-icon>
        </el-button>
      </div>
    </header>

    <button
      v-if="navOpen"
      class="portal-scrim"
      type="button"
      aria-label="关闭导航"
      @click="navOpen = false"
    />

    <div class="portal-body">
      <!-- 侧边栏 -->
      <aside
        ref="navPanel"
        :inert="mobileNavigation && !navOpen"
        :aria-hidden="mobileNavigation && !navOpen ? true : undefined"
        :role="mobileNavigation && navOpen ? 'dialog' : undefined"
        :aria-modal="mobileNavigation && navOpen ? true : undefined"
        aria-label="业务导航"
        :class="['portal-sidebar', { 'is-open': navOpen }]"
      >
        <el-menu
          :default-active="route.path"
          router
          :collapse="false"
          class="portal-sidebar__menu"
          @select="navOpen = false"
        >
          <!-- 创作中心：仅 CREATOR 角色可见 -->
          <template v-if="userStore.isCreator && activeCenter === 'creator'">
            <el-menu-item tabindex="0" index="/creator/dashboard">
              <el-icon><Odometer /></el-icon>
              <span>工作台</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/creator/works">
              <el-icon><Picture /></el-icon>
              <span>我的作品</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/creator/claims">
              <el-icon><Stamp /></el-icon>
              <span>我的确权</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/creator/listings">
              <el-icon><Goods /></el-icon>
              <span>我的上架</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/creator/license-templates">
              <el-icon><Document /></el-icon>
              <span>授权模板</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/creator/orders">
              <el-icon><List /></el-icon>
              <span>创作者订单</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/creator/income">
              <el-icon><Wallet /></el-icon>
              <span>收益管理</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/market">
              <el-icon><Shop /></el-icon>
              <span>内容市场</span>
            </el-menu-item>
          </template>

          <!-- 交易中心：仅 BUYER 角色可见 -->
          <template v-if="userStore.isBuyer && activeCenter === 'buyer'">
            <el-menu-item tabindex="0" index="/buyer/dashboard">
              <el-icon><ShoppingCart /></el-icon>
              <span>购买中心</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/market">
              <el-icon><Shop /></el-icon>
              <span>内容市场</span>
            </el-menu-item>
            <el-menu-item tabindex="0" index="/buyer/licenses">
              <el-icon><Key /></el-icon>
              <span>我的授权</span>
            </el-menu-item>
          </template>

          <!-- 通用区：portal 全部角色可见 -->
          <el-divider />
          <el-menu-item tabindex="0" index="/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <el-menu-item tabindex="0" index="/notices">
            <el-icon><Bell /></el-icon>
            <span>消息通知</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="portal-main">
        <router-view :key="route.path" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useNavigationDrawer } from '@/shared/composables/useNavigationDrawer'
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  Close,
  Document,
  Goods,
  Key,
  List,
  Menu,
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
const { navOpen, navPanel, navToggle, mobileNavigation } = useNavigationDrawer()

/** 当前默认首页：取 user store 的统一映射 */
const homePath = computed(() => userStore.defaultHome)

/** 市场和通用页面沿用用户刚选择的中心，角色专属路由则显式切换。 */
const preferredCenter = ref<'creator' | 'buyer'>(userStore.isCreator ? 'creator' : 'buyer')
watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/creator/')) preferredCenter.value = 'creator'
    if (path.startsWith('/buyer/')) preferredCenter.value = 'buyer'
  },
  { immediate: true }
)
const activeCenter = computed(() =>
  userStore.isCreator && userStore.isBuyer
    ? preferredCenter.value
    : userStore.isCreator
      ? 'creator'
      : 'buyer'
)

watch(
  () => route.fullPath,
  () => {
    navOpen.value = false
  }
)

/** 加载未读通知数 */
onMounted(async () => {
  try {
    const res = await appApi.getNotices({ pageNo: 1, pageSize: 1, readFlag: false })
    unreadCount.value = res.data?.total || 0
  } catch {
    /* 忽略 */
  }
})

/** 用户菜单命令处理 */
async function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await userStore.logout()
    } catch {
      // 撤销失败已由请求层提示，本地凭证仍须清理。
    } finally {
      await router.push('/login')
    }
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  --lc-accent: #38684c;
  --lc-accent-strong: #315b43;
  --lc-accent-soft: #e8f0e8;
  --lc-canvas: #f1f3ee;
}

.portal-layout--buyer {
  --lc-accent: #356f73;
  --lc-accent-strong: #245458;
  --lc-accent-soft: #e5eff0;
}

.portal-header {
  min-height: $header-height;
  flex: 0 0 $header-height;
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
  box-shadow: none;

  &__left {
    display: flex;
    align-items: center;
    gap: 32px;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0;
    border: 0;
    background: transparent;
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
    gap: 4px;
    padding: 3px;
    background: #f1f3f0;
    border: 1px solid $card-border;
    border-radius: 4px;
  }

  &__nav-item {
    min-height: 30px;
    padding: 5px 12px;
    border: 0;
    border-radius: 3px;
    background: transparent;
    color: $text-secondary;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition:
      background-color 160ms ease,
      color 160ms ease;

    &.active {
      background: $brand-dark;
      color: #fff;
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 14px;
    color: $text-primary;
  }

  &__menu-toggle {
    display: none;
  }
}

.portal-body {
  display: flex;
  min-height: calc(100vh - #{$header-height});
}

.portal-sidebar {
  width: $sidebar-width;
  flex: 0 0 $sidebar-width;
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
      border-radius: 3px;

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
  min-width: 0;
  margin-left: $sidebar-width;
  padding: $content-padding;
  overflow-y: auto;
  background: $page-bg;
}

.portal-header__menu-toggle,
.portal-scrim {
  display: none;
}

.el-divider {
  margin: 8px 16px;
}

@media (max-width: 860px) {
  .portal-header {
    padding: 0 16px;

    &__left {
      gap: 14px;
    }

    &__right {
      gap: 8px;
    }

    &__user span {
      display: none;
    }

    &__menu-toggle {
      display: inline-flex;
    }
  }

  .portal-body {
    min-height: calc(100vh - #{$header-height});
  }

  .portal-sidebar {
    position: fixed;
    top: $header-height;
    bottom: 0;
    left: 0;
    z-index: 120;
    width: min(280px, calc(100vw - 48px));
    transform: translateX(-102%);
    transition: transform 180ms ease;

    &.is-open {
      transform: translateX(0);
    }
  }

  .portal-scrim {
    position: fixed;
    inset: $header-height 0 0;
    z-index: 110;
    display: block;
    border: 0;
    background: rgba(22, 33, 29, 0.4);
  }

  .portal-main {
    width: 100%;
    margin-left: 0;
    padding: 22px 18px;
  }
}

@media (max-width: 560px) {
  .portal-header {
    padding: 0 10px;

    &__logo-text {
      font-size: 15px;
    }

    &__nav-capsule {
      gap: 2px;
      padding: 2px;
    }

    &__nav-item {
      padding: 4px 7px;
      font-size: 11px;
    }

    &__search {
      display: none;
    }
  }

  .portal-main {
    padding: 18px 12px;
  }
}
</style>

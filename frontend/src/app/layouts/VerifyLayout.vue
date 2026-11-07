<template>
  <!-- Verify 公开验真布局：极简，无侧栏 -->
  <div class="verify-layout">
    <header class="verify-header">
      <button class="verify-header__logo" type="button" @click="router.push('/verify')">
        <span class="verify-header__logo-icon">◈</span>
        <span class="verify-header__logo-text">LifeChain <small>公开验真</small></span>
      </button>
      <div class="verify-header__right">
        <el-button link @click="router.push('/verify/help')">帮助</el-button>
        <el-button link @click="router.push('/login')">登录</el-button>
      </div>
    </header>
    <main class="verify-main">
      <router-view :key="route.path" />
    </main>
    <footer class="verify-footer">
      <p>AIGC 内容可信验真 · 公开页仅展示必要摘要，不展示敏感原始内容</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
const router = useRouter()
const route = useRoute()
</script>

<style lang="scss" scoped>
.verify-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f4f6f1;
  --lc-accent: #376a62;
  --lc-accent-strong: #27534c;
  --lc-accent-soft: #e7f0e9;
  --lc-canvas: #f4f6f1;
}

.verify-header {
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 max(20px, calc((100vw - 1240px) / 2));
  background: #fff;
  border-bottom: 1px solid var(--lc-line);

  &__logo {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0;
    border: 0;
    background: transparent;
    cursor: pointer;

    &-icon {
      font-size: 20px;
      color: var(--lc-accent);
    }
    &-text {
      color: #202a28;
      font-size: 15px;
      font-weight: 650;
    }
    small {
      margin-left: 6px;
      color: #65736d;
      font-size: 11px;
      font-weight: 500;
    }
  }

  &__right {
    display: flex;
    gap: 16px;
  }
}

.verify-main {
  flex: 1;
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 36px 32px 20px;
}

.verify-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--lc-line);
  color: #55645c;
  font-size: 11px;
  text-align: center;
}

@media (max-width: 760px) {
  .verify-header {
    padding: 0 14px;
  }

  .verify-main {
    padding: 24px 16px 12px;
  }

  .verify-header__right {
    gap: 4px;
  }
}
</style>

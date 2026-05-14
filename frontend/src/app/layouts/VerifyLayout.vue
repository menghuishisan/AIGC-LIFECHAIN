<template>
  <!-- Verify 公开验真布局：极简，无侧栏 -->
  <div class="verify-layout">
    <header class="verify-header">
      <div class="verify-header__logo" @click="router.push('/verify')">
        <span class="verify-header__logo-icon">◈</span>
        <span class="verify-header__logo-text">LifeChain 验真</span>
      </div>
      <div class="verify-header__right">
        <el-button link @click="router.push('/verify/help')">帮助</el-button>
        <el-button link @click="router.push('/login')">登录</el-button>
      </div>
    </header>
    <main class="verify-main">
      <router-view />
    </main>
    <footer class="verify-footer">
      <p>AIGC 内容可信验真 · 公开页仅展示必要摘要，不展示敏感原始内容</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style lang="scss" scoped>
.verify-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #F6F8F9;
}

.verify-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: #fff;
  border-bottom: 1px solid #E8ECF0;

  &__logo {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    &-icon { font-size: 20px; color: #0F5F66; }
    &-text { font-size: 16px; font-weight: 600; color: #132126; }
  }

  &__right {
    display: flex;
    gap: 16px;
  }
}

.verify-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.verify-footer {
  text-align: center;
  padding: 24px;
  font-size: 12px;
  color: #8A9AA1;
}
</style>

<template>
  <div class="auth-layout">
    <header class="auth-header">
      <RouterLink class="auth-brand" to="/login" aria-label="LifeChain 登录">
        <span class="auth-brand__mark">◈</span>
        <span>LifeChain</span>
      </RouterLink>
      <nav class="auth-header__nav" aria-label="公开导航">
        <span>账户访问</span>
        <el-button link type="primary" @click="router.push('/verify')">公开验真</el-button>
      </nav>
    </header>

    <main class="auth-main">
      <section class="auth-panel" aria-labelledby="auth-title">
        <p class="auth-panel__eyebrow">ACCOUNT ACCESS / LIFECHAIN</p>
        <h1 id="auth-title">{{ title }}</h1>
        <p class="auth-panel__description">{{ description }}</p>
        <slot />
      </section>
    </main>

    <footer class="auth-footer">账号凭证仅用于登录与身份核验</footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
defineProps<{ title: string; description: string }>()

const router = useRouter()
</script>

<style scoped lang="scss">
@use '@/shared/styles/variables' as *;

.auth-layout {
  display: grid;
  min-height: 100vh;
  grid-template-rows: 60px 1fr auto;
  background: var(--lc-canvas);
  --lc-accent: #38684c;
  --lc-accent-strong: #315b43;
  --lc-accent-soft: #e8f0e8;
  --lc-canvas: #f1f3ee;
}

.auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 max(16px, calc((100vw - 1120px) / 2));
  background: var(--lc-surface);
  border-bottom: 1px solid var(--lc-line);
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--lc-ink);
  font-size: 16px;
  font-weight: 700;
  text-decoration: none;
}

.auth-brand__mark {
  color: var(--lc-accent);
  font-size: 22px;
}

.auth-header__nav {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--lc-muted);
  font-size: 12px;
}

.auth-main {
  display: grid;
  align-items: center;
  justify-items: center;
  padding: 36px 16px;
}

.auth-panel {
  width: min(100%, 460px);
  padding: 32px;
  background: var(--lc-surface);
  border: 1px solid var(--lc-line);
}

.auth-panel__eyebrow {
  color: var(--lc-accent-strong);
  font: 600 10px/1.4 var(--lc-mono);
}

.auth-panel h1 {
  margin: 8px 0 4px;
  color: var(--lc-ink);
  font-size: 26px;
  font-weight: 650;
}

.auth-panel__description {
  margin-bottom: 22px;
  color: var(--lc-muted);
  font-size: 13px;
}

:deep(.auth-form .el-form-item) {
  margin-bottom: 16px;
}

:deep(.auth-form .el-input__wrapper),
:deep(.auth-form .el-select__wrapper) {
  min-height: 42px;
}

:deep(.auth-submit) {
  width: 100%;
  margin-top: 4px;
}

:deep(.auth-form__footer) {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  color: var(--lc-muted);
  font-size: 11px;
}

.auth-footer {
  padding: 16px;
  border-top: 1px solid var(--lc-line);
  color: var(--lc-muted);
  font-size: 11px;
  text-align: center;
}

@media (max-width: 520px) {
  .auth-panel {
    padding: 24px 18px;
  }

  .auth-header__nav > span {
    display: none;
  }
}
</style>

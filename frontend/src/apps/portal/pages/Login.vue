<template>
  <!-- 登录页：左右双栏布局 -->
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="login-brand__content">
        <div class="login-brand__logo">
          <span class="login-brand__logo-icon">◈</span>
          <span class="login-brand__logo-text">LifeChain</span>
        </div>
        <h1 class="login-brand__title">可信管理 AIGC 内容的每一个关键节点</h1>
        <p class="login-brand__desc">
          从注册认证、DID 生效、作品确权，到证书验真、授权交易、收益分账与监管留痕，<br>
          把原本割裂的内容生命周期收束到一个可信工作台。
        </p>
        <!-- 流程节点 -->
        <div class="login-brand__flow">
          <div class="flow-node" v-for="step in flowSteps" :key="step">
            <span class="flow-node__dot"></span>
            <span class="flow-node__text">{{ step }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录卡 -->
    <div class="login-form-area">
      <div class="login-card">
        <h2 class="login-card__title">欢迎回来</h2>
        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <el-form-item prop="mobile">
            <el-input
              v-model="form.mobile"
              placeholder="手机号"
              size="large"
              :prefix-icon="Phone"
              maxlength="11"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-card__btn"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-card__footer">
          <el-button link type="primary" @click="router.push('/register')">去注册</el-button>
          <el-button link type="info" @click="router.push('/verify')">公开验真</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock, Phone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/app/store/user'
import { authApi } from '@/shared/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ mobile: '', password: '' })

const rules = {
  mobile: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const flowSteps = ['注册认证', 'DID生效', '作品确权', '证书验真', '授权交易']

/** 登录处理 */
async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await authApi.login({ mobile: form.mobile, password: form.password })
    userStore.setLogin(res.data)
    await userStore.loadProfile()
    ElMessage.success('登录成功')

    /* 根据角色跳转 */
    const redirect = route.query.redirect as string
    if (redirect) {
      router.push(redirect)
    } else if (userStore.isAdmin) {
      router.push('/admin/dashboard')
    } else if (userStore.isRegulator) {
      router.push('/regulator/dashboard')
    } else {
      router.push('/creator/dashboard')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/shared/styles/variables' as *;

.login-page {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #F4F7F8 0%, #E7F4F5 100%);
}

.login-brand {
  flex: 0 0 56%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(160deg, #0A3F45 0%, #0F5F66 50%, #0B7FA2 100%);
  position: relative;
  overflow: hidden;

  /* 网格背景 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
    background-size: 40px 40px;
  }

  &__content {
    position: relative;
    z-index: 1;
    max-width: 560px;
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 32px;

    &-icon { font-size: 32px; color: #5CC1C9; }
    &-text { font-size: 24px; font-weight: 700; color: #fff; }
  }

  &__title {
    font-size: 32px;
    font-weight: 700;
    color: #fff;
    line-height: 1.4;
    margin-bottom: 16px;
  }

  &__desc {
    font-size: 15px;
    color: rgba(255, 255, 255, 0.7);
    line-height: 1.8;
    margin-bottom: 40px;
  }

  &__flow {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.flow-node {
  display: flex;
  align-items: center;
  gap: 6px;

  &__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #5CC1C9;
    border: 2px solid rgba(92, 193, 201, 0.3);
  }

  &__text {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.8);
  }

  &::after {
    content: '────';
    color: rgba(255, 255, 255, 0.2);
    font-size: 8px;
    margin: 0 2px;
    letter-spacing: -2px;
  }

  &:last-child::after {
    display: none;
  }
}

.login-form-area {
  flex: 0 0 44%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(15, 40, 45, 0.08);

  &__title {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    margin-bottom: 32px;
  }

  &__btn {
    width: 100%;
    height: 46px;
    font-size: 16px;
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    margin-top: 16px;
  }
}
</style>

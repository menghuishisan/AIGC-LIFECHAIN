<template>
  <AuthLayout title="登录 LifeChain" description="使用平台账户进入对应的业务工作台。">
    <el-form
      ref="formRef"
      class="auth-form"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="手机号" prop="mobile">
        <el-input
          v-model="form.mobile"
          autocomplete="username"
          inputmode="tel"
          placeholder="请输入手机号"
          size="large"
          :prefix-icon="Phone"
          maxlength="11"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          autocomplete="current-password"
          type="password"
          placeholder="请输入密码"
          size="large"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>
      <el-alert v-if="loginError" type="error" :title="loginError" :closable="false" show-icon />
      <el-button
        type="primary"
        native-type="submit"
        size="large"
        :loading="loading"
        class="auth-submit"
        >登录</el-button
      >
    </el-form>
    <div class="auth-form__footer">
      <el-button link type="primary" @click="router.push('/register')">创建账户</el-button>
      <span>登录后按账户角色进入工作台</span>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock, Phone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import { useUserStore } from '@/app/store/user'
import { safeRedirect } from '@/app/guards/authGuard'
import { authApi } from '@/shared/api'
defineOptions({ name: 'PortalLoginPage' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const loginError = ref('')
const form = reactive({ mobile: '', password: '' })
const rules: FormRules = {
  mobile: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  if (route.query.forbidden === '1') ElMessage.warning('当前账户无权访问该页面，请使用对应角色登录')
})

/** 登录资料加载成功后再跳转，失败时清理不完整的登录态。 */
async function handleLogin() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  loginError.value = ''
  try {
    const res = await authApi.login({ mobile: form.mobile.trim(), password: form.password })
    userStore.setLogin(res.data)
    await userStore.loadProfile()
    await router.push(safeRedirect(route.query.redirect) ?? userStore.defaultHome)
  } catch (error) {
    await userStore.logout().catch(() => undefined)
    loginError.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

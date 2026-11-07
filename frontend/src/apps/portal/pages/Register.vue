<template>
  <AuthLayout title="创建账户" description="完成手机号验证，开始管理作品和授权。">
    <el-form
      ref="formRef"
      class="auth-form"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleRegister"
    >
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="form.nickname"
          autocomplete="nickname"
          placeholder="请输入昵称"
          size="large"
          :prefix-icon="User"
          maxlength="64"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input
          v-model="form.mobile"
          autocomplete="tel"
          inputmode="tel"
          placeholder="请输入手机号"
          size="large"
          :prefix-icon="Phone"
          maxlength="11"
        />
      </el-form-item>
      <el-form-item label="短信验证码" prop="smsCode">
        <div class="sms-row">
          <el-input
            v-model="form.smsCode"
            autocomplete="one-time-code"
            inputmode="numeric"
            placeholder="6位验证码"
            size="large"
            maxlength="6"
          />
          <el-button
            size="large"
            :disabled="smsCooldown > 0 || !isMobileValid"
            :loading="smsLoading"
            @click="handleSendSms"
          >
            {{ smsCooldown > 0 ? smsCooldown + ' 秒后重发' : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          autocomplete="new-password"
          type="password"
          placeholder="8至32位密码"
          size="large"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          autocomplete="new-password"
          type="password"
          placeholder="再次输入密码"
          size="large"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>
      <el-form-item label="账户类型" prop="accountType">
        <el-radio-group v-model="form.accountType"
          ><el-radio value="PERSONAL">个人账户</el-radio
          ><el-radio value="ENTERPRISE">企业账户</el-radio></el-radio-group
        >
      </el-form-item>
      <el-button
        type="primary"
        native-type="submit"
        size="large"
        :loading="loading"
        class="auth-submit"
        >注册</el-button
      >
    </el-form>
    <div class="auth-form__footer">
      <el-button link type="primary" @click="router.push('/login')">已有账户？去登录</el-button>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AuthLayout from '@/app/layouts/AuthLayout.vue'
import { Lock, Phone, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/app/store/user'
import { authApi } from '@/shared/api'
import { generateRequestId } from '@/shared/utils'
defineOptions({ name: 'PortalRegisterPage' })

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const smsLoading = ref(false)
const smsCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  nickname: '',
  mobile: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  accountType: 'PERSONAL'
})

const isMobileValid = computed(() => /^1[3-9]\d{9}$/.test(form.mobile))

const rules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度为8-32位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

async function handleSendSms() {
  if (smsLoading.value || smsCooldown.value > 0) return
  if (!isMobileValid.value) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  smsLoading.value = true
  try {
    await authApi.sendSmsCode(form.mobile)
    ElMessage.success('验证码已发送，请注意查收')
    smsCooldown.value = 60
    cooldownTimer = setInterval(() => {
      smsCooldown.value--
      if (smsCooldown.value <= 0 && cooldownTimer) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
  } catch {
    // 错误由 http 拦截器统一提示
  } finally {
    smsLoading.value = false
  }
}

async function handleRegister() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await authApi.register({
      nickname: form.nickname,
      mobile: form.mobile,
      smsCode: form.smsCode,
      password: form.password,
      accountType: form.accountType,
      requestId: generateRequestId()
    })
    if (res.success && res.data) {
      userStore.setLogin(res.data)
      ElMessage.success('注册成功')
      router.push('/')
    }
  } catch {
    // 错误由 http 拦截器统一提示
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
.sms-row {
  display: flex;
  width: 100%;
  gap: 8px;
}
.sms-row .el-input {
  min-width: 0;
  flex: 1;
}
</style>

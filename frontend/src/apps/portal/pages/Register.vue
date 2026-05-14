<template>
  <!-- 注册页 -->
  <div class="register-page">
    <div class="register-card">
      <div class="register-card__header">
        <span class="register-card__logo" @click="router.push('/login')">◈ LifeChain</span>
        <h2>创建账户</h2>
        <p>注册后即可使用 AIGC 内容可信管理平台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="mobile">
          <el-input v-model="form.mobile" placeholder="手机号" size="large" :prefix-icon="Phone" maxlength="11" />
        </el-form-item>
        <el-form-item prop="smsCode">
          <div class="sms-row">
            <el-input v-model="form.smsCode" placeholder="短信验证码" size="large" maxlength="6" />
            <el-button
              size="large"
              :disabled="smsCooldown > 0 || !isMobileValid"
              :loading="smsLoading"
              class="sms-btn"
              @click="handleSendSms"
            >
              {{ smsCooldown > 0 ? `${smsCooldown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（8-32位）" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="accountType">
          <el-radio-group v-model="form.accountType">
            <el-radio value="PERSONAL">个人账户</el-radio>
            <el-radio value="ENTERPRISE">企业账户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="register-card__btn" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-card__footer">
        <el-button link type="primary" @click="router.push('/login')">已有账户？去登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Phone, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/app/store/user'
import { authApi } from '@/shared/api'
import { generateRequestId } from '@/shared/utils'

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

const rules = {
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
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== form.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }]
}

async function handleSendSms() {
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

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page);
}

.register-card {
  width: 420px;
  padding: 40px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-light);

  &__header {
    text-align: center;
    margin-bottom: 32px;

    h2 {
      margin: 8px 0 4px;
      font-size: 22px;
      font-weight: 600;
    }

    p {
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }
  }

  &__logo {
    font-size: 20px;
    font-weight: 700;
    color: var(--el-color-primary);
    cursor: pointer;
  }

  &__btn {
    width: 100%;
  }

  &__footer {
    text-align: center;
    margin-top: 16px;
  }
}

.sms-row {
  display: flex;
  gap: 8px;
  width: 100%;

  .el-input {
    flex: 1;
  }

  .sms-btn {
    white-space: nowrap;
    min-width: 120px;
  }
}
</style>

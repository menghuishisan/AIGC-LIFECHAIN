<template>
  <!-- 个人中心页 -->
  <div class="profile-page" v-loading="loading">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <template v-if="profile">
      <el-row :gutter="20">
        <!-- 基本信息 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">基本信息</div>
            <el-form :model="editForm" label-width="80px">
              <el-form-item label="昵称">
                <el-input v-model="editForm.nickname" :disabled="!editing" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="editForm.email" :disabled="!editing" />
              </el-form-item>
              <el-form-item label="头像URL">
                <el-input v-model="editForm.avatarUrl" :disabled="!editing" />
              </el-form-item>
              <el-form-item>
                <el-button v-if="!editing" type="primary" @click="editing = true">编辑</el-button>
                <template v-else>
                  <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
                  <el-button @click="resetEdit">取消</el-button>
                </template>
              </el-form-item>
            </el-form>
          </div>
        </el-col>

        <!-- 账户状态 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">账户状态</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="账户编号"><span class="font-mono">{{ profile.accountNo }}</span></el-descriptions-item>
              <el-descriptions-item label="手机号">{{ maskMobile(profile.mobile) }}</el-descriptions-item>
              <el-descriptions-item label="账户类型">{{ AccountTypeMap[profile.accountType] || profile.accountType }}</el-descriptions-item>
              <el-descriptions-item label="账户状态">
                <StatusTag :status="profile.status" type="account" />
              </el-descriptions-item>
              <el-descriptions-item label="实名认证">
                <StatusTag :status="profile.authStatus" type="auth" />
                <el-button v-if="['NOT_AUTH', 'AUTH_REJECTED'].includes(profile.authStatus)" link type="primary" style="margin-left: 8px;" @click="router.push('/profile/auth')">
                  去认证
                </el-button>
              </el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag v-for="r in profile.roles" :key="r" size="small" style="margin: 2px;">{{ RoleMap[r] || r }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <!-- DID 信息 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">DID 信息</div>
            <template v-if="profile.didInfo">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="DID 编号"><span class="font-mono">{{ profile.didInfo.didNo }}</span></el-descriptions-item>
                <el-descriptions-item label="DID 值"><span class="font-mono">{{ profile.didInfo.didValue }}</span></el-descriptions-item>
                <el-descriptions-item label="DID 状态">
                  <StatusTag :status="profile.didInfo.status" type="did" />
                </el-descriptions-item>
                <el-descriptions-item label="链上状态">
                  <StatusTag :status="profile.didInfo.chainStatus" type="chain" />
                </el-descriptions-item>
              </el-descriptions>
            </template>
            <template v-else>
              <el-empty description="尚未申请 DID" :image-size="80">
                <el-button type="primary" @click="router.push('/profile/did/apply')">申请 DID</el-button>
              </el-empty>
            </template>
          </div>
        </el-col>

        <!-- 认证主体信息 -->
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">认证主体</div>
            <template v-if="profile.subjectInfo">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="主体类型">{{ profile.subjectInfo.subjectType === 'PERSONAL' ? '个人' : '企业' }}</el-descriptions-item>
                <el-descriptions-item label="真实姓名">{{ profile.subjectInfo.realName }}</el-descriptions-item>
                <el-descriptions-item label="证件类型">{{ profile.subjectInfo.idCardType }}</el-descriptions-item>
                <el-descriptions-item label="证件号码">{{ maskIdCard(profile.subjectInfo.idCardNo) }}</el-descriptions-item>
              </el-descriptions>
            </template>
            <template v-else>
              <el-empty description="尚未完成实名认证" :image-size="80">
                <el-button v-if="['NOT_AUTH', 'AUTH_REJECTED'].includes(profile.authStatus)" type="primary" @click="router.push('/profile/auth')">
                  去认证
                </el-button>
              </el-empty>
            </template>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { AccountTypeMap, RoleMap } from '@/shared/constants'
import { maskMobile } from '@/shared/utils'
import type { AccountProfileVO } from '@/shared/types'

const router = useRouter()
const loading = ref(false)
const editing = ref(false)
const saving = ref(false)
const profile = ref<AccountProfileVO | null>(null)

const editForm = reactive({
  nickname: '',
  email: '',
  avatarUrl: ''
})

/** 脱敏证件号 */
function maskIdCard(idCard: string): string {
  if (!idCard || idCard.length < 8) return idCard || ''
  return idCard.substring(0, 4) + '****' + idCard.substring(idCard.length - 4)
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await authApi.getProfile()
    profile.value = res.data
    resetEdit()
  } finally {
    loading.value = false
  }
}

function resetEdit() {
  editing.value = false
  if (profile.value) {
    editForm.nickname = profile.value.nickname
    editForm.email = profile.value.email || ''
    editForm.avatarUrl = profile.value.avatarUrl || ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    await authApi.updateProfile({
      nickname: editForm.nickname || undefined,
      email: editForm.email || undefined,
      avatarUrl: editForm.avatarUrl || undefined
    })
    ElMessage.success('资料已更新')
    editing.value = false
    loadProfile()
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

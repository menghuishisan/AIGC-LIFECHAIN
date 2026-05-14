<template>
  <!-- DID 申请页 -->
  <div class="did-apply-page">
    <div class="page-header">
      <h2>申请 DID</h2>
    </div>

    <div class="lc-card" style="max-width: 640px;">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 24px;">
        <template #title>DID 说明</template>
        <p>DID（分布式数字身份标识）是基于区块链的数字身份凭证。</p>
        <p>申请前请确保您已完成实名认证。提交后系统将审核并上链。</p>
      </el-alert>

      <!-- 当前 DID 状态 -->
      <div v-if="didInfo" style="margin-bottom: 24px;">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="DID 编号"><span class="font-mono">{{ didInfo.didNo }}</span></el-descriptions-item>
          <el-descriptions-item label="DID 值"><span class="font-mono">{{ didInfo.didValue }}</span></el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="didInfo.status" type="did" />
          </el-descriptions-item>
          <el-descriptions-item label="链上状态">
            <StatusTag :status="didInfo.chainStatus" type="chain" />
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(didInfo.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ formatTime(didInfo.activeTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 申请按钮（仅未申请时） -->
      <div v-if="!didInfo || didInfo.status === 'DID_NOT_APPLIED'">
        <el-button type="primary" size="large" :loading="applying" @click="handleApply">
          申请 DID
        </el-button>
      </div>
      <div v-else>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { DidInfoVO } from '@/shared/types'

const router = useRouter()
const applying = ref(false)
const didInfo = ref<DidInfoVO | null>(null)

async function loadDid() {
  try {
    const profileRes = await authApi.getProfile()
    didInfo.value = profileRes.data.didInfo || null
  } catch { /* 忽略 */ }
}

async function handleApply() {
  applying.value = true
  try {
    await authApi.applyDid({ requestId: generateRequestId() })
    ElMessage.success('DID 申请已提交，请等待审核')
    loadDid()
  } finally {
    applying.value = false
  }
}

onMounted(loadDid)
</script>

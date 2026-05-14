<template>
  <!-- 证书生成页（管理员） -->
  <div class="cert-generate-page">
    <div class="page-header">
      <h2>生成证书</h2>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <div class="lc-card">
      <el-form :model="form" label-width="120px" @submit.prevent style="max-width: 600px;">
        <el-form-item label="确权编号" required>
          <el-input v-model="form.claimNo" placeholder="请输入已审核通过的确权编号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleGenerate">生成证书</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="result" class="lc-card" style="margin-top: 20px;">
      <div class="lc-card__title">证书已生成</div>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="证书编号"><span class="font-mono">{{ result.basicInfo.certNo }}</span></el-descriptions-item>
        <el-descriptions-item label="状态"><StatusTag :status="result.statusInfo.status" type="cert" /></el-descriptions-item>
        <el-descriptions-item label="作品编号"><span class="font-mono">{{ result.relationInfo.workNo }}</span></el-descriptions-item>
        <el-descriptions-item label="确权编号"><span class="font-mono">{{ result.relationInfo.claimNo }}</span></el-descriptions-item>
        <el-descriptions-item label="签发时间">{{ formatTime(result.timeInfo.issueTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workApi } from '@/shared/api'
import { StatusTag } from '@/shared/components'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { CertDetailVO } from '@/shared/types'

const router = useRouter()
const submitting = ref(false)
const result = ref<CertDetailVO | null>(null)

const form = reactive({
  claimNo: '',
})

async function handleGenerate() {
  if (!form.claimNo.trim()) {
    ElMessage.warning('请输入确权编号')
    return
  }
  submitting.value = true
  try {
    const res = await workApi.generateCertificate({
      claimNo: form.claimNo.trim(),
      requestId: generateRequestId(),
    })
    result.value = res.data
    ElMessage.success('证书生成成功')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>

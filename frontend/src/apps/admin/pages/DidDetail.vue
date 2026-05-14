<template>
  <!-- DID 详情页（管理员） -->
  <div class="did-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="`DID ${detail.didNo}`" :biz-no="detail.didNo" :status="detail.status" status-type="did">
        <template #actions>
          <el-button v-if="detail.status === 'DID_PENDING'" type="primary" @click="openReview">审核</el-button>
          <el-button v-if="detail.status === 'DID_ACTIVE'" type="warning" @click="handleSuspend">暂停</el-button>
          <el-button v-if="['DID_ACTIVE','DID_SUSPENDED'].includes(detail.status)" type="danger" @click="handleRevoke">撤销</el-button>
        </template>
      </DetailHeader>

      <div class="lc-card">
        <div class="lc-card__title">DID 信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="DID 编号"><span class="font-mono">{{ detail.didNo }}</span></el-descriptions-item>
          <el-descriptions-item label="DID 值"><span class="font-mono">{{ detail.didValue }}</span></el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :status="detail.status" type="did" /></el-descriptions-item>
          <el-descriptions-item label="链上状态"><StatusTag :status="detail.chainStatus" type="chain" /></el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(detail.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ formatTime(detail.activeTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </template>

    <!-- 审核对话框 -->
    <el-dialog v-model="showReview" title="DID 审核" width="480px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.reviewResult">
            <el-radio value="APPROVED">通过</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReview = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReview">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi } from '@/shared/api'
import { DetailHeader, StatusTag } from '@/shared/components'
import { formatTime, generateRequestId } from '@/shared/utils'
import type { DidInfoVO } from '@/shared/types'

const props = defineProps<{ didNo: string }>()
const loading = ref(false)
const submitting = ref(false)
const detail = ref<DidInfoVO | null>(null)
const showReview = ref(false)
const reviewForm = reactive({ reviewResult: 'APPROVED', reviewComment: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await authApi.getDidInfo(props.didNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function openReview() {
  reviewForm.reviewResult = 'APPROVED'
  reviewForm.reviewComment = ''
  showReview.value = true
}

async function handleReview() {
  submitting.value = true
  try {
    await authApi.reviewDid({ didNo: props.didNo, reviewResult: reviewForm.reviewResult, reviewComment: reviewForm.reviewComment || undefined, requestId: generateRequestId() })
    ElMessage.success('审核已提交')
    showReview.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleSuspend() {
  const res = await ElMessageBox.prompt('请输入暂停原因', '暂停 DID', { inputType: 'textarea' })
  await authApi.suspendDid({ didNo: props.didNo, reason: res.value, requestId: generateRequestId() })
  ElMessage.success('DID 已暂停')
  loadData()
}

async function handleRevoke() {
  const res = await ElMessageBox.prompt('请输入撤销原因', '撤销 DID', { inputType: 'textarea', type: 'warning' })
  await authApi.revokeDid({ didNo: props.didNo, reason: res.value, requestId: generateRequestId() })
  ElMessage.success('DID 已撤销')
  loadData()
}

onMounted(loadData)
</script>

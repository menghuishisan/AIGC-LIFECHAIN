<template>
  <!-- 确权审核详情页（管理员） -->
  <div class="claim-review-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="`确权审核 ${detail.basicInfo.claimNo}`" :biz-no="detail.basicInfo.claimNo" :status="detail.statusInfo.status" status-type="claim">
        <template #actions>
          <el-button v-if="detail.allowedActions.includes('REVIEW')" type="success" @click="handleReview('APPROVED')">通过</el-button>
          <el-button v-if="detail.allowedActions.includes('REVIEW')" type="danger" @click="handleReview('REJECTED')">驳回</el-button>
          <el-button v-if="detail.allowedActions.includes('GENERATE_CERT')" type="primary" @click="handleGenerateCert">生成证书</el-button>
        </template>
      </DetailHeader>

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">基础信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="确权编号"><span class="font-mono">{{ detail.basicInfo.claimNo }}</span></el-descriptions-item>
              <el-descriptions-item label="摘要哈希"><span class="font-mono">{{ detail.basicInfo.summaryHash || '-' }}</span></el-descriptions-item>
              <el-descriptions-item label="关联作品">
                <el-button link type="primary" @click="router.push(`/creator/works/${detail.relationInfo.workNo}`)">{{ detail.relationInfo.workNo }}</el-button>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="当前状态"><StatusTag :status="detail.statusInfo.status" type="claim" /></el-descriptions-item>
              <el-descriptions-item label="审核意见">{{ detail.statusInfo.reviewComment || '-' }}</el-descriptions-item>
              <el-descriptions-item label="驳回原因">{{ detail.statusInfo.rejectReason || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <div style="margin-top: 20px;"><ChainInfoCard :chain-info="detail.chainInfo" /></div>
    </template>

    <!-- 审核对话框 -->
    <el-dialog v-model="showDialog" :title="dialogAction === 'APPROVED' ? '通过确权' : '驳回确权'" width="480px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
        <el-form-item label="原因代码" v-if="dialogAction === 'REJECTED'">
          <el-input v-model="reviewForm.reasonCode" placeholder="驳回原因代码（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard } from '@/shared/components'
import { generateRequestId } from '@/shared/utils'
import type { ClaimDetailVO } from '@/shared/types'

const props = defineProps<{ claimNo: string }>()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const detail = ref<ClaimDetailVO | null>(null)
const showDialog = ref(false)
const dialogAction = ref<'APPROVED' | 'REJECTED'>('APPROVED')
const reviewForm = reactive({ reviewComment: '', reasonCode: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await workApi.getClaimDetail(props.claimNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function handleReview(action: 'APPROVED' | 'REJECTED') {
  dialogAction.value = action
  reviewForm.reviewComment = ''
  reviewForm.reasonCode = ''
  showDialog.value = true
}

async function submitReview() {
  submitting.value = true
  try {
    await workApi.reviewClaim({
      claimNo: props.claimNo,
      reviewResult: dialogAction.value,
      reviewComment: reviewForm.reviewComment || undefined,
      reasonCode: reviewForm.reasonCode || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('审核已提交')
    showDialog.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleGenerateCert() {
  await workApi.generateCertificate({ claimNo: props.claimNo, requestId: generateRequestId() })
  ElMessage.success('证书生成请求已提交')
  loadData()
}

onMounted(loadData)
</script>

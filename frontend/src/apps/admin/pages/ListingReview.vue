<template>
  <!-- 上架审核详情页（管理员） -->
  <div class="listing-review-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="detail.workTitle" :biz-no="detail.listingNo" :status="detail.status" status-type="listing">
        <template #actions>
          <el-button v-if="detail.status === 'PENDING_REVIEW'" type="success" @click="handleReview('APPROVED')">通过</el-button>
          <el-button v-if="detail.status === 'PENDING_REVIEW'" type="danger" @click="handleReview('REJECTED')">驳回</el-button>
          <el-button v-if="detail.status === 'LISTED'" type="warning" @click="handleRemove">强制下架</el-button>
        </template>
      </DetailHeader>

      <div class="lc-card">
        <div class="lc-card__title">上架信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="上架编号"><span class="font-mono">{{ detail.listingNo }}</span></el-descriptions-item>
          <el-descriptions-item label="作品编号"><span class="font-mono">{{ detail.workNo }}</span></el-descriptions-item>
          <el-descriptions-item label="作品名称">{{ detail.workTitle }}</el-descriptions-item>
          <el-descriptions-item label="授权类型">{{ LicenseTypeMap[detail.licenseType] || detail.licenseType }}</el-descriptions-item>
          <el-descriptions-item label="价格">{{ formatCurrency(detail.priceAmount) }}</el-descriptions-item>
          <el-descriptions-item label="授权范围">{{ detail.scopeDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="授权天数">{{ detail.durationDays ? `${detail.durationDays}天` : '永久' }}</el-descriptions-item>
          <el-descriptions-item label="创作者"><span class="font-mono">{{ detail.creatorAccountNo }}</span></el-descriptions-item>
          <el-descriptions-item label="上架时间">{{ formatTime(detail.listTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </template>

    <!-- 审核对话框 -->
    <el-dialog v-model="showDialog" :title="dialogAction === 'APPROVED' ? '通过上架' : '驳回上架'" width="480px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { DetailHeader } from '@/shared/components'
import { LicenseTypeMap } from '@/shared/constants'
import { formatCurrency, formatTime, generateRequestId } from '@/shared/utils'
import type { ListingDetailVO } from '@/shared/types'

const props = defineProps<{ listingNo: string }>()
const loading = ref(false)
const submitting = ref(false)
const detail = ref<ListingDetailVO | null>(null)
const showDialog = ref(false)
const dialogAction = ref<'APPROVED' | 'REJECTED'>('APPROVED')
const reviewForm = reactive({ reviewComment: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getListingDetail(props.listingNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function handleReview(action: 'APPROVED' | 'REJECTED') {
  dialogAction.value = action
  reviewForm.reviewComment = ''
  showDialog.value = true
}

async function submitReview() {
  submitting.value = true
  try {
    await tradeApi.reviewListing({
      listingNo: props.listingNo,
      reviewResult: dialogAction.value,
      reviewComment: reviewForm.reviewComment || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('审核已提交')
    showDialog.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleRemove() {
  await ElMessageBox.confirm('确定要强制下架该作品吗？', '强制下架', { type: 'warning' })
  await tradeApi.removeListing(props.listingNo, generateRequestId())
  ElMessage.success('已下架')
  loadData()
}

onMounted(loadData)
</script>

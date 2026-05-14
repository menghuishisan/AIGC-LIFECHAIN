<template>
  <!-- 冻结记录详情页 -->
  <div class="freeze-detail-page" v-loading="loading">
    <DetailHeader title="冻结记录详情" :bizNo="detail?.freezeNo" :allowBack="true">
      <template #status>
        <StatusTag v-if="detail" :status="detail.freezeStatus" type="freeze" />
      </template>
      <template #actions>
        <!-- 待复核 → 复核 -->
        <el-button
          v-if="detail?.freezeStatus === 'FREEZE_APPLIED'"
          type="warning"
          @click="showReviewDialog = true"
        >复核</el-button>
        <!-- 已生效 → 解冻 -->
        <el-button
          v-if="detail?.freezeStatus === 'FREEZE_APPROVED'"
          type="success"
          @click="showUnfreezeDialog = true"
        >申请解冻</el-button>
      </template>
    </DetailHeader>

    <template v-if="detail">
      <!-- 基本信息 -->
      <div class="lc-card">
        <div class="lc-card__title">基本信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="冻结编号"><span class="font-mono">{{ detail.freezeNo }}</span></el-descriptions-item>
          <el-descriptions-item label="目标类型">{{ TargetTypeMap[detail.targetType] || detail.targetType }}</el-descriptions-item>
          <el-descriptions-item label="目标编号"><span class="font-mono">{{ detail.targetNo }}</span></el-descriptions-item>
          <el-descriptions-item label="冻结模式">{{ FreezeModeMap[detail.freezeMode] || detail.freezeMode }}</el-descriptions-item>
          <el-descriptions-item label="冻结状态"><StatusTag :status="detail.freezeStatus" type="freeze" /></el-descriptions-item>
          <el-descriptions-item label="复核状态">{{ detail.reviewStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请角色">{{ detail.applyRole }}</el-descriptions-item>
          <el-descriptions-item label="原因编码">{{ detail.reasonCode || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 冻结原因 -->
      <div class="lc-card">
        <div class="lc-card__title">冻结原因</div>
        <p style="white-space: pre-wrap; line-height: 1.6;">{{ detail.freezeReason }}</p>
      </div>

      <!-- 时间轴 -->
      <div class="lc-card">
        <div class="lc-card__title">时间线</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请时间">{{ detail.applyTime ? formatTime(detail.applyTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="批准时间">{{ detail.approveTime ? formatTime(detail.approveTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ detail.effectiveTime ? formatTime(detail.effectiveTime) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="解冻时间">{{ detail.unfreezeTime ? formatTime(detail.unfreezeTime) : '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 解冻原因 -->
      <div class="lc-card" v-if="detail.unfreezeReason">
        <div class="lc-card__title">解冻原因</div>
        <p style="white-space: pre-wrap; line-height: 1.6;">{{ detail.unfreezeReason }}</p>
      </div>

      <!-- 链上信息 -->
      <div class="lc-card" v-if="detail.txHash">
        <div class="lc-card__title">链上信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="链上状态">{{ ChainStatusMap[detail.chainStatus!] || detail.chainStatus }}</el-descriptions-item>
          <el-descriptions-item label="交易哈希"><span class="font-mono">{{ detail.txHash }}</span></el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ detail.blockHeight || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </template>

    <!-- 复核对话框 -->
    <el-dialog v-model="showReviewDialog" title="冻结复核" width="480px">
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="复核结果" required>
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">批准</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input v-model="reviewForm.reviewNote" type="textarea" :rows="3" placeholder="请输入复核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">提交</el-button>
      </template>
    </el-dialog>

    <!-- 解冻对话框 -->
    <el-dialog v-model="showUnfreezeDialog" title="申请解冻" width="480px">
      <el-form label-width="90px">
        <el-form-item label="解冻原因" required>
          <el-input v-model="unfreezeReason" type="textarea" :rows="3" placeholder="请说明解冻原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUnfreezeDialog = false">取消</el-button>
        <el-button type="success" :loading="unfreezeLoading" @click="submitUnfreeze">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { regulatorApi } from '@/shared/api'
import { TargetTypeMap, FreezeModeMap, ChainStatusMap } from '@/shared/constants'
import { formatTime, generateRequestId } from '@/shared/utils'
import StatusTag from '@/shared/components/StatusTag.vue'
import DetailHeader from '@/shared/components/DetailHeader.vue'
import type { FreezeRecordVO } from '@/shared/types'

const route = useRoute()
const freezeNo = route.params.freezeNo as string

const loading = ref(false)
const detail = ref<FreezeRecordVO | null>(null)

async function loadDetail() {
  loading.value = true
  try {
    const res = await regulatorApi.getFreezeDetail(freezeNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

/* ========== 复核表单 ========== */
const showReviewDialog = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({ approved: true as boolean, reviewNote: '' })

async function submitReview() {
  reviewLoading.value = true
  try {
    await regulatorApi.reviewFreeze({
      freezeNo,
      approved: reviewForm.approved,
      reviewNote: reviewForm.reviewNote || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('复核提交成功')
    showReviewDialog.value = false
    loadDetail()
  } finally {
    reviewLoading.value = false
  }
}

/* ========== 解冻表单 ========== */
const showUnfreezeDialog = ref(false)
const unfreezeLoading = ref(false)
const unfreezeReason = ref('')

async function submitUnfreeze() {
  if (!unfreezeReason.value) return
  unfreezeLoading.value = true
  try {
    await regulatorApi.applyUnfreeze({
      freezeNo,
      unfreezeReason: unfreezeReason.value,
      requestId: generateRequestId()
    })
    ElMessage.success('解冻申请提交成功')
    showUnfreezeDialog.value = false
    loadDetail()
  } finally {
    unfreezeLoading.value = false
  }
}

onMounted(loadDetail)
</script>

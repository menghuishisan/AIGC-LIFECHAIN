<template>
  <!-- 退款处理页（管理员） -->
  <div class="refund-process-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="`退款 ${detail.refundNo}`" :biz-no="detail.refundNo" :status="detail.refundStatus" status-type="refund" />

      <div class="lc-card">
        <div class="lc-card__title">退款信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="退款编号"><span class="font-mono">{{ detail.refundNo }}</span></el-descriptions-item>
          <el-descriptions-item label="关联订单"><span class="font-mono">{{ detail.orderNo }}</span></el-descriptions-item>
          <el-descriptions-item label="支付渠道">{{ PayChannelMap[detail.payChannel] || detail.payChannel }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">{{ formatCurrency(detail.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="退款状态"><el-tag size="small">{{ detail.refundStatus }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ detail.refundReason }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(detail.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(detail.completeTime) }}</el-descriptions-item>
          <el-descriptions-item label="失败原因" v-if="detail.failReason">{{ detail.failReason }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 处理操作 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="detail.refundStatus === 'REFUND_PENDING'">
        <div class="lc-card__title">退款处理</div>
        <el-form :model="processForm" label-width="80px" style="max-width: 480px;">
          <el-form-item label="操作">
            <el-radio-group v-model="processForm.action">
              <el-radio value="APPROVE">同意退款</el-radio>
              <el-radio value="REJECT">拒绝退款</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核意见">
            <el-input v-model="processForm.reviewComment" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="processing" @click="handleProcess">提交</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { tradeApi } from '@/shared/api'
import { DetailHeader } from '@/shared/components'
import { PayChannelMap } from '@/shared/constants'
import { formatCurrency, formatTime, generateRequestId } from '@/shared/utils'
import type { RefundDetailVO } from '@/shared/types'

const props = defineProps<{ refundNo: string }>()
const loading = ref(false)
const processing = ref(false)
const detail = ref<RefundDetailVO | null>(null)
const processForm = reactive({ action: 'APPROVE', reviewComment: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await tradeApi.getRefundDetail(props.refundNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleProcess() {
  processing.value = true
  try {
    await tradeApi.processRefund({
      refundNo: props.refundNo,
      action: processForm.action,
      reviewComment: processForm.reviewComment || undefined,
      requestId: generateRequestId()
    })
    ElMessage.success('处理完成')
    loadData()
  } finally {
    processing.value = false
  }
}

onMounted(loadData)
</script>

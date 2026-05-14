<template>
  <!-- 结算详情页（管理员） -->
  <div class="settlement-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="`结算 ${detail.settleNo}`" :biz-no="detail.settleNo" :status="detail.settleStatus" status-type="settlement">
        <template #actions>
          <el-button v-if="['SETTLE_FAILED'].includes(detail.settleStatus)" type="primary" @click="handleRetry">重试结算</el-button>
          <el-button v-if="['SETTLE_SUCCESS'].includes(detail.settleStatus)" type="warning" @click="handleReverse">逆分账</el-button>
        </template>
      </DetailHeader>

      <div class="lc-card">
        <div class="lc-card__title">结算信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="结算编号"><span class="font-mono">{{ detail.settleNo }}</span></el-descriptions-item>
          <el-descriptions-item label="关联订单"><span class="font-mono">{{ detail.orderNo }}</span></el-descriptions-item>
          <el-descriptions-item label="作品编号"><span class="font-mono">{{ detail.workNo || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="总金额">{{ formatCurrency(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="结算状态"><StatusTag :status="detail.settleStatus" type="settlement" /></el-descriptions-item>
          <el-descriptions-item label="链上状态">{{ detail.chainStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易哈希"><span class="font-mono">{{ detail.txHash || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="结算时间">{{ formatTime(detail.settleTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(detail.completeTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 分账项 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="detail.items?.length">
        <div class="lc-card__title">分账明细</div>
        <el-table :data="detail.items" stripe size="small">
          <el-table-column prop="roleType" label="角色" width="120" />
          <el-table-column prop="accountNo" label="账户" min-width="180">
            <template #default="{ row }"><span class="font-mono">{{ row.accountNo }}</span></template>
          </el-table-column>
          <el-table-column prop="ratio" label="分成比例" width="100">
            <template #default="{ row }">{{ (row.ratio * 100).toFixed(1) }}%</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">{{ formatCurrency(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="itemStatus" label="状态" width="120" />
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { appApi } from '@/shared/api'
import { DetailHeader, StatusTag } from '@/shared/components'
import { formatCurrency, formatTime, generateRequestId } from '@/shared/utils'
import type { SettlementDetailVO } from '@/shared/types'

const props = defineProps<{ settleNo: string }>()
const loading = ref(false)
const detail = ref<SettlementDetailVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await appApi.getSettlementDetail(props.settleNo)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleRetry() {
  await ElMessageBox.confirm('确定要重试结算吗？', '重试结算')
  await appApi.retrySettlement({ settleNo: props.settleNo, requestId: generateRequestId() })
  ElMessage.success('重试已发起')
  loadData()
}

async function handleReverse() {
  const { value: reason } = await ElMessageBox.prompt('请输入逆分账原因', '逆分账', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValidator: (v: string) => !!v?.trim() || '原因不能为空',
    type: 'warning'
  })
  await appApi.reverseSettlement({ settleNo: props.settleNo, reason: reason!, requestId: generateRequestId() })
  ElMessage.success('逆分账已发起')
  loadData()
}

onMounted(loadData)
</script>

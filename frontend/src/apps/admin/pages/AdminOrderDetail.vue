<template>
  <!-- 订单详情页（管理员） -->
  <div class="admin-order-detail-page" v-loading="loading">
    <template v-if="detail">
      <DetailHeader :title="detail.basicInfo.workTitle" :biz-no="detail.basicInfo.orderNo" :status="detail.statusInfo.orderStatus" status-type="order" />

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">订单信息</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="订单编号"><span class="font-mono">{{ detail.basicInfo.orderNo }}</span></el-descriptions-item>
              <el-descriptions-item label="作品名称">{{ detail.basicInfo.workTitle }}</el-descriptions-item>
              <el-descriptions-item label="授权类型">{{ LicenseTypeMap[detail.basicInfo.licenseType] || detail.basicInfo.licenseType }}</el-descriptions-item>
              <el-descriptions-item label="标价">{{ formatCurrency(detail.basicInfo.priceAmount) }}</el-descriptions-item>
              <el-descriptions-item label="实付">{{ formatCurrency(detail.basicInfo.payAmount) }}</el-descriptions-item>
              <el-descriptions-item label="渠道">{{ PayChannelMap[detail.basicInfo.payChannel] || detail.basicInfo.payChannel }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="lc-card">
            <div class="lc-card__title">状态与时间</div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="订单状态"><StatusTag :status="detail.statusInfo.orderStatus" type="order" /></el-descriptions-item>
              <el-descriptions-item label="支付状态"><StatusTag :status="detail.statusInfo.payStatus" type="pay" /></el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(detail.timeInfo.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ formatTime(detail.timeInfo.payTime) }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ formatTime(detail.timeInfo.completeTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

      <div class="lc-card" style="margin-top: 20px;">
        <div class="lc-card__title">关联信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="作品编号"><span class="font-mono">{{ detail.relationInfo.workNo }}</span></el-descriptions-item>
          <el-descriptions-item label="上架编号"><span class="font-mono">{{ detail.relationInfo.listingNo }}</span></el-descriptions-item>
          <el-descriptions-item label="买家"><span class="font-mono">{{ detail.relationInfo.buyerAccountNo }}</span></el-descriptions-item>
          <el-descriptions-item label="创作者"><span class="font-mono">{{ detail.relationInfo.creatorAccountNo }}</span></el-descriptions-item>
          <el-descriptions-item label="授权编号"><span class="font-mono">{{ detail.relationInfo.licenseNo || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="结算编号"><span class="font-mono">{{ detail.relationInfo.settleNo || '-' }}</span></el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 结算信息 -->
      <div class="lc-card" style="margin-top: 20px;" v-if="settlement">
        <div class="lc-card__title">结算信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="结算编号"><span class="font-mono">{{ settlement.settleNo }}</span></el-descriptions-item>
          <el-descriptions-item label="总金额">{{ formatCurrency(settlement.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="结算状态"><StatusTag :status="settlement.settleStatus" type="settlement" /></el-descriptions-item>
          <el-descriptions-item label="链上状态">{{ settlement.chainStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易哈希"><span class="font-mono">{{ settlement.txHash || '-' }}</span></el-descriptions-item>
          <el-descriptions-item label="结算时间">{{ formatTime(settlement.settleTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-table v-if="settlement.items?.length" :data="settlement.items" stripe size="small" style="margin-top: 12px;">
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

      <div style="margin-top: 20px;"><ChainInfoCard :chain-info="detail.chainInfo" /></div>

      <div class="lc-card" style="margin-top: 20px;">
        <TraceTimeline title="订单轨迹" :events="traces" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { tradeApi, appApi } from '@/shared/api'
import { DetailHeader, StatusTag, ChainInfoCard, TraceTimeline } from '@/shared/components'
import { LicenseTypeMap, PayChannelMap } from '@/shared/constants'
import { formatCurrency, formatTime } from '@/shared/utils'
import type { OrderDetailVO, TraceEventVO, SettlementDetailVO } from '@/shared/types'

const props = defineProps<{ orderNo: string }>()
const loading = ref(false)
const detail = ref<OrderDetailVO | null>(null)
const traces = ref<TraceEventVO[]>([])
const settlement = ref<SettlementDetailVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const [detailRes, traceRes] = await Promise.all([
      tradeApi.getOrderDetail(props.orderNo),
      appApi.getOrderTraces(props.orderNo)
    ])
    detail.value = detailRes.data
    traces.value = traceRes.data || []

    /* 加载结算信息 */
    try {
      const settleRes = await appApi.getOrderSettlement(props.orderNo)
      settlement.value = settleRes.data
    } catch { /* 未结算或无数据 */ }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
